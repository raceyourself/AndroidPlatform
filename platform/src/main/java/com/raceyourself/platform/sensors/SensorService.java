package com.raceyourself.platform.sensors;

import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

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
import android.view.Surface;
import android.view.WindowManager;

public class SensorService extends Service implements SensorEventListener {
    
    private final IBinder sensorServiceBinder = new SensorServiceBinder();
    
    private SensorManager mSensorManager;
    private WindowManager windowManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor magnetometer;
    private Sensor rotationVector;
    private Sensor linearAcceleration;
    private Sensor orientation;
    
    private float[] acc = new float[3];
    private float[] gyro = {0.0f, 0.0f, 0.0f};
    private float[] mag = new float[3];
    
    float azimuth;
    
    private float[] worldToDeviceRotationVector = new float[3]; // quaternion to rotate from world to device
    private float[] deviceToWorldRotationVector = new float[3]; // quaternion to rotate from device to world
    private float[] deviceToWorldTransform = new float[16]; // rotation matrix to get from device co-ords to world co-ords
    private float[] worldToDeviceTransform = new float[16]; // rotation matrix to get from world co-ords to device co-ords
    private float[] ypr = new float[3]; // yaw, pitch, roll
    private float[] linAcc = new float[3];
    
    private float[] gameYpr = new float[3];
    
    private Quaternion gyroDroidQuaternion = Quaternion.identity();;
    private Quaternion glassfitQuaternion = Quaternion.identity();
    private Quaternion deltaQuaternion;
    private Quaternion accelQuaternion = Quaternion.identity();
    
    float accRoll = 0.0f;
    float accPitch = 0.0f;
    float fusedRoll = 0.0f;
    float fusedPitch = 0.0f;
    
    Vector3D deviceAcceleration;
    Vector3D realWorldAcceleration;
    
    private long timestamp = 0;
    
    
    /* The next three definitions set up this class as a service */

    public class SensorServiceBinder extends Binder {
        public SensorService getService() {
            return SensorService.this;
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        
        ORMDroidApplication.initialize(getBaseContext());
        
        windowManager = (WindowManager)this.getSystemService("window");  // so we can check screen rotation
        
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
        orientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, linearAcceleration, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, orientation, SensorManager.SENSOR_DELAY_GAME);
        
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

            // calculate pitch and roll
            //TODO: make roll go between -180 and +180 (currently -90 to +90)
            float sampleRoll = -(float)Math.atan(acc[0]/Math.sqrt(acc[1]*acc[1]+acc[2]*acc[2]));
            float samplePitch = (float)Math.PI/2.0f - (float)Math.atan(acc[2]/Math.sqrt(acc[0]*acc[0]+acc[1]*acc[1]));
            
            // TODO: reimplement the rest of this method in quaternions to reduce number of trig functions executed
            float lowpass = 0.1f;
            accRoll = lowpass*sampleRoll + (1-lowpass)*accRoll;
            accPitch = lowpass*samplePitch + (1-lowpass)*accPitch;
            
            // Update Orientation to correct any gyro drift in roll/pitch axes
            // transform acceleration into real-world co-ordinates
            deviceAcceleration = new Vector3D(acc[0],acc[1], acc[2]).normalize();  // (x,y,z) device co-ords
            realWorldAcceleration = accelQuaternion.rotateVector(deviceAcceleration);  // (x,y,z) world co-ords
            
            // calculate a correction to apply to the gyro orientation
            Vector3D straightUp = new Vector3D(0.0f, 0.0f, 1.0f);  // (x,y,z) world co-ords
            Quaternion correction = Quaternion.quaternionBetween(realWorldAcceleration, straightUp);  // rotation accel -> straight up
            
            // apply the correction to get ideal orientation (but affected by linear acc and accel noise)
            accelQuaternion = glassfitQuaternion.multiply(correction);
            
            // update glassfit orienation with a small amount of the corrected orientation (to get rid of accel noise/linacc)
            glassfitQuaternion = glassfitQuaternion.nlerp(accelQuaternion, 0.02f);
            
        } else if (event.sensor == gyroscope) {
            
            // raw gyro values are angular velocity in rad/s
            System.arraycopy(event.values, 0, gyro, 0, 3);
            
            // initialise timestamp in 1st loop for calculating dT
            if (timestamp == 0) {
                timestamp = event.timestamp;
                return;
            }
            
            // initialise the orientation to the accelerometer roll/pitch and zero yaw
            if (glassfitQuaternion.equals(Quaternion.identity())) {
                if (accRoll != 0.0f || accPitch != 0.0f) {
                    glassfitQuaternion = new Quaternion(0.0f, accPitch, accRoll);
                    accelQuaternion = new Quaternion(0.0f, accPitch, accRoll);
                }
                return;
            }
            
            // integrate gyro velocity to get orientation
            float dT = (event.timestamp - timestamp) / 1000000000.0f;
            deltaQuaternion = new Quaternion(getRotationVectorFromGyro(gyro, dT));
            glassfitQuaternion = glassfitQuaternion.multiply(deltaQuaternion);
            accelQuaternion = accelQuaternion.multiply(deltaQuaternion);

            // measurement done, save current time for next interval
            timestamp = event.timestamp;
            

        } else if (event.sensor == magnetometer) {
            mag = event.values;

        } else if (event.sensor == rotationVector) {           
            Quaternion sensorRotation = new Quaternion(event.values);
            // take 90 degrees off the pitch, so (0,0,0) is straight in front of us
            // should ideally do this in quaternions (is it possible?) to reduce CPU
            //float[] YPR = sensorRotation.toYpr();
            //gyroDroidQuaternion = new Quaternion(YPR[0], YPR[1]-(float)(Math.PI/2.0), YPR[2]);
            gyroDroidQuaternion = sensorRotation;
            //gyroDroidQuaternion = new Quaternion(0, (float)(-Math.PI/2.0), 0).multiply(sensorRotation);
            
            // Calculate azimuth (0-360)
            float Rtmp[] = new float[9];
            float R[] = new float[9];
            try {
            	SensorManager.getRotationMatrixFromVector(Rtmp, event.values);
            } catch(IllegalArgumentException e) {
            	// Samsung error
            	
            }
            
            SensorManager.remapCoordinateSystem(Rtmp,
                        SensorManager.AXIS_X, SensorManager.AXIS_Z,
                        R);
                        
            float orientation[] = new float[3];
            SensorManager.getOrientation(R, orientation);
            azimuth = ((float)Math.toDegrees(orientation[0])+360)%360;

        } else if (event.sensor == orientation) {
            // if no rotationvector sensor available, fall back to orientation (deprecated)
            if (rotationVector == null) {
                Quaternion sensorRotation = new Quaternion(event.values[0], event.values[1] - (float)(Math.PI/2.0), event.values[2]); // yaw, pitch, roll
            }
            
        } else if (event.sensor == linearAcceleration) {
            linAcc = event.values;
//            linAcc[0] = (float)(0.95*linAcc[0] + 0.05*event.values[0]);
//            linAcc[1] = (float)(0.95*linAcc[1] + 0.05*event.values[1]);
//            linAcc[2] = (float)(0.95*linAcc[2] + 0.05*event.values[2]);
        }

    }
    
    public Quaternion getScreenRotation() {
        Quaternion screenRotation;
        switch (windowManager.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_0: screenRotation = new Quaternion(0, 0, 0); break;
            case Surface.ROTATION_90: screenRotation = new Quaternion((float)-Math.PI/2.0f, 0, 0); break;
            case Surface.ROTATION_180: screenRotation = new Quaternion((float)Math.PI, 0, 0); break;
            case Surface.ROTATION_270: screenRotation = new Quaternion((float)Math.PI/2.0f, 0, 0); break;
            default: screenRotation = new Quaternion(0, 0, 0); break;
        }
        return screenRotation;
    }
    
    public void resetGyros() {
        if (accRoll != 0.0f || accPitch != 0.0f) {
            glassfitQuaternion = new Quaternion(0.0f, accPitch, accRoll);
            Log.i("SensorService","Orientation has been reset to accelerometers");
        }
    }
    
    public static final float EPSILON = 0.000000001f;
    
    private float[] getRotationVectorFromGyro(float[] gyroValues, float deltaTime) {
        float[] deltaRotationVector = new float[4];
        float[] normValues = new float[3];
     
        // Calculate the angular speed of the sample
        float omegaMagnitude =
            (float)Math.sqrt(gyroValues[0] * gyroValues[0] +
            gyroValues[1] * gyroValues[1] +
            gyroValues[2] * gyroValues[2]);
     
        // Normalise the rotation vector if it's big enough to get the axis
        if(omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude;  //pitch=x
            normValues[1] = gyroValues[1] / omegaMagnitude;  //yaw=y
            normValues[2] = gyroValues[2] / omegaMagnitude;  //roll=z
        }
     
        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // Return as quaternion
        float thetaOverTwo = omegaMagnitude * deltaTime / 2.0f;
        float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
        deltaRotationVector[3] = cosThetaOverTwo;
        return deltaRotationVector;
    }
    
//    private float[] getRotationMatrixFromOrientation(float[] o) {
//        float[] xM = new float[9];
//        float[] yM = new float[9];
//        float[] zM = new float[9];
//     
//        float sinX = (float)Math.sin(o[1]);
//        float cosX = (float)Math.cos(o[1]);
//        float sinY = (float)Math.sin(o[2]);
//        float cosY = (float)Math.cos(o[2]);
//        float sinZ = (float)Math.sin(o[0]);
//        float cosZ = (float)Math.cos(o[0]);
//     
//        // rotation about x-axis (pitch)
//        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
//        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
//        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;
//     
//        // rotation about y-axis (roll)
//        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
//        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
//        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;
//     
//        // rotation about z-axis (azimuth)
//        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
//        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
//        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;
//     
//        // rotation order is y, x, z (roll, pitch, azimuth)
//        float[] resultMatrix = matrixMultiplication(xM, yM);
//        resultMatrix = matrixMultiplication(zM, resultMatrix);
//        return resultMatrix;
//    }
    
    private float[] matrixMultiplication(float[] A, float[] B) {
        
        // reject if matrices are different sizes
        if (A.length != B.length) {
            throw new IllegalArgumentException("Helper: Matrix multiplication - dimensions do not match.");
        }
        
        // invert both matrices into column-major format
        float[] At = new float[A.length];
        float[] Bt = new float[B.length];
        float[] Rt = new float[A.length];
        android.opengl.Matrix.transposeM(At, 0, A, 0);
        android.opengl.Matrix.transposeM(Bt, 0, B, 0);
        android.opengl.Matrix.multiplyMM(Rt, 0, At, 0, Bt, 0);
        
        // invert the result
        float[] R = new float[A.length];
        android.opengl.Matrix.transposeM(R, 0, Rt, 0);
     
//        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
//        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
//        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];
//     
//        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
//        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
//        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];
//     
//        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
//        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
//        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];
     
        return R;
    }
    
    private float[] getQuaternionFromRotationMatrix(float[] rotMat) {
        float[] quat = new float[4];
        if (rotMat.length == 16) {
            float trace = rotMat[0] + rotMat[5] + rotMat[10] + rotMat[15];
            quat[3] = (float)(0.5f*Math.sqrt(1+trace));
            float s = 1.0f/quat[3];
            quat[0] = (rotMat[9]-rotMat[6])*s;
            quat[1] = (rotMat[2]-rotMat[8])*s;
            quat[2] = (rotMat[4]-rotMat[1])*s;
            
        } else {
            Log.e("SensorService","Function not implemented: getQuaternionFromRotationMatrix() currently only works for 4x4 matrices.");
        }
        return quat;
    }
    
    public Quaternion getGyroDroidQuaternion() {
        return gyroDroidQuaternion;
    }
    
    public float[] getAccValues() {
        return acc;
    }
    
    public Vector3D getDeviceAccelerationVector() {
        return deviceAcceleration;
    }
    
    public Vector3D getRealWorldAccelerationVector() {
        return realWorldAcceleration;
    }

    public float[] getGyroValues() {
        return gyro;
    }
    
    public float[] getMagValues() {
        return mag;
    }
    
    public float[] getWorldToDeviceRotationVector() {
        return worldToDeviceRotationVector;
    }
    
    public float[] getDeviceToWorldRotationVector() {
        return deviceToWorldRotationVector;
    }
    
    public float[] getYprValues() {
        return ypr;
    }
    
    public float[] getGameYpr() {
        return gameYpr;
    }
    
    public Quaternion getGlassfitQuaternion() {
        return glassfitQuaternion;
    }
    
    public Quaternion getDeltaQuaternion() {
        return deltaQuaternion;
    }    
    
    public Quaternion getCorrection() {
        return accelQuaternion;
    }
    
    public Quaternion getRotationVectorQuaternion() {
        return new Quaternion(worldToDeviceRotationVector);
    }
    
    public float[] getLinAccValues() {
        return linAcc;
    }
    
    public float getAccRoll() {
        return accRoll;
    }
    
    public float getAccPitch() {
        return accPitch;
    }
    
    public float getFusedPitch() {
        return fusedPitch;
    }
    
    public float getFusedRoll() {
        return fusedRoll;
    }    
    
    public float getAzimuth() {
    	return azimuth;
    }
    public float[] rotateToRealWorld(float[] inVec) {
        float[] resultVec = new float[4];
        Matrix.multiplyMV(resultVec, 0, deviceToWorldTransform, 0, inVec, 0);
        return resultVec;
    }
    
    /**
     * Computes the component of the device acceleration along a given axis (e.g. forward-backward)
     * @param float[] (x,y,z) unit vector in real-world space
     * @return acceleration along the given vector in m/s/s
     */
    public float getAccelerationAlongAxis(float[] axisVector) {
        float forwardAcceleration = dotProduct(getRealWorldAcceleration(), axisVector);
        return forwardAcceleration;
    }
    
    /**
     * Computes the magnitude of the device's acceleration vector
     * @return
     */
    public float getTotalAcceleration() {
        float[] rawAcceleration3 = getLinAccValues();
        if (linearAcceleration == null) {
            // Fall back to accelerometer
            rawAcceleration3 = getAccValues();
        }
        double mag = Math.sqrt(Math.pow(rawAcceleration3[0],2) + Math.pow(rawAcceleration3[1],2) + Math.pow(rawAcceleration3[2],2));
        if (linearAcceleration == null) {
            // Remove gravity
            mag = Math.abs(mag-9.82);
        }
        return (float)mag;
    }
    
    /** 
     * Get the devices current acceleration in it's own co-ordinates
     * @return 3D acceleration vector in device co-ordinates
     */
    public float[] getDeviceAcceleration() {
        return getLinAccValues();
    }
    
    /**
     * Get the device's current acceleration in real-world space
     * @return 3D acceleration vector in real-world co-ordinates
     */
    public float[] getRealWorldAcceleration() {
        float[] rawAcceleration3 = getLinAccValues();
        float[] rawAcceleration4 = {rawAcceleration3[0], rawAcceleration3[1], rawAcceleration3[2], 0};
        float[] realWorldAcceleration4 = rotateToRealWorld(rawAcceleration4);
        float[] realWorldAcceleration3 = {realWorldAcceleration4[0], realWorldAcceleration4[1], realWorldAcceleration4[2]};
        return realWorldAcceleration3;
    }
    
    /**
     * Compute the dot-product of the two input vectors
     * @param v1d
     * @param v2
     * @return dot-product of v1 and v2
     */
    public static float dotProduct(float[] v1, float[] v2) {
        if (v1.length != v2.length) {
            Log.e("GPSTracker", "Failed to compute dot product of vectoers of different lengths.");
            return -1;
        }
        float res = 0;
        for (int i = 0; i < v1.length; i++)
            res += v1[i] * v2[i];
        return res;
    }
    
    private static float matrixSum(float[] matrix) {
        float result = 0.0f;
        for (float f : matrix) {
            result += f;
        }
        return result;
    }

  }
