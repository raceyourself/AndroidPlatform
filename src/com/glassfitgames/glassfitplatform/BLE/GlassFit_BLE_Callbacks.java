package com.glassfitgames.glassfitplatform.BLE;

/**
 * Created by Dmitry on 29/09/13.
 */
public interface GlassFit_BLE_Callbacks {
    public void onHRUpdate();
    public void onSPEEDOUpdate();
    public void onDeviceFound(String name);
    public void onDeviceConnected(int event_id);
    public void onDeviceDisconnected(String name);



}
