package com.glassfitgames.glassfitplatform.BLE;

/**
 * Created by Dmitry on 25/09/13.
 */
public interface GlassFitBLEDevice_SPEEDO {
    //Communicates updated speed and cadence in units set by SetSpeedUnit and SetCadenceUnits
    //To access cumulative wheel and crank revolutions call respective class methods
    public void onNewWheelEvent(float current_speed);
    public void onNewCrankEvent(float current_cadence);
    public void onDisconnect();
}
