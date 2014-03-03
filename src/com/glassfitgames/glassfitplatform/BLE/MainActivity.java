//package com.glassfitgames.glassfitplatform.BLE;
//
//
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.content.Context;
//import android.content.Intent;
//
//import android.media.Ringtone;
//import android.media.RingtoneManager;
//import android.net.Uri;
//import android.os.Bundle;
//import android.app.Activity;
//import android.os.Environment;
//import android.os.Vibrator;
//import android.support.v4.app.NotificationCompat;
//import android.support.v4.app.TaskStackBuilder;
//import android.text.format.Time;
//import android.text.method.ScrollingMovementMethod;
//import android.util.Log;
//import android.view.Menu;
//import android.view.View;
//import android.widget.Button;
//import android.widget.TextView;
//
//import java.io.File;
//import java.io.FileWriter;
//
//import java.util.Calendar;
//
//
//public class MainActivity extends Activity implements GlassFit_BLE_Callbacks{
//    private GlassFit_BLE_HR ble_HR_monitor;
//    private GlassFit_BLE_SPEEDO ble_speedometer;
//
//    public Button button_init_BLE;
//    public TextView text_general;
//    public TextView text_log;
//    public TextView text_speed;
//    public TextView text_cadence;
//    public TextView text_distance;
//    String TAG=GlassFit_BLE.TAG;
//    FileWriter log_file=null;
//    private int heart_rate=0;
//    public TextView text_HR;
//    private NotificationManager mNotificationManager;
//    NotificationCompat.Builder mBuilder;
//    private Ringtone ringtone;
//    private Vibrator vib;
//
//
//    @Override
//    public void onHRUpdate() {
//
//        heart_rate=ble_HR_monitor.getHeartRate();
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                text_HR.setText(Integer.toString(heart_rate));
//            }
//        });
//        SavetoCSV();
//    }
//
//
//    @Override
//    public void onDeviceConnected(int event_id){
//        //TODO decide on how to convey the information
//    }
//    @Override
//    public void onDeviceFound(String name){
//        //could add name string to the callback
//        final String s=name;
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                text_log.append("Device found "+s+"\n");
//            }
//        });
//
//
//
//    }
//
//    @Override
//    public void onSPEEDOUpdate() {
//        String date= java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
//        final Time today = new Time(Time.getCurrentTimezone());
//        today.setToNow();
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                text_general.setText("Got Speedo update " + today.second);
//                String speed_value = String.format("%.2f", ble_speedometer.getCurrentSpeed(GlassFit_BLE_SPEEDO.SPEED_UNIT.KMPH));
//                int cadence=(int)(ble_speedometer.getCurrentCadence(GlassFit_BLE_SPEEDO.CADENCE_UNIT.RPM));
//                String cadence_value=Integer.toString(cadence);
//                String total_distance_value=String.format("%.1f", (ble_speedometer.getTotalDistance() / 1000));
//                //text_log.append("Speed " + speed_value + "\n");
//                text_speed.setText(speed_value);
//                text_cadence.setText(cadence_value+"rpm");
//                text_distance.setText(total_distance_value+"km");
//
//            }
//        });
//
//
//        //now lets log it into the file :-)
//        SavetoCSV();
//
//    }
//    @Override
//    public void onDeviceDisconnected(String name){
//        final String s=name;
//        final Time today = new Time(Time.getCurrentTimezone());
//        today.setToNow();
//        final String time=Integer.toString(today.hour)+" "+Integer.toString(today.minute);
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    ringtone.play();
//                } catch (Exception e) {Log.d(TAG,"Ringtone didn't work");}
//
//                long[] vib_pattern={0, 500, 300, 500,300,500,300,500,300,500};
//                vib.vibrate(vib_pattern, -1);
//                text_log.append("Device disconnected "+time+"\n");
//
//            }
//        });
//    }
//
//
//    void SavetoCSV()
//    {
//        //Get time and date
//        String date= java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
//        Time today = new Time(Time.getCurrentTimezone());
//        today.setToNow();
//        String value ="";
//        //Always record heart rate
//        if (ble_HR_monitor.isPhysicalDeviceDetected()){
//            value+=Integer.toString(ble_HR_monitor.getHeartRate());
//        }
//
//        if (ble_speedometer.isPhysicalDeviceDetected()){
//            String speed_value = String.format("%.2f" , ble_speedometer.getCurrentSpeed(GlassFit_BLE_SPEEDO.SPEED_UNIT.KMPH));
//            value+=speed_value;
//            int cadence=(int)(ble_speedometer.getCurrentCadence(GlassFit_BLE_SPEEDO.CADENCE_UNIT.RPM));
//            String cadence_value=Integer.toString(cadence);
//            value+=','+cadence_value;
//            value+=','+Integer.toString(ble_speedometer.getCumulativeWheelRevolutions());
//            value+=','+Integer.toString(ble_speedometer.getCumulativeCrankRevolutions());
//            value+=','+ Integer.toString(ble_speedometer.getWheelEventDifference());
//            value+=','+Integer.toString(ble_speedometer.getCrankEventDifference());
//        }
//        if (log_file!=null)
//        {
//            try{
//                log_file.append(date+","+value+"\n");
//            } catch (Exception e)
//            {
//                Log.d(TAG,"File log writing fucked up!");
//
//            }
//        }
//
//
//
//    }
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//
//        //Set up our BLE classes
//        ble_HR_monitor = new GlassFit_BLE_HR(this);
//        ble_speedometer = new GlassFit_BLE_SPEEDO(this);
//
//        //Set up UI controls
//        button_init_BLE= (Button)findViewById(R.id.button1);
//        button_init_BLE.setOnClickListener(button_init_BLE_click);
//        text_general=(TextView)findViewById(R.id.textView);
//        text_log=(TextView)findViewById(R.id.textView2);
//        text_HR=(TextView)findViewById(R.id.textView3);
//        text_log.setMovementMethod(new ScrollingMovementMethod());
//        text_speed=(TextView)findViewById(R.id.textView_speed);
//        text_distance=(TextView)findViewById(R.id.textView_dist);
//        text_cadence=(TextView)findViewById(R.id.textView_cadence);
//
//        //And non-visual components for notification
//        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//        ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
//        vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
//
//
//
//    }
//
//    private void StopAll(){
//        ble_speedometer.CloseDevice();
//        ble_HR_monitor.CloseDevice();
//
//        if (log_file!=null)
//        {
//            try {
//                log_file.close();
//            } catch (Exception e){
//                Log.d(TAG,"Could not even close goddamn file?!");
//            }
//        }
//    }
//
//    @Override
//    protected void onDestroy(){
//        super.onDestroy();
//        StopAll();
//
//    }
//
//
//
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode== GlassFit_BLE.REQUEST_ENABLE_BT && resultCode==Activity.RESULT_OK){
//            //Start scanning for devices
//            //ble_HR_monitor.ScanForBLEDevices(true);
//            //text_log.append("Started scanning for HR BLE devices\n\r");
//
//        }
//
//        super.onActivityResult(requestCode, resultCode, data);
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }
//
//    private View.OnClickListener button_init_BLE_click = new View.OnClickListener(){
//        @Override
//        public void onClick(View view) {
//            ble_HR_monitor.Set_auto_reconnect(true);
//
//            if (ble_HR_monitor.Check_BLE()){
//
//                //if already enabled
//                if (ble_HR_monitor.EnableBLE()){
//                    ble_HR_monitor.ScanForBLEDevices(true);
//                    text_log.append("Started scanning for HR BLE devices\n");
//                }
//
//            }
//
//
//        }
//    };
//
//
//    public void ButtonOnClick(View v){
//        switch (v.getId()){
//
//
//            case R.id.button_show_HR:
//                if (ble_HR_monitor.isPhysicalDeviceDetected())
//                    text_general.setText("HR found");
//                else text_general.setText("No HRs found yet");
//                ble_HR_monitor.StartDevice();
//                break;
//
//
//            case R.id.button_show_speedo:
//                if (ble_speedometer.isPhysicalDeviceDetected())
//                    text_general.setText("Speedo found");
//                else text_general.setText("No speedo found yet");
//                ble_speedometer.StartDevice();
//                break;
//
//            case R.id.button_HR_read_name:
//                ble_HR_monitor.RequestDeviceName();
//                text_general.setText(ble_HR_monitor.deviceName);
//
//                break;
//
//
//            case R.id.button_speedo_read_name:
//                ble_speedometer.RequestDeviceName();
//                text_general.setText(ble_speedometer.deviceName);
//                break;
//
//            case R.id.button_init_speedo:
//
//                if (ble_speedometer.Check_BLE()){
//
//                    //if already enabled
//                    if (ble_speedometer.EnableBLE()){
//                        ble_speedometer.ScanForBLEDevices(true);
//                        text_log.append("Started scanning for BLE devices\n\r");
//                    }
//
//                }
//
//                break;
//
//            case R.id.button_dist_reset:
//                ble_speedometer.ResetTotalDistance();
//                break;
//
//            case R.id.button_save_to_csv:
//
//                File folder = new File(Environment.getExternalStorageDirectory()
//                        + "/GlassFitLogs");
//
//                boolean var = false;
//                if (!folder.exists())
//                    var = folder.mkdir();
//
//                Log.d(TAG,"Got folder "+var);
//
//                String mydate = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
//                final String filename = folder.toString() + "/" + "BLE log "+mydate+".csv";
//                try {
//                    log_file = new FileWriter(filename);
//                }catch (Exception e) {
//                    Log.d(TAG,"File writing fucked up");
//                }
//
//                break;
//            case R.id.button_stop_all:
//                StopAll();
//
//                break;
//        }
//
//    }
//}
