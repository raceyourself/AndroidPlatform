package com.glassfitgames.glassfitplatform.BLE;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

/**
 * Created by Dmitry on 22/09/13.
 */
public final class GlassFit_BLE_SPEEDO extends GlassFit_BLE {
    private int cumulative_wheel_revolutions;
    private int cumulative_crank_revolutions;
    private double current_speed;
    BluetoothLeService_SPEEDO mBluetoothLeService;
    private BluetoothGattCharacteristic mCSCMeasurementCharacteristic;
    private int old_wheel_event_timestamp;
    private int old_cumulative_wheel_revolutions;
    private int old_cumulative_crank_revolutions;
    private int old_crank_event_timestamp;
    private double current_cadence;
    private double total_distance;
    private int base_wheel_revolutions;
    private boolean is_first=true;
    private long old_time_millis;
    private boolean auto_reconnect=false;
    private String device_name;
    int wheel_event_difference;
    int crank_event_difference;
    private BluetoothGattCharacteristic notif_gatt_charc;

    private boolean is_service_connected=false;


    public GlassFit_BLE_SPEEDO(Activity a){
        super(a);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService_SPEEDO.LocalBinder) service).getService();

            Log.d(TAG,"In onServiceConnected...");
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                return;
            }
            // Automatically connects to the device upon successful start-up initialization.
            //TODO change this to be able to choose and check which device to connect to
            if (!mLeDevices.isEmpty()){
                BluetoothDevice device=mLeDevices.get(0);
                if (device!=null)
                {
                    mBluetoothLeService.connect(device.getAddress());
                    is_service_connected=true;
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            is_service_connected=false;
        }
    };

    public void StartDevice(){


        //re-connect (not sure if closing the connection first is a good idea)
        if (is_service_connected){
            Log.d(TAG, "Trying to connect to device but service is already connected");
            //CloseDevice(); //close it first
        }

        //only try to get services if we have the device
        if (!mLeDevices.isEmpty() /*&& !is_service_connected*/){

            Log.d(TAG,"Attempting to start speedo device");
            Intent gattServiceIntent = new Intent(mParentActivity, BluetoothLeService_SPEEDO.class);
            mParentActivity.startService(gattServiceIntent);

            mParentActivity.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

            mParentActivity.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
            Log.d(TAG,"Started service and registered receiver++");
            is_service_connected=true;
            is_first=true;

        }
        //The service will be start and in OnStart() it will connect to the first device in our list
    }


    //To be called in main application's onDestroy
    public void CloseDevice(){


        if (is_service_connected)
        {
            //Cancel notifications for the CSC characteristic
           // mBluetoothLeService.setCharacteristicNotification(notif_gatt_charc,false);
            //Need to wait for this to finish, its async
            mBluetoothLeService.disconnect();
            //mBluetoothLeService.close();
            //Shall we wait here too?
            mLeDevices.clear();
            mParentActivity.unbindService(mServiceConnection);
            is_service_connected=false;

        }
        mBluetoothLeService = null;

    }





    //Go through the device(s) and print their services and characteristics
    //essentially prune the list to find out device


    //The behaviour of this method changes with the nature of bluetooth device, so must be overridden
    //and appended in the subclasses
    //however GATT_CONNECTED and GATT_DISCONNECTED stay the same


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            RespondToBLEIntent(intent);
        }
    };

    public void RequestDeviceName(){
        //only request device name if the name characteristic has been defined
        if (mDeviceNameCharacteristic!=null)
            mBluetoothLeService.readCharacteristic(mDeviceNameCharacteristic);
    }



    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService_SPEEDO.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService_SPEEDO.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService_SPEEDO.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService_SPEEDO.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }





    protected void RespondToBLEIntent(Intent intent){


        final String action = intent.getAction();
        String uuid = null;
        if (BluetoothLeService_SPEEDO.ACTION_GATT_CONNECTED.equals(action)) {
            mConnected = true;
            //strictly speaking this has
            //is_service_connected=true;
            Log.d(TAG,"GATT is now connected (here in subclass)");

        } else if (BluetoothLeService_SPEEDO.ACTION_GATT_DISCONNECTED.equals(action)) {
            mConnected = false;
            //is_service_connected=false;
            Log.d(TAG,"GATT is now DISconnected (here subclass)");
            mBLE_Callback_parent.onDeviceDisconnected(device_name);

            //TODO copy over code from HR

        }

        if (BluetoothLeService_SPEEDO.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
            // Show all the supported services and characteristics on the user interface.
            gattServices=mBluetoothLeService.getSupportedGattServices();
            //Now we need to loop through the services
            for (BluetoothGattService gattService : gattServices){
                uuid = gattService.getUuid().toString();
                Log.d(TAG, "UUID of the service is " + uuid);
                List<BluetoothGattCharacteristic> gattCharacteristics=gattService.getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    uuid=gattCharacteristic.getUuid().toString();
                    Log.d(TAG,"Charc UUID is "+uuid);


                    if (uuid.contains("2a24")){ //this is the device name (something else for speedo)
                        //Cannot read actual characteristics here - need to
                        //save this characteristic (don't need in future)
                        mDeviceNameCharacteristic=gattCharacteristic;


                    }
                    if (uuid.contains("2a5b")){

                        //Set up a notification for this characteristic, we cannot read it directly
                        int charc_prop=gattCharacteristic.getProperties();
                        notif_gatt_charc=gattCharacteristic;
                        if ((charc_prop|BluetoothGattCharacteristic.PROPERTY_NOTIFY)>0){
                            Log.d(TAG,"Has just set notification for CSC");
                            mBluetoothLeService.setCharacteristicNotification(gattCharacteristic,true);
                        }
                        mCSCMeasurementCharacteristic=gattCharacteristic;
                    }
                }
            }
        } else if (BluetoothLeService_SPEEDO.ACTION_DATA_AVAILABLE.equals(action)) {
            //TODO extract these ifs into a separate, clear routines,

            if (intent.getStringExtra(BluetoothLeService_SPEEDO.CHARACTERISTIC_TYPE).equals(BluetoothLeService_SPEEDO.DATA_TYPE_DEVICE_NAME)){
                deviceName=intent.getStringExtra(BluetoothLeService_SPEEDO.EXTRA_DATA);
                Log.d(TAG,"Device name is "+deviceName);

            }
            else if (intent.getStringExtra(BluetoothLeService_SPEEDO.CHARACTERISTIC_TYPE).equals(BluetoothLeService_SPEEDO.DATA_TYPE_CSC_EVENT)){

                //Log.d(TAG,encoded_csc);


                cumulative_wheel_revolutions=intent.getIntExtra(BluetoothLeService_SPEEDO.DATA_TYPE_CUMULATIVE_WHEEL_REVOLUTIONS,0);
                cumulative_crank_revolutions=intent.getIntExtra(BluetoothLeService_SPEEDO.DATA_TYPE_CUMULATIVE_CRANK_REVOLUTIONS,0);
                //Let's work out speed
                int new_wheel_event_timestamp=intent.getIntExtra(BluetoothLeService_SPEEDO.DATA_TYPE_LAST_WHEEL_EVENT_TIME,0);
                int new_crank_event_timestamp=intent.getIntExtra(BluetoothLeService_SPEEDO.DATA_TYPE_LAST_CRANK_EVENT_TIME,0);
                // Log.d(TAG,"Wheel event:"+new_wheel_event_timestamp);
                //time difference between last two wheel events, units = seconds

                if (is_first){
                    base_wheel_revolutions =cumulative_wheel_revolutions;
                    is_first=false;
                }

                double wheel_time_diff=0;
                int diff=0;
                if (new_wheel_event_timestamp>old_wheel_event_timestamp){
                    diff=new_wheel_event_timestamp-old_wheel_event_timestamp;
                    Log.d(TAG,"wheel diff "+Integer.toString(diff));

                }
                //if we wrapped around
                else if (new_wheel_event_timestamp<old_wheel_event_timestamp){
                    diff=(65536-old_wheel_event_timestamp)+new_wheel_event_timestamp;

                }

                wheel_event_difference=diff;
                //Need to take into the account number of wheel revolutions - could (and likely to be) more than 1

                wheel_time_diff=((double)diff)/1024;

                if (wheel_time_diff>0){
                    int wheel_revs=cumulative_wheel_revolutions-old_cumulative_wheel_revolutions;
                    Log.d(TAG,"Wheel revs "+wheel_revs);
                    current_speed=((1/wheel_time_diff*wheel_revs)*3600*0.7*Math.PI)/1000;
                    old_wheel_event_timestamp=new_wheel_event_timestamp;

                } else if (wheel_time_diff==0)
                    current_speed=0;


                old_cumulative_wheel_revolutions=cumulative_wheel_revolutions;

                //current_speed=diff;

                //==================================================================================
                //now the same for cadence
                double crank_time_diff=0;
                diff=0;
                if (new_crank_event_timestamp>old_crank_event_timestamp){
                    diff=new_crank_event_timestamp-old_crank_event_timestamp;


                }
                //if we wrapped around
                else if (new_crank_event_timestamp<old_crank_event_timestamp){
                    diff=(65536-old_crank_event_timestamp)+new_crank_event_timestamp;
                }

                crank_event_difference=diff;
                crank_time_diff=((double)diff)/1024;

                if (crank_time_diff>0){
                    int crank_revs=cumulative_crank_revolutions-old_cumulative_crank_revolutions;
                    Log.d(TAG,"crank diff "+Integer.toString(diff)+" crank revs "+crank_revs);

                    //Perhaps need some smoothing here
                    current_cadence=((1/crank_time_diff*crank_revs))*60;
                    old_crank_event_timestamp=new_crank_event_timestamp;
                } else if (crank_time_diff==0)
                    current_cadence=0;


                    Log.d(TAG,"cad "+current_cadence);

                old_cumulative_crank_revolutions=cumulative_crank_revolutions;


                //===============================================================
                //Now calculate total distance, in meters
                //Roughly 2100mm for a 700c road wheel
                //Need to calibrate
                total_distance=(cumulative_wheel_revolutions- base_wheel_revolutions)*2.1;




                mBLE_Callback_parent.onSPEEDOUpdate();
            }




        }

    }



    protected void  IdentifyNewDevice(BluetoothDevice device){
        String name=device.getName();

        if (/*name.contains("RB CSC") ||*/ name.contains("Cycling sensor V11")){
            mLeDevices.add(device);
            scanLeDevice(false);//once we found one, stop searching
            device_name=name;
            Log.d(TAG,"Device found and added:"+name);
            mBLE_Callback_parent.onDeviceFound(name);


        }
    }

    public enum SPEED_UNIT{
        KMPH, MPH, MS
    }

    public enum CADENCE_UNIT{
        RPM, HZ
    }

    public double getCurrentSpeed(SPEED_UNIT units){

        return current_speed;
    }

    public double getCurrentCadence(CADENCE_UNIT units){
        return current_cadence;
    }

    public int getCumulativeWheelRevolutions(){
        return cumulative_wheel_revolutions;
    }

    public int getCumulativeCrankRevolutions(){
        return cumulative_crank_revolutions;
    }

    public double getTotalDistance(){
    return total_distance;

    }


    public int getWheelEventDifference(){
        return wheel_event_difference;
    }

    public int getCrankEventDifference(){
        return crank_event_difference;

    }
    public void ResetTotalDistance(){
        total_distance=0;

    }



}