package com.glassfitgames.glassfitplatform.BLE;

import com.glassfitgames.glassfitplatform.utils.UnityInterface;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

public class BluetoothLeUnityPlugin implements BluetoothLeListener {
    
    private BluetoothLeHelper helper;

    public BluetoothLeUnityPlugin(Context c) {
        helper = new BluetoothLeHelper(c);
    }
    
    // Called from Unity to start listening
    // Unity messages will be sent on new devices / services / characteristic data
    public void startListening() {
        helper.registerListener(this);
        helper.startListening();
    }
    
    // Called from Unity to stop listening
    // Unity messages will caese to be sent
    public void stopListening() {
        helper.stopListening();
        helper.unregisterListener(this);
    }

    @Override
    public void characteristicDetected(BluetoothGattCharacteristic characteristic) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onNewHeartrateData(int heartRateBpm) {
        UnityInterface.unitySendMessage("PlatformPartner", "OnBleHeartrateData", Integer.toString(heartRateBpm));
    }

    @Override
    public void onNewCadenceData(float cadenceRpm) {
        UnityInterface.unitySendMessage("PlatformPartner", "OnBleCadenceData", Float.toString(cadenceRpm));
    }

    @Override
    public void onNewWheelSpeedData(float wheelSpeedRpm) {
        UnityInterface.unitySendMessage("PlatformPartner", "OnBleWheelSpeedData", Float.toString(wheelSpeedRpm));
    }
}
