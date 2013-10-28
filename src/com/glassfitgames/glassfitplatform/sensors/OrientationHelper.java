
package com.glassfitgames.glassfitplatform.sensors;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.glassfitgames.glassfitplatform.auth.AuthenticationActivity;
import com.glassfitgames.glassfitplatform.models.Orientation;
import com.glassfitgames.glassfitplatform.R;
import com.roscopeco.ormdroid.Entity;

public class OrientationHelper extends Activity {

    private SensorService sensorService;
    
    private Button getOrientationButton;
    private TextView orientationText;
    
    private Timer timer;
    private OrientationTask task;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.orientation_helper);
        
        getOrientationButton = (Button)findViewById(R.id.getOrientationButton);
        orientationText = (TextView)findViewById(R.id.orientationText);
        
        getOrientationButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentOrientation();
            }
        });
        
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
        
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            Log.i("OrientationHelper","External storage is mounted as writeable.");
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            Log.w("OrientationHelper","External storage is mounted as read-only.");
            return;
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            Log.w("OrientationHelper","External storage is not available.");
            return;
        }
        
        Orientation o = new Orientation();
        try {
            File file = new File(getExternalFilesDir(null), "OrientationData.csv");
            Log.i("OrientationHelper","External File dir is: " + getExternalFilesDir(null));
            file.getParentFile().mkdirs();
            Log.i("OrientationHelper","Directories created ok");
            if (!file.exists()) file.createNewFile();
            Log.i("OrientationHelper","Writing orientation data to " + file.getAbsolutePath());
            o.allToCsv(file);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private ServiceConnection sensorServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            sensorService = ((SensorService.SensorServiceBinder)binder).getService();
            Log.d("GlassFitPlatform", "OrientationHelper has bound to SensorService");
            
            // clear existing orientations from database
            List<Orientation> existingOrientations = Entity.query(Orientation.class).executeMulti();
            for (Orientation o : existingOrientations) {
                o.delete();
            }
            
            // start polling the sensors
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
        oText += "LinAcc: x:" + df.format(sensorService.getLinAccValues()[0]) + ", y:" + df.format(sensorService.getLinAccValues()[1]) + ", z:" + df.format(sensorService.getLinAccValues()[2]) + "m/s.\n\n";
        oText += "\n";
        
        oText += "Current Yaw: " + df.format(sensorService.getYprValues()[0]) + "\n";
        oText += "Current Pitch: " + df.format(sensorService.getYprValues()[1]) + "\n";
        oText += "Current Roll: " + df.format(sensorService.getYprValues()[2]) + "\n\n";
        
        oText += "Game Yaw: " + df.format(sensorService.getGameYpr()[0]) + "\n";
        oText += "Game Pitch: " + df.format(sensorService.getGameYpr()[1]) + "\n";
        oText += "Game Roll: " + df.format(sensorService.getGameYpr()[2]);
        
        orientationText.setText(oText);
        
        Orientation o = new Orientation();
        
        o.setAccelerometer(sensorService.getAccValues());
        o.setGyroscope(sensorService.getGyroValues());
        o.setMagnetometer(sensorService.getMagValues());
        //o.setRotationVector(sensorService.getQuatValues());
        o.setYawPitchRoll(sensorService.getYprValues());
        o.setLinearAcceleration(sensorService.getLinAccValues());
        o.setTimestamp(System.currentTimeMillis());
        //o.save();
        return o;
    }

}
