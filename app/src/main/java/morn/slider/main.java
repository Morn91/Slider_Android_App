package morn.slider;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Scanner;

public class main extends Activity {
    public Typeface timerFont;
    public TextView timer;
    public TextView timerBg;
    public TextView battery;
    public ProgressBar progressBar;
    public ImageView imageView;
    public ImageView imageView1;
    public ImageView batteryImage;
    public Button startButton;
    public Button stopButton;
    public ImageButton dirButton;
    public Spinner spinner;

    public AlertDialog.Builder alertDialog;
    public Notification.Builder notificationBuilder;
    public NotificationManager notificationManager;
    public BluetoothSocket bluetoothSocket = null;
    public BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public InputStream input;
    public OutputStream output;

    public int h = 1, m = 0, s = 0, p = 0, dir = 0, batMin = 660, batMax = 840;
    public String address = "30:14:10:17:01:77";
    public boolean go = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        timerFont = Typeface.createFromAsset(getAssets(), "fonts/digital_clock.ttf");

        timer = (TextView)findViewById(R.id.timer);
        timer.setTypeface(timerFont);
        timer.setText(String.format("%02d:%02d:%02d", h, m, s));

        timerBg = (TextView)findViewById(R.id.timerBg);
        timerBg.setTypeface(timerFont);

        battery = (TextView)findViewById(R.id.battery);

        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        progressBar.setProgress(p);

        imageView = (ImageView)findViewById(R.id.imageView);

        imageView1 = (ImageView)findViewById(R.id.imageView1);
        imageView1.setAlpha((float)0.05);
        imageView1.setImageResource(R.drawable.a01234);

        batteryImage = (ImageView)findViewById(R.id.batteryImage);
        batteryImage.setImageResource(R.drawable.battery);

        startButton = (Button)findViewById(R.id.start);
        stopButton = (Button)findViewById(R.id.stop);

        dirButton = (ImageButton)findViewById(R.id.dirButton);
        dirButton.setImageResource(R.drawable.dir0);
        dirButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(dir == 0) {
                    dir = 1;
                    dirButton.setImageResource(R.drawable.dir1);
                } else {
                    dir = 0;
                    dirButton.setImageResource(R.drawable.dir0);
                }
            }
        });

        spinner = (Spinner)findViewById(R.id.spinner);
        spinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, new String[] {
                getString(R.string.a0), getString(R.string.a1), getString(R.string.a2), getString(R.string.a3), getString(R.string.a4)
        }));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        imageView.setImageResource(R.drawable.a0);
                        break;
                    case 1:
                        imageView.setImageResource(R.drawable.a1);
                        break;
                    case 2:
                        imageView.setImageResource(R.drawable.a2);
                        break;
                    case 3:
                        imageView.setImageResource(R.drawable.a3);
                        break;
                    case 4:
                        imageView.setImageResource(R.drawable.a4);
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        alertDialog = new AlertDialog.Builder(this);

        notificationBuilder = new Notification.Builder(getApplicationContext());
        notificationManager = (NotificationManager)getApplicationContext().getSystemService(NOTIFICATION_SERVICE);

        new connect().execute();
    }

    public void start(View view) {
        try {
            while(input.available() > 0)
                input.read();
            output.write(String.format("+ %d %d %d", dir, spinner.getSelectedItemId(), (h * 3600 + m * 60 + s)).getBytes());
            SystemClock.sleep(500);
            request(null);
        } catch(IOException e) {
            Log.d("BLUETOOTH", e.getMessage());
        }
    }

    public void stop(View view) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.stop))
                .setMessage(getString(R.string.stopDialog))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            output.write('-');
                            request(null);
                        } catch (IOException e) {
                            Log.d("BLUETOOTH", e.getMessage());
                        }
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                })
                .show();
    }

    public void request(View view) {
        if(bluetoothSocket != null && bluetoothSocket.isConnected()) {
            String buff = "";
            try {
                output.write('*');
                int i = 0;
                char a;
                while(input.available() == 0 && i < 100) {
                    i++;
                    SystemClock.sleep(10);
                }
                SystemClock.sleep(100);
                if(input.available() > 0)
                    while(true) {
                        if(input.available() > 0) {
                            a = (char) input.read();
                            if(a == '\n') {
                                break;
                            } else
                                buff += a;
                        }
                    }
                else
                    new connect().execute();
            } catch(IOException e) {
                Log.d("BLUETOOTH", e.getMessage());
            }
            Scanner scanner = new Scanner(buff);
            int dir1 = scanner.nextInt();
            int accelType = scanner.nextInt();
            int time = scanner.nextInt();
            int left = scanner.nextInt();
            int voltage = scanner.nextInt();
            if(left > 0) {
                lock();
                timer.setText(String.format("%02d:%02d:%02d", (int)Math.floor(left / 3600.0), (int)Math.floor(left % 3600 / 60.0), left % 3600 % 60));
                h = (int)Math.floor(time / 3600.0);
                m = (int)Math.floor(time % 3600 / 60.0);
                s = time % 3600 % 60;
                dir = dir1;
                if(dir == 1)
                    dirButton.setImageResource(R.drawable.dir1);
                else
                    dirButton.setImageResource(R.drawable.dir0);
                go = true;
                spinner.setSelection(accelType);
                float p0 = 1 - (float)left / time;
                switch(accelType) {
                    case 0:
                        p = Math.round(1000 * p0);
                        break;
                    case 1:
                        p = Math.round(1000 * (float)Math.pow(p0, 2));
                        break;
                    case 2:
                        p = Math.round(1000 * (1 - (float)Math.pow(p0 - 1, 2)));
                        break;
                    case 3:
                        if(p0 < 0.5)
                            p = Math.round(2000 * (float)Math.pow(p0, 2));
                        else
                            p = Math.round(1000 * (1 - 2 * (float)Math.pow(p0 - 1, 2)));
                        break;
                    case 4:
                        if(p0 < 0.5)
                            p = Math.round(500 * (1 - (float)Math.pow(p0 * 2 - 1, 2)));
                        else
                            p = Math.round(500 * (1 + (float)Math.pow(p0 * 2 - 1, 2)));
                        break;
                }
                progressBar.setProgress(p);
            } else {
                go = false;
                unlock();
                timer.setText(String.format("%02d:%02d:%02d", h, m, s));
                p = 0;
                progressBar.setProgress(p);
            }
            battery.setText(Math.round(100.0 * (float)(voltage - batMin) / (batMax - batMin)) + "%");
        } else
            new connect().execute();
    }

    class connect extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setIndeterminate(true);
            lock();
            battery.setText("");
            stopButton.setEnabled(false);
            startButton.setVisibility(View.VISIBLE);
            stopButton.setVisibility(View.INVISIBLE);
        }
        @Override
        protected Void doInBackground(Void... params) {
            if(bluetoothAdapter.isEnabled()) {
                if(!(bluetoothSocket != null && bluetoothSocket.isConnected()))
                    try {
                        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
                        Method m = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                        bluetoothSocket = (BluetoothSocket)m.invoke(device, 1);
                        bluetoothSocket.connect();
                    } catch(IOException e) {
                        Log.d("BLUETOOTH", e.getMessage());
                    } catch(SecurityException e) {
                        Log.d("BLUETOOTH", e.getMessage());
                    } catch(NoSuchMethodException e) {
                        Log.d("BLUETOOTH", e.getMessage());
                    } catch(IllegalArgumentException e) {
                        Log.d("BLUETOOTH", e.getMessage());
                    } catch(IllegalAccessException e) {
                        Log.d("BLUETOOTH", e.getMessage());
                    } catch(InvocationTargetException e) {
                        Log.d("BLUETOOTH", e.getMessage());
                    }
                try {
                    input = bluetoothSocket.getInputStream();
                    output = bluetoothSocket.getOutputStream();
                } catch(IOException e) {
                    Log.d("BLUETOOTH", e.getMessage());
                }
            } else
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 0);
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(bluetoothSocket != null && bluetoothSocket.isConnected()) {
                progressBar.setIndeterminate(false);
                if(go)
                    stopButton.setEnabled(true);
                else
                    unlock();
                request(null);
            } else
            if(bluetoothAdapter.isEnabled())
                alertDialog.setTitle(getString(R.string.error))
                        .setMessage(getString(R.string.noConnectDialog))
                        .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new connect().execute();
                            }
                        })
                        .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if(!go)
                                    finish();
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                finish();
                            }
                        })
                        .show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        notificationManager.cancel(1);
    }

    @Override
    public void onBackPressed() {
        if(go)
            moveTaskToBack(true);
        else
            super.onBackPressed();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == -1)
            new connect().execute();
        else
            finish();
    }

    public void setTime(View view) {
        new TimePickerDialog(this, myCallBack, h, m, true).show();
    }
    TimePickerDialog.OnTimeSetListener myCallBack = new TimePickerDialog.OnTimeSetListener() {
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            h = hourOfDay;
            m = minute;
            s = 0;
            timer.setText(String.format("%02d:%02d:%02d", h, m, s));
        }
    };

    public void lock() {
        timer.setClickable(false);
        startButton.setEnabled(false);
        startButton.setVisibility(View.INVISIBLE);
        stopButton.setEnabled(true);
        stopButton.setVisibility(View.VISIBLE);
        dirButton.setEnabled(false);
        spinner.setEnabled(false);
    }

    public void unlock() {
        timer.setClickable(true);
        startButton.setEnabled(true);
        startButton.setVisibility(View.VISIBLE);
        stopButton.setEnabled(false);
        stopButton.setVisibility(View.INVISIBLE);
        dirButton.setEnabled(true);
        spinner.setEnabled(true);
    }
}