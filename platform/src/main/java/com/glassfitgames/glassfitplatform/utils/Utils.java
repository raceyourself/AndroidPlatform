
package com.glassfitgames.glassfitplatform.utils;

public class Utils {

    public static final int PLATFORM_VERSION = 1;

    // shared preference name to store the last synced time
    public static final String SYNC_PREFERENCES = "sync_preferences"; 

    // shared preference variable name for gps data
    public static final String SYNC_GPS_DATA = "last_synced_time"; 
    
    // Tail pivot time (usually the same as the first sync time or 0 for tail fully synced)
    public static final String SYNC_TAIL_TIME = "last_synced_tail_time"; 
    // Tail skip count (records synced from tail pivot backwards)
    public static final String SYNC_TAIL_SKIP = "last_synced_tail_skip"; 

    public static final String WS_URL = "http://auth.raceyourself.com/";
//    public static final String WS_URL = "http://a.staging.raceyourself.com/";
    public static final String API_URL = WS_URL + "api/1/";

    // post url for position table
    public static final String POSITION_SYNC_URL = API_URL + "sync/"; 

    // Web service oauth credentials
    // Production:
    public static final String CLIENT_ID = "8c8f56a8f119a2074be04c247c3d35ebed42ab0dcc653eb4387cff97722bb968";
    public static final String CLIENT_SECRET = "892977fbc0d31799dfc52e2d59b3cba88b18a8e0080da79a025e1a06f56aa8b2";
    // Staging:
//    public static final String CLIENT_ID = "293a42e2e4e2caeff758bbe512059365002d2c6524c1f17c57154f332941e9cd";
//    public static final String CLIENT_SECRET = "fb47367332bac7633b9cb6f12b411327a1a8df257852c61feac75ff9d0311c53";

    
    public static final String GCM_REG_ID = "gcm_reg_id";
}
