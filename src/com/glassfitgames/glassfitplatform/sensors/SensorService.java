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
import android.opengl.Matrix;
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
    private float[] worldToDeviceRotationVector = new float[3]; // quaternion to rotate from world to device
    private float[] deviceToWorldRotationVector = new float[3]; // quaternion to rotate from device to world
    private float[] deviceToWorldTransform = new float[16]; // rotation matrix to get from device co-ords to world co-ords
    private float[] worldToDeviceTransform = new float[16]; // rotation matrix to get from world co-ords to device co-ords
    private float[] ypr = new float[3]; // yaw, pitch, roll
    private float[] linAcc = new float[3];
    
    
    /* The next three definitions set up this class as a service */

    public class SensorServiceBinder extends Binder {
        public SensorService getService() {
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
            // compute the rotation of device in real-world co-ords, and vice-versa
            worldToDeviceRotationVector = event.values;
            deviceToWorldRotationVector[0] = -event.values[0];
            deviceToWorldRotationVector[1] = -event.values[1];
            deviceToWorldRotationVector[2] = -event.values[2];
            
            // compute rotation matrices to convert between device and real-world coordinate sytems
            SensorManager.getRotationMatrixFromVector(worldToDeviceTransform, worldToDeviceRotationVector);
            SensorManager.getRotationMatrixFromVector(deviceToWorldTransform, deviceToWorldRotationVector);
            
            // calculate device's roll, pitch and yaw in real-world co-ords (for display)
            SensorManager.getOrientation(worldToDeviceTransform, ypr);
            
        } else if (event.sensor == linearAcceleration) {
            linAcc = event.values;
//            linAcc[0] = (float)(0.95*linAcc[0] + 0.05*event.values[0]);
//            linAcc[1] = (float)(0.95*linAcc[1] + 0.05*event.values[1]);
//            linAcc[2] = (float)(0.95*linAcc[2] + 0.05*event.values[2]);
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
        return worldToDeviceRotationVector;
    }
    
    public float[] getYprValues() {
        return ypr;
    }
    
    public float[] getLinAccValues() {
        return linAcc;
    }
    
    public float[] rotateToRealWorld(float[] inVec) {
        float[] resultVec = new float[4];
        Matrix.multiplyMV(resultVec, 0, deviceToWorldTransform, 0, inVec, 0);
        return resultVec;
    }

  }
