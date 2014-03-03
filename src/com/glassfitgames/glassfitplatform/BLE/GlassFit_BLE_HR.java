package com.glassfitgames.glassfitplatform.BLE;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.opengl.EGLExt;
import android.os.IBinder;
import android.util.Log;
import java.util.List;

/**
 * Created by Dmitry on 22/09/13.
 */
public final class GlassFit_BLE_HR extends GlassFit_BLE{
    private int heartRate=0;
    private String device_name;

    protected BluetoothLeService_HR mBluetoothLeService;
    private boolean is_service_connected=false;
    private boolean auto_reconnect=false;
    private int auto_reconnect_attempts=0;


    public GlassFit_BLE_HR(Context a){
        super(a);
    }


    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService_HR.LocalBinder) service).getService();

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
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    public void Set_auto_reconnect(boolean value){
        auto_reconnect=value;
        auto_reconnect_attempts=10;
    }
    //Go through the device(s) and print their services and characteristics
    //essentially prune the list to find out device
    public void StartDevice(){
    	Log.d(TAG,"Trying to start the device");
    	if (mLeDevices.isEmpty()) Log.d(TAG,"Devices list is empty");

        if (!mLeDevices.isEmpty()&&!is_service_connected){
            Intent gattServiceIntent = new Intent(mParentActivity, BluetoothLeService_HR.class);
            mParentActivity.startService(gattServiceIntent);
            mParentActivity.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

            if (mParentActivity.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())!=null){
                Log.d(TAG,"Started service and registered receiver");
                is_service_connected=true;
            }
            else Log.d(TAG,"Registering receiver failed");
            //The service will be start and in OnStart() it will connect to the first device in our list
        }
    }
    //To be called in main application's onDestroy
    public void CloseDevice(){
        if (is_service_connected)
        {
            mParentActivity.unbindService(mServiceConnection);
            is_service_connected=false;
            mBluetoothLeService.disconnect();
            mBluetoothLeService.close();
        }
        mBluetoothLeService = null;
    }



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
        intentFilter.addAction(BluetoothLeService_HR.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService_HR.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService_HR.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService_HR.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }



    protected void RespondToBLEIntent(Intent intent){
        //Call the super. method to handleCONNECTED and DISCONNECTED events
        final String action = intent.getAction();
        String uuid = null;
        if (BluetoothLeService_HR.ACTION_GATT_CONNECTED.equals(action)) {

            Log.d(TAG,"GATT is now connected (here in superclass)");
            mConnected = true;
        } else if (BluetoothLeService_HR.ACTION_GATT_DISCONNECTED.equals(action)) {

            Log.d(TAG,"GATT is now DISconnected (here in superclass)");
            mBLE_Callback_parent.onDeviceDisconnected(device_name);
            if (auto_reconnect && auto_reconnect_attempts>0){
                Log.d(TAG,"Attempting to reconnect...");
                auto_reconnect_attempts--;
                //we do not need to mess around with the service, connection with which won't be affected
                //instead, we need to instruct the service to reconnect
                mBluetoothLeService.connect(mLeDevices.get(0).getAddress());
                //if (is_service_connected)
                //Log.d(TAG,"Successfully reconnected");
            }
            else {
                Log.d(TAG,"Gonna try to disconnect this fucker!");
                mBluetoothLeService.disconnect();
                CloseDevice();
            }
            mConnected = false;

        }


        if (BluetoothLeService_HR.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
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
                    //Now check if this chara is HR measurement and if so enable notification for it
                    if (uuid.contains("2a37")){ //this is the actual heart rate
                        int charc_prop=gattCharacteristic.getProperties();
                        if ((charc_prop|BluetoothGattCharacteristic.PROPERTY_NOTIFY)>0){
                            Log.d(TAG,"Has just set notification for HR measurement");
                            mBluetoothLeService.setCharacteristicNotification(gattCharacteristic,true);
                        }


                    }
                    if (uuid.contains("2a24")){ //this is the device name
                        //Cannot read actual characteristics here - need to
                        //save this characteristic (don't need in future)
                        mDeviceNameCharacteristic=gattCharacteristic;


                    }
                }
            }
        } else if (BluetoothLeService_HR.ACTION_DATA_AVAILABLE.equals(action)) {
            // Log.d(TAG,"Got some DATA!!!");
            if (intent.getStringExtra(BluetoothLeService_HR.CHARACTERISTIC_TYPE).equals(BluetoothLeService_HR.DATA_TYPE_HEART_RATE)){
                Log.d(TAG, "HR rate is"+intent.getStringExtra(BluetoothLeService_HR.EXTRA_DATA));
                heartRate=Integer.parseInt(intent.getStringExtra(BluetoothLeService_HR.EXTRA_DATA));
                if (heartRate<30) heartRate=30;
                if (heartRate>215) heartRate=215; //just a bit of sanity checking
                mBLE_Callback_parent.onHRUpdate();
            }
            else if (intent.getStringExtra(BluetoothLeService_HR.CHARACTERISTIC_TYPE).equals(BluetoothLeService_HR.DATA_TYPE_DEVICE_NAME)){
                //TODO replace two device name with something sensible
                deviceName=intent.getStringExtra(BluetoothLeService_HR.EXTRA_DATA);
                Log.d(TAG,"Device name is "+deviceName);

            }
        }
    }

    void IdentifyNewDevice(BluetoothDevice device){

        String name=device.getName();
        //first name string is what's in the iSport device and the second is Millenium concept (HR1)
        if (name.equals("Heart Rate Sensor")||name.equals("Heartrate Sensor")){
            mLeDevices.add(device);
            scanLeDevice(false);//once we found one, stop searching
            device_name=name;
            Log.d(TAG,"Found and added device "+name);

        }
        mBLE_Callback_parent.onDeviceFound(name);
    }

    public int getHeartRate(){
        return heartRate;
    }



}


