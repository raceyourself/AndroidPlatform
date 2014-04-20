package com.glassfitgames.glassfitplatform.BLE;

import android.bluetooth.BluetoothGattCharacteristic;

public interface BluetoothLeListener {

    public void characteristicDetected(BluetoothGattCharacteristic characteristic);
    
    public void onNewHeartrateData(int heartRateBpm);
    
    public void onNewCadenceData(float cadenceRpm);
    
    public void onNewWheelSpeedData(float wheelSpeedRpm);
    
}
