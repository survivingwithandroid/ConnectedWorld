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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;


public class SensorService  extends Service implements SensorEventListener {

    private SensorManager sManager;
    private Sensor sensor;

    public static final String TEMP_BALL_SENSOR = "com.survivingwithandroid.tempball";
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Temp", "On start sensor");
        sManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        sManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        return Service.START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // We get the temperature and notify it to the Activity
        float temp = event.values[0];
        Intent i = new Intent();
        i.setAction(TEMP_BALL_SENSOR);
        i.putExtra("value", temp);
        sendBroadcast(i);

        // stop listener
        if (sManager != null)
            sManager.unregisterListener(this);

        // stop service
        stopSelf();

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sManager != null)
            sManager.unregisterListener(this);

    }
}
