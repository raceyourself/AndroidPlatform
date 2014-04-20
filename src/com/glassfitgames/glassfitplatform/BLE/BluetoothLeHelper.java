package com.glassfitgames.glassfitplatform.BLE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.glassfitgames.glassfitplatform.utils.RaceYourselfLog;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;

public class BluetoothLeHelper {
    
    private RaceYourselfLog log = new RaceYourselfLog(this.getClass().getSimpleName());
    private final BluetoothAdapter mBluetoothAdapter;
    private Context mContext;
    private Set<BluetoothLeListener> mListeners = new HashSet<BluetoothLeListener>();
    private DeviceFoundCallback mDeviceFoundCallback;
    private Map<BluetoothGatt, BluetoothGattCharacteristic> openGatt = new HashMap<BluetoothGatt, BluetoothGattCharacteristic>(); // connections/characteristics that we've asked for notifications from
    
    private static final long SCAN_PERIOD = 10000;  // scan for 10,000ms then stop

    // Constructor
    public BluetoothLeHelper(Context c) {  //, BluetoothLeListener receiver
        
        //if (c == null || receiver == null) throw new IllegalArgumentException("Must pass a non-null Context and BluetoothLeRceiver");
        mContext = c;
        //mReceiver = receiver;
        
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        
        // Determine whether BLE is supported on the device.
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) || mBluetoothAdapter == null) {
            throw new UnsupportedOperationException("Bluetooth Low Energy not supported on this device.");
        }
    }

    // Trigger scanning for BLE devices
    // For each device detected, we look at it's characteristics and fire receiver's callbacks if there's something of interest
    public void startListening() {
        
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                // TODO: callback to request user to enable BT 
                //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                //mContext.startActivityForResult(enableBtIntent, 1);
                log.warn("Bluetooth not enabled, giving up!");
                return;
            }
        }
        
        // Create a new callback that'll filter for devices we're interested in
        mDeviceFoundCallback = new DeviceFoundCallback();
        
        // Start scanning
        log.info("Scenning for BLE devices...");
        mBluetoothAdapter.startLeScan(mDeviceFoundCallback);
        
        // Stops scanning after a pre-defined scan period.
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.stopLeScan(mDeviceFoundCallback);
                if (openGatt.isEmpty()) {
                    log.info("Scan finished, no useful BLE devices found");
                } else {
                    log.info("Scan finished, registered " + openGatt.keySet().size() + " devices");
                }
            }
        }, SCAN_PERIOD);
        
    }
    
    public void stopListening() {
        log.info("Disconnecting from all BLE devices");
        for (BluetoothGatt c : openGatt.keySet()) {
            // TODO: should store & close all characteristics for each gatt rather than just the latest
            c.setCharacteristicNotification(openGatt.get(c), false);  // stop notifications
            c.close();
            c.disconnect();
        }
    }

    public void registerListener(BluetoothLeListener listener) {
        mListeners.add(listener);
        log.info("Registered BLE listener: " + listener.toString());
    }
    
    public void unregisterListener(BluetoothLeListener listener) {
        if (mListeners.contains(listener)) {
            mListeners.remove(listener);
            log.info("Un-registered BLE listener: " + listener.toString());
        } else {
            log.info("BLE listener: " + listener.toString() + "wasn't registered, won't un-register");
        }
    }
    
    // Device scan callback - handles all devices
    private class DeviceFoundCallback implements BluetoothAdapter.LeScanCallback {
        
        // Interface from BluetoothAdapter.LeScanCallback - called when a device is discovered
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            log.info("Found BLE device: " + device.getName());
            // connect to the device & listen for data
            CallbackMonitor callbackMonitor = new CallbackMonitor(device);
            device.connectGatt(mContext, false, callbackMonitor);
        }
    }
    
    // Device connected / disconnected / new data callbacks
    // MUST create a separate instance for each BLE device!
    private class CallbackMonitor extends BluetoothGattCallback {
        
        private BluetoothGatt mBluetoothGatt;
        private BluetoothDevice mDevice;
        
        protected CallbackMonitor(BluetoothDevice device) {
            mDevice = device;
        }

        // Called by mDevice when it connects / disconnects to the remote BLE device
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                log.info("Connected to BLE device " + mDevice.getName() + ", starting service discovery: " + mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                log.info("Disconnected from BLE device " + mDevice.getName());
                gatt.close();
            }
        }

        // Called by mDevice when remote BLE device reports some supported services
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            
            if (status != BluetoothGatt.GATT_SUCCESS) {
                log.warn("BLE device " + mDevice.getName() + " failed whilst reporting services: " + status);
                return;
            }
            
            // TODO: loop through the services, if one if of interest save this callback monitor and do something useful with it!
            // If not useful, gatt.getDevice().disconnectGatt() should get rid of it.
            boolean keepDeviceConnected = false;
            List<BluetoothGattService> services = gatt.getServices();
            for (BluetoothGattService s : services) {
                String serviceType = SampleGattAttributes.lookup(s.getUuid().toString(), "unknown");
                
                // pull out cycling speed/cadence services
                if (serviceType == "org.bluetooth.service.cycling_speed_and_cadence") {
                    
                    // this device has a cycling speed and cadence service
                    log.info("BLE device " + mDevice.getName() + " has a cycling speed and cadence service");
                    
                    for (BluetoothGattCharacteristic c : s.getCharacteristics()) {
                        String characteristicType = SampleGattAttributes.lookup(c.getUuid().toString(), "unknown");
                        
                        // read the csc_feature characteristic to check if it supports speed, cadence or both
                        if (characteristicType == "org.bluetooth.characteristic.csc_feature") {
                            this.mBluetoothGatt.readCharacteristic(c);
                        
                        // request notifications when the speed/cadence data is updated
                        } else if (characteristicType == "org.bluetooth.characteristic.csc_measurement") {
                            this.mBluetoothGatt.setCharacteristicNotification(c, true);
                            openGatt.put(mBluetoothGatt, c); // save reference to gatt/characteristic so we can disconnect later
                        }
                    }
                    
                // pull out heart-rate services
                } else if (serviceType == "org.bluetooth.service.heart_rate") {
                    
                    // this device has a heart-rate service
                    log.info("BLE device " + mDevice.getName() + " has a heartrate service");
                    
                    for (BluetoothGattCharacteristic c : s.getCharacteristics()) {
                        String characteristicType = SampleGattAttributes.lookup(c.getUuid().toString(), "unknown");
                        
                        // request notifications when the heart-rate data is updated
                        if (characteristicType == "org.bluetooth.characteristic.heart_rate_measurement") {
                            this.mBluetoothGatt.setCharacteristicNotification(c, true);
                            openGatt.put(mBluetoothGatt, c); // save reference to gatt/characteristic so we can disconnect later
                        }
                    }
                    
                // add other services we become interested in here
                } else {
                    // nothing - not interested in this service yet
                }
            } // for : services
            
            // for devices we are not interested in, close/disconnect the connection
            if (!openGatt.containsKey(gatt)) {
                gatt.close();
                gatt.disconnect();
            }
        }

        // Called by mDevice when remote BLE device returns some data in response to a "read" request
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic c, int status) {
            String characteristicType = SampleGattAttributes.lookup(c.getUuid().toString(), "unknown");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                log.info("BLE read: " + mDevice.getName() + "." + characteristicType + " has properties: " + c.getProperties());
                // TODO: send this data to listeners
            } else {
                log.warn("BLE read: " + mDevice.getName() + " returned error " + status + " when reporting " + characteristicType);
            }
        }

        // Called by mDevice when remote BLE device has been asked to notify us when there is new data for a given characteristic
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic c) {
            
            String characteristicType = SampleGattAttributes.lookup(c.getUuid().toString(), "unknown");
            log.debug("BLE notify: " + mDevice.getName() + "." + characteristicType + " has data: " + byteArrayToHexString(c.getValue()));
            
            // extract cycle speed/cadence data
            if (characteristicType == "org.bluetooth.characteristic.csc_measurement") {
                // look for wheel speed data
                if ((c.getProperties() & 0x01) == 0x01) {
                    // have speed data - print to log
                    int revs = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 1);
                    float timeSinceLastTrigger = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 5) / 1024.0f;
                    log.debug(String.format("Received wheel revs: %d and time since last trigger %ds", revs, timeSinceLastTrigger));
                    // TODO: send this data to listeners
                }
                if ((c.getProperties() & 0x10) == 0x10) {
                    // have cadence data - print to log
                    int revs = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 7);
                    float timeSinceLastTrigger = c.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 9) / 1024.0f;
                    log.debug(String.format("Received crank revs: %d and time since last trigger %ds", revs, timeSinceLastTrigger));
                    // TODO: send this data to listeners
                }
                
            // extract heart-rate data
            } else if (characteristicType == "org.bluetooth.characteristic.heart_rate_measurement") {
                int flag = c.getProperties();
                int format = ((flag & 0x01) != 0) ? BluetoothGattCharacteristic.FORMAT_UINT16 : BluetoothGattCharacteristic.FORMAT_UINT8;
                int heartRate = c.getIntValue(format, 1);
                log.debug(String.format("Received heart rate: %d", heartRate));
                // TODO: send this data to listeners
            }
        }
        
    };  // CallbackMonitor
    
    private String byteArrayToHexString(byte[] data) {
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for(byte byteChar : data) {
                stringBuilder.append(String.format("%02X ", byteChar));
            }
            return stringBuilder.toString();
        } else {
            return "";
        }
    }
    
}
