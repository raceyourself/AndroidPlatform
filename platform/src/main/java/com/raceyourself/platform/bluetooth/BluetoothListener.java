package com.raceyourself.platform.bluetooth;

import android.bluetooth.BluetoothDevice;

/**
 * Created by benlister on 14/07/2014.
 */
public interface BluetoothListener {

    public void onConnected(BluetoothDevice device);
    public void onDisconnected(BluetoothDevice device);
    public void onMessageReceived(String message);

}
