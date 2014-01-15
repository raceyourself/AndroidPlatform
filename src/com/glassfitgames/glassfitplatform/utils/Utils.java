
package com.glassfitgames.glassfitplatform.utils;

public class Utils {

    public static final int PLATFORM_VERSION = 1;

    // shared preference name to store the last synced time
    public static final String SYNC_PREFERENCES = "sync_preferences"; 

    // shared preference variable name for gps data
    public static final String SYNC_GPS_DATA = "last_synced_time"; 

    public static final String WS_URL = "http://auth.raceyourself.com/";
    public static final String API_URL = WS_URL + "api/1/";

    // post url for position table
    public static final String POSITION_SYNC_URL = API_URL + "sync/"; 

    // Web service oauth credentials
    public static final String CLIENT_ID = "8c8f56a8f119a2074be04c247c3d35ebed42ab0dcc653eb4387cff97722bb968";
    public static final String CLIENT_SECRET = "892977fbc0d31799dfc52e2d59b3cba88b18a8e0080da79a025e1a06f56aa8b2";

}
