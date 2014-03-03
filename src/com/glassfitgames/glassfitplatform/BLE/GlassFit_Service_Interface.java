package com.glassfitgames.glassfitplatform.BLE;

/**
 * Created by Dmitry on 25/09/13.
 */
public interface GlassFit_Service_Interface {
    public final static String TAG = "BigD";
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    //Used to add extra data to the intents that are sent to the main app to convey bluetooth events
    //and pass characteristic values and updates
    //The base string - Update to match the package name
    public static String ACTION_STRING_BASE="com.glassfit.BLE";


    public final static String ACTION_GATT_CONNECTED =
            ACTION_STRING_BASE+"ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            ACTION_STRING_BASE+"ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            ACTION_STRING_BASE+"ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            ACTION_STRING_BASE+"ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            ACTION_STRING_BASE+"EXTRA_DATA";
    //this string will be used to identify the characteristic data as its being
    //passed from the service to the main app. Below are the values which this string can take

    public final static String CHARACTERISTIC_TYPE =
            ACTION_STRING_BASE+"CHARACTERISTIC_TYPE";


    //The following strings are broadly split into 3 categories (for now):
    //Its important to note that each of these uniquely corresponds to a UUID base number,
    //as specified on:
    //https://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicsHome.aspx

    //TODO Replace individual strings with a hashmap?
    //(is it worth creating a hashmap similar to one in "samplegattattributes", except only
    //match the base number not the entire string?)
    //These strings are used to give UUIDs a human-readable description which will clarify the use of
    //the data in code

    // 1. Device independent
    //This characteristic will be used to identify whether it's our own device or not
    public final static String DATA_TYPE_DEVICE_NAME="DEVICE_NAME";

    //2. Heart rate sensor specific
    //This is the actual heart rate
    public final static String DATA_TYPE_HEART_RATE="HEART_RATE";

    //3. Speedometer specific
    public final static String DATA_TYPE_CSC_EVENT ="LAST_WHEEL_EVENT_TIME";
    public final static String DATA_TYPE_CUMULATIVE_WHEEL_REVOLUTIONS="CUMULATIVE_WHEEL_REVOLUTIONS";
    public final static String DATA_TYPE_LAST_CRANK_EVENT_TIME="LAST_CRANK_EVENT_TIME";
    public final static String DATA_TYPE_CUMULATIVE_CRANK_REVOLUTIONS="CUMULATIVE_CRANK_REVOLUTIONS";
    public final static String DATA_TYPE_LAST_WHEEL_EVENT_TIME="LAST_WHEEL_EVENT_TIME";


}
