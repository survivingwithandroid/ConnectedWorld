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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

import orbotix.robot.base.Robot;
import orbotix.robot.base.RobotProvider;
import orbotix.sphero.ConnectionListener;
import orbotix.sphero.DiscoveryListener;
import orbotix.sphero.Sphero;


public class BallConnectionService extends Service {

    private Sphero mySphero;
    private static int RANGE = 40;

    public static int CONNECTING = 0;
    public static int CONNECTED = 100;
    public static int FAILED = -100;
    int NOTIFICATION_ID = 1107;

    public static String BALL_STATUS_FILTER = "com.survivingwithandroid.ball_filter";

    int tempValue = -10;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mySphero == null)
            doConnection();

        IntentFilter rec = new IntentFilter();
        rec.addAction(SensorService.TEMP_BALL_SENSOR);
        registerReceiver(receiver, rec);

        return Service.START_STICKY;
    }


    private void doConnection() {

        sendStatus(CONNECTING);
        createNotification("Connecting...");

        RobotProvider.getDefaultProvider().addConnectionListener(new ConnectionListener() {
            @Override
            public void onConnected(Robot robot) {
                Log.d("Temp", "Connected");
                mySphero = (Sphero) robot;
                sendStatus(CONNECTED);
                createNotification("Connected");
            }

            @Override
            public void onConnectionFailed(Robot robot) {
                Log.d("Temp", "Conection failed");
                sendStatus(FAILED);
            }

            @Override
            public void onDisconnected(Robot robot) {
                Log.d("Temp", "Disconnected");
                mySphero = null;
                createNotification("Disconnected!");
            }
        });

        RobotProvider.getDefaultProvider().addDiscoveryListener(new DiscoveryListener() {
            @Override
            public void onBluetoothDisabled() {
                Log.d("Temp", "BT Disabled");
            }

            @Override
            public void discoveryComplete(List<Sphero> spheros) {
                Log.d("Temp", "Found ["+spheros.size()+"]");
            }

            @Override
            public void onFound(List<Sphero> spheros) {
                // Do connection
                Log.d("Temp", "Found ball");
                RobotProvider.getDefaultProvider().connect(spheros.get(0));
            }
        });

        boolean success = RobotProvider.getDefaultProvider().startDiscovery(this);
    }

    @Override
    public void onDestroy() {
        Log.d("Temp", "Destroy BallConnection");
        unregisterReceiver(receiver);
        if (mySphero != null) {
            mySphero.disconnect();
            //RobotProvider.getDefaultProvider().shutdown();
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
        super.onDestroy();

    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            float val = intent.getFloatExtra("value", 0);
            Log.d("Temp", "Received value ["+val+"]");

           if (mySphero != null) {
                // send color to sphero
                int red = (int) (255 * val / RANGE) * (val > 10 ? 1 : 0);
                int green = (int) ( (255 * (RANGE - Math.abs(val)) / RANGE) * (val < 10 ? 0.2 : 1) );
                int blue = (int) (255 * (10 - val) / 10) * (val < 10  ? 1 : 0);
                mySphero.setColor(red, green, blue);
            }
        }
    };


    private void sendStatus(int status) {
        Intent i = new Intent();
        i.setAction(BALL_STATUS_FILTER);
        i.putExtra("status", status);
        sendBroadcast(i);
    }

    private void createNotification(String text) {
        Intent intent = new Intent(this, MyActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification notification =
                new Notification.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_icon)
                        .setContentTitle(getResources().getString(R.string.not_text))
                        .setContentText(text)
                        .setContentIntent(pIntent)
                .build();


        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(NOTIFICATION_ID, notification);
    }
}
