package com.glassfitgames.glassfitplatform.BLE;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGattCharacteristic;

@TargetApi(18)
public interface BluetoothLeListener {

    public void characteristicDetected(BluetoothGattCharacteristic characteristic);
    
    public void onNewHeartrateData(int heartRateBpm);
    
    public void onNewCadenceData(float cadenceRpm);
    
    public void onNewWheelSpeedData(float wheelSpeedRpm);
    
}
