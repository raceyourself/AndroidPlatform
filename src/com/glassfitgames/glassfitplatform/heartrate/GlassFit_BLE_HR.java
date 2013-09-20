package com.glassfitgames.glassfitplatform.heartrate;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dmitry on 14/09/13.
 */
@TargetApi(18)
public class GlassFit_BLE_HR {
    private final static String TAG = "BigD";
    private ArrayList<BluetoothDevice> mLeDevices;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeService mBluetoothLeService; //we declare an instance of the BLE service
    private boolean mScanning;
    private Handler mHandler;//only used during a time-limited thread for scanning for devices
    private Context mContext=null;
    private Activity mParentActivity=null;
    private List<BluetoothGattService> gattServices;
    private boolean mConnected;
    private int heartRate=0;
    private BluetoothGattCharacteristic mDeviceNameCharacteristic;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;


    //This must be passed to this class for it to be able to get BLE-related information about the system
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            Log.d(TAG,"In onServiceConnected...");
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mLeDevices.get(0).getAddress());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };




    public GlassFit_BLE_HR (Activity a){
        //Will use this for starting service and asking for permissions etc
        mParentActivity=a;
        mContext=a.getApplicationContext();
        mHandler = new Handler();
        mLeDevices = new ArrayList<BluetoothDevice>();
    }

    //returns true if we have the required BLE hardware and API18
    public boolean Check_BLE(){
        if (mContext==null)
            return false;
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return false;
        }
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter==null)
            return false;


        return true;
    }


    public void  EnableBLE() {


        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                mParentActivity.startActivity(enableBtIntent);
                //the problem is, we do care about the result but how do declare on
                //impossible to override onactivityresult of out parent activity without extending it :-(
            }
        }


        //  scanLeDevice(true);
    }
    public void ScanForBLEDevices(boolean s){
        if (!mBluetoothAdapter.isEnabled())
            //need to device a mechanism of informing the main app about the result
            return;
        scanLeDevice(s);
    }



    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            Log.d(TAG, "Started looking for devices");
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);

                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }

    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

                    //But we don't need to display anything in the UI thread!
                    //Actually need to add devices to a list here
                    mLeDevices.add(device);
                    Log.d(TAG, "Device added " + device.getName().toString());
                    mBluetoothAdapter.stopLeScan(mLeScanCallback); //once we found one, stop

                    //mLeDeviceListAdapter.addDevice(device);


                }
            };

    //Go through the device(s) and print their services and characteristics
    //essentially prune the list to find out device
    public void GetServices(){

        //firs we need to get/connect the gatt service...
        //and in turn to do that we need to start the BLE service
        //final Intent intent = getIntent();
        //Could store these locally within the class
        //mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        //mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);


        Intent gattServiceIntent = new Intent(mParentActivity, BluetoothLeService.class);
        mParentActivity.bindService(gattServiceIntent, mServiceConnection, android.content.Context.BIND_AUTO_CREATE);

        mParentActivity.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        Log.d(TAG,"Started service and registered receiver");
        //The service will be start and in OnStart() it will connect to the first device in our list
        //how do I check if the service has started at this point (is there an async callback somewhere to enable)
        //ans = we actually only call getsupportedgatt services when we get an intent from the service,
        //which would imply that 1. service has started 2. gatt has been connected
        //so we kind of need to wait here


    }

private int sex;

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            String uuid = null;
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                Log.d(TAG,"GATT is now connected");

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                gattServices=mBluetoothLeService.getSupportedGattServices();
                //Now we need to loop through the services
                for (BluetoothGattService gattService : gattServices){
                    uuid=gattService.getUuid().toString();
                    Log.d(TAG,"UUID of the service is "+uuid);
                    List<BluetoothGattCharacteristic> gattCharacteristics=gattService.getCharacteristics();
                    for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                        uuid=gattCharacteristic.getUuid().toString();
                        Log.d(TAG,"Charc UUID is "+uuid);
                        //Now check if this chara is HR measurement and if so enable notification for it
                        if (uuid.contains("2a37")){ //this is the actual heart rate
                            int charc_prop=gattCharacteristic.getProperties();
                            if ((charc_prop|BluetoothGattCharacteristic.PROPERTY_NOTIFY)>0)
                                mBluetoothLeService.setCharacteristicNotification(gattCharacteristic,true);
                        }
                        if (uuid.contains("2a24")){ //this is the device name
                            //Cannot read actual characteristics here - need to 
                            //final byte[] data = gattCharacteristic.getValue();
                            //save this characteristic
                            mDeviceNameCharacteristic=gattCharacteristic;

                            //Log.d(TAG,"Device name is "+data.toString());

                        }
                    }
                }
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                if (intent.getStringExtra(BluetoothLeService.CHARACTERISTIC_TYPE)=="HR"){
                    Log.d(TAG, "HR rate is"+intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                    heartRate=Integer.parseInt(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                    if (heartRate<30) heartRate=30;
                    if (heartRate>215) heartRate=215; //just a bit of sanity checking
                }
//add if the return type is the device name, display and store appropriately


            }
        }
    };
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }




}
