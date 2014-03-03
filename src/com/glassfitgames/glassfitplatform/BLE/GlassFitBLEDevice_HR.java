package com.glassfitgames.glassfitplatform.BLE;

/**
 * Created by Dmitry on 25/09/13.
 */
public interface GlassFitBLEDevice_HR {
    public void onHeartRateChange(int new_heart_rate);
    public void onDisconnect();
}
