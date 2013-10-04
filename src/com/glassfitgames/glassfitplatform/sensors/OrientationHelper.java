
package com.glassfitgames.glassfitplatform.sensors;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.glassfitgames.glassfitplatform.auth.AuthenticationActivity;
import com.glassfitgames.glassfitplatform.models.Orientation;
import com.glassfitgames.glassfitplatform.R;

public class OrientationHelper extends Activity {

    private Context context;
    private SensorService sensorService;
    
    private Button getOrientationButton;
    private TextView orientationText;

    private float roll; // Roll
    private float pitch; // Pitch
    private float yaw; // Yaw
    private float mag_x; // Magnetometer x-axis
    private float mag_y; // Magnetometer y-axis
    private float mag_z; // Magnetometer z-axis
    private float acc_x; // Accelerometer x-axis
    private float acc_y; // Accelerometer y-axis
    private float acc_z; // Accelerometer z-axis
    private float gyro_x; // Gyroscope x-axis
    private float gyro_y; // Gyroscope y-axis
    private float gyro_z; // Gyroscope z-axis
    
    private Timer timer;
    private OrientationTask task;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.orientation_helper);
        
        getOrientationButton = (Button)findViewById(R.id.getOrientationButton);
        getOrientationButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentOrientation();
            }
        });
        orientationText = (TextView)findViewById(R.id.orientationText);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, SensorService.class), sensorServiceConnection,
                        Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (task != null) task.cancel();
        unbindService(sensorServiceConnection);
    }

    private ServiceConnection sensorServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            sensorService = ((SensorService.SensorServiceBinder)binder).getService();
            Log.d("GlassFitPlatform", "OrientationHelper has bound to SensorService");
            timer = new Timer();
            task = new OrientationTask();
            timer.scheduleAtFixedRate(task, 0, 50);            
        }

        public void onServiceDisconnected(ComponentName className) {
            sensorService = null;
            Log.d("GlassFitPlatform", "OrientationHelper has unbound from SensorService");
        }
    };
    
    private class OrientationTask extends TimerTask {
        public void run() {
            runOnUiThread(new Runnable() {
                public void run() {
                    getCurrentOrientation();
                }
            });
        }
    }

    public Orientation getCurrentOrientation() {
        
        Log.v("GlassFitPlatform", "Accel: x:" + sensorService.getAccValues()[0] + ", y:" + sensorService.getAccValues()[1] + ", z:" + sensorService.getAccValues()[2] + "m/s/s." );
        Log.v("GlassFitPlatform", "Gyro: x:" + sensorService.getGyroValues()[0] + ", y:" + sensorService.getGyroValues()[1] + ", z:" + sensorService.getGyroValues()[2] + "rad." );
        Log.v("GlassFitPlatform", "Mag: x:" + sensorService.getMagValues()[0] + ", y:" + sensorService.getMagValues()[1] + ", z:" + sensorService.getMagValues()[2] + "uT." );
        
        String oText = new String();
        DecimalFormat df = new DecimalFormat("#.##");
        
        oText += "Accel: x:" + df.format(sensorService.getAccValues()[0]) + ", y:" + df.format(sensorService.getAccValues()[1]) + ", z:" + df.format(sensorService.getAccValues()[2]) + "m/s/s.\n";
        oText += "Gyro: x:" + df.format(sensorService.getGyroValues()[0]) + ", y:" + df.format(sensorService.getGyroValues()[1]) + ", z:" + df.format(sensorService.getGyroValues()[2]) + "rad.\n";
        oText += "Mag: x:" + df.format(sensorService.getMagValues()[0]) + ", y:" + df.format(sensorService.getMagValues()[1]) + ", z:" + df.format(sensorService.getMagValues()[2]) + "uT.\n";
        oText += "RPY: x:" + df.format(sensorService.getYprValues()[0]) + ", y:" + df.format(sensorService.getYprValues()[1]) + ", z:" + df.format(sensorService.getYprValues()[2]) + "degrees.\n";
        oText += "LinAcc: x:" + df.format(sensorService.getLinAccValues()[0]) + ", y:" + df.format(sensorService.getLinAccValues()[1]) + ", z:" + df.format(sensorService.getLinAccValues()[2]) + "m/s.\n";
        oText += "\n";
        
        oText += "Current Yaw: " + df.format(sensorService.getYprValues()[0]) + "\n";
        oText += "Current Pitch: " + df.format(sensorService.getYprValues()[1]) + "\n";
        oText += "Current Roll: " + df.format(sensorService.getYprValues()[2]);
        orientationText.setText(oText);
        
        Orientation o = new Orientation();
        
        o.setAccelerometer(sensorService.getAccValues());
        o.setGyroscope(sensorService.getGyroValues());
        o.setMagnetometer(sensorService.getMagValues());
        o.setRotationVector(sensorService.getQuatValues());
        o.setYawPitchRoll(sensorService.getYprValues());
        o.setLinearAcceleration(sensorService.getLinAccValues());
        o.setTimestamp(System.currentTimeMillis());
        o.save();
        return o;
    }

}
