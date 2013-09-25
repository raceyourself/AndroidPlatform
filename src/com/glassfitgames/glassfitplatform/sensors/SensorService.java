package com.glassfitgames.glassfitplatform.sensors;

import java.util.List;

import com.roscopeco.ormdroid.ORMDroidApplication;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class SensorService extends Service implements SensorEventListener {
    
    private final IBinder sensorServiceBinder = new SensorServiceBinder();
    
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor magnetometer;
    private Sensor rotationVector;
    private Sensor linearAcceleration;
    
    private float[] acc = new float[3];
    private float[] gyro = {0.0f, 0.0f, 0.0f};
    private float[] mag = new float[3];
    private float[] quat = new float[3]; // current orientation as a quaternion
    private float[] rotMat = new float[16]; // rotation matrix to get from flat/north to current rotation
    private float[] ypr = new float[3]; // yaw, pitch, roll
    private float[] linAcc = new float[3];
    
    
    /* The next three definitions set up this class as a service */

    public class SensorServiceBinder extends Binder {
        SensorService getService() {
            return SensorService.this;
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        
        ORMDroidApplication.initialize(getBaseContext());
        
        mSensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> allSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        
        for (Sensor s : allSensors) {
            Log.i("GlassFitPlatform","Found sensor " + s.getName());
        }
        
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        rotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        linearAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, linearAcceleration, SensorManager.SENSOR_DELAY_GAME);
        
        return sensorServiceBinder;
    }
    
    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this);
    }    
       


    /* From here down we're working with the sensors */
    
    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
      // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        
        if (event.sensor == accelerometer) {
            acc = event.values;
        } else if (event.sensor == gyroscope) {
            gyro[0] += event.values[0];
            gyro[1] += event.values[1];
            gyro[2] += event.values[2];
        } else if (event.sensor == magnetometer) {
            mag = event.values;
        } else if (event.sensor == rotationVector) {
            // convert quaternion to roll, pitch and yaw for display
            quat = event.values;
            SensorManager.getRotationMatrix(rotMat, event.values, acc, mag);
            SensorManager.getOrientation(rotMat, ypr);
        } else if (event.sensor == linearAcceleration) {
            linAcc = event.values;
        }

    }
    
    public float[] getAccValues() {
        return acc;
    }

    public float[] getGyroValues() {
        return gyro;
    }
    
    public float[] getMagValues() {
        return mag;
    }
    
    public float[] getQuatValues() {
        return quat;
    }
    
    public float[] getYprValues() {
        return ypr;
    }
    
    public float[] getLinAccValues() {
        return linAcc;
    }    

  }
