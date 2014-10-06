/*
 * Copyright (C) 2014 Francesco Azzola
 *  Surviving with Android (http://www.survivingwithandroid.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.survivingwithandroid.tempball;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


public class MyActivity extends Activity {

    private TextView tempView;
    private Animation pulseAnim;
    private ImageButton btnControl;
    private boolean isPlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        if (savedInstanceState != null)
          isPlaying = savedInstanceState.getBoolean("isPlaying");

        tempView = (TextView) findViewById(R.id.temp);
        pulseAnim = AnimationUtils.loadAnimation(this, R.anim.scale);
        btnControl = (ImageButton) findViewById(R.id.btn);
        btnControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlaying) {
                    stop();
                }
                else {
                    start();
                }
            }
        });

        if (isPlaying)
            changeButton(R.drawable.btn_bckg_stop, android.R.drawable.ic_media_pause);
        else
            changeButton(R.drawable.btn_bckg, android.R.drawable.ic_media_play);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
            int id = item.getItemId();
        if (id == R.id.action_start) {
            start();
        }
        else if (id == R.id.action_stop) {
            stop();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isPlaying", isPlaying);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        isPlaying = savedInstanceState.getBoolean("isPlaying");

    }

    private void start() {

        // Check if BT is on
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        String msg = null;
        if (mBluetoothAdapter == null) {
            msg = "Your smartphone doesn't support Bluetooth";
        }
        else if (!mBluetoothAdapter.isEnabled()) {
            msg = "Enable Bluetooth and pair your smartphone with Sphero";
        }

        if (msg != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth Error");
            builder.setMessage(msg).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    return ;
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
            return ;
        }

        register();
        PendingIntent pi = createAlarm();
        AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 5 * 1000, pi);


        Intent i1 = new Intent(this, BallConnectionService.class);
        startService(i1);

        isPlaying = true;
        changeButton(R.drawable.btn_bckg_stop, android.R.drawable.ic_media_pause);
   }


    private void changeButton(int bckId, int imgId) {
        btnControl.setBackgroundResource(bckId);
        btnControl.setImageResource(imgId);
    }


    private void stop() {
        // Intent i = new Intent(this, SensorService.class);
        // stopService(i);

        PendingIntent pi = createAlarm();
        AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        scheduler.cancel(pi);

        Intent i1 = new Intent(this, BallConnectionService.class);
        stopService(i1);
        unregister();
        tempView.clearAnimation();

        changeButton(R.drawable.btn_bckg, android.R.drawable.ic_media_play);
        isPlaying = false;
    }

    private void register() {
        IntentFilter rec = new IntentFilter();
        rec.addAction(SensorService.TEMP_BALL_SENSOR);
        registerReceiver(sensorReceiver, rec);

        // register ball connection status
        IntentFilter statRec = new IntentFilter();
        statRec.addAction(BallConnectionService.BALL_STATUS_FILTER);
        registerReceiver(ballReceiver, statRec);

    }

    private void unregister() {
        try {
            unregisterReceiver(sensorReceiver);
        } catch (Throwable ignore) {}
        try {
            unregisterReceiver(ballReceiver);
        } catch (Throwable ignore) {}
    }



    @Override
    protected void onPause() {
        super.onPause();
        unregister();
    }

    @Override
    protected void onResume() {
        super.onResume();
        register();
        Intent i = new Intent(this, SensorService.class);
        startService(i);
    }


    private PendingIntent createAlarm() {
        AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this,SensorService.class );
        PendingIntent scheduledIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return scheduledIntent;
    }

    private BroadcastReceiver sensorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            float val = intent.getFloatExtra("value", 0);
            tempView.setText(String.format("%.1f",val));
        }
    };

    private BroadcastReceiver ballReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int status = intent.getIntExtra("status", -1000);

            Log.d("Temp", "Value Status ["+status+"]");
            if  (status == BallConnectionService.CONNECTING) {
                tempView.startAnimation(pulseAnim);
                Toast.makeText(MyActivity.this, "Connecting...", Toast.LENGTH_SHORT).show();
            }
            else if (status == BallConnectionService.CONNECTED) {
                tempView.clearAnimation();
                Intent i = new Intent(MyActivity.this, SensorService.class);
                startService(i);
                Toast.makeText(MyActivity.this, "Connected", Toast.LENGTH_LONG).show();
            }
            else if (status == BallConnectionService.FAILED) {
                Toast.makeText(MyActivity.this, "Connection failed. Try again pressing start button", Toast.LENGTH_LONG).show();
            }

        }
    };
}
