/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at https://www.live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d.wp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.util.Log;

import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Live2DGLRenderer implements GLSurfaceView.Renderer, SensorEventListener {
    Context con;

    private SensorManager sensorManager;
    private Sensor sensor;
    private boolean useSensor = false;


    public Live2DGLRenderer(Context context)
    {
        con = context;
        initUseSensor();
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        JniBridgeJava.nativeOnSurfaceCreated();
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        JniBridgeJava.nativeOnSurfaceChanged(width, height);
    }

    public void onDrawFrame(GL10 gl) {
        JniBridgeJava.nativeOnDrawFrame();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        JniBridgeJava.SetGravitationalAccelerationX(event.values[0]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    /**
     * Called when the engine is destroyed. Do any necessary clean up because
     * at this point your renderer instance is now done for.
     */
    public void release() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this, sensor);
        }
    }

    public void initUseSensor() {
        if (!useSensor) {
            useSensor = con.getSharedPreferences("com.live2d.wp_preferences", Context.MODE_PRIVATE).getBoolean("sensor", false);
        }
        // TODO: tmp
//        return;

        if (useSensor && (sensorManager == null || sensor == null)) {
            sensorManager = (SensorManager)con.getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null){
                sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
            }
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
        Log.d("030-wp", "sensor: " + useSensor);
    }

    public void setUseSensor(boolean useSensor) {
        this.useSensor = useSensor;
        try {
            if (!useSensor) {
                release();
            } else {
                initUseSensor();
            }
        } catch (Exception ex) {
            Log.e("030-sensor", String.format("%s: %s", ex.getMessage(), Arrays.toString(ex.getStackTrace())));
        }
    }
}
