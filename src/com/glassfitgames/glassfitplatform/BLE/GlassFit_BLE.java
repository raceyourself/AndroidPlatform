package com.glassfitgames.glassfitplatform.BLE;

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
public abstract class GlassFit_BLE {
    public final static String TAG = "BigD";
    protected ArrayList<BluetoothDevice> mLeDevices;
    protected BluetoothAdapter mBluetoothAdapter;
   // protected BluetoothLeService mBluetoothLeService; //we declare an instance of the BLE service
    private boolean mScanning;
    private Handler mHandler;//only used during a time-limited thread for scanning for devices
    private Context mContext=null;
    protected Activity mParentActivity=null;
    protected List<BluetoothGattService> gattServices;
    protected boolean mConnected;
    public String deviceName ="Not yet known";
    protected GlassFit_BLE_Callbacks mBLE_Callback_parent;


    protected BluetoothGattCharacteristic mDeviceNameCharacteristic=null;

    public static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;







//Constructor
    public GlassFit_BLE(Context c){
        //Will use this for starting service and asking for permissions etc
        //mParentActivity=a;
        mContext=c;
        mHandler = new Handler();
        mLeDevices = new ArrayList<BluetoothDevice>();
      //  mBLE_Callback_parent=(GlassFit_BLE_Callbacks)a;
    }

    public GlassFit_BLE(){

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

        Log.d(TAG,"Have got Bluetooth Smart!");
        return true;
    }

    // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
    // fire an intent to display a dialog asking the user to grant permission to enable it.
    public boolean  EnableBLE() {
        if (mBluetoothAdapter==null)
            return false;
//        if (!mBluetoothAdapter.isEnabled()) {
//
//                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                mParentActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//        return false;
//        }

    return true;
    }
  
    
    public boolean ScanForBLEDevices(boolean s){
        //need to device a mechanism of informing the main app about the result
        if (!mBluetoothAdapter.isEnabled())
            return false;
        scanLeDevice(s);

        return true;
    }



    protected void scanLeDevice(final boolean enable) {
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


    //Identifying whether its a HR or a speedo varies so we implement it in subclasses
    abstract void IdentifyNewDevice(BluetoothDevice device);

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    //Calls the abstract method above, which subclasses implement
                    IdentifyNewDevice(device);
                }
            };






    public String getDeviceName(){
        return deviceName;
    }

    public boolean isPhysicalDeviceDetected(){
        if (mLeDevices.isEmpty())
            return false;
            else return true;
    }

}
