
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

    public static final String WS_URL = "http://api.raceyourself.com/";
//    public static final String WS_URL = "http://a.staging.raceyourself.com/";
    public static final String API_URL = WS_URL + "api/1/";

    // post url for position table
    public static final String POSITION_SYNC_URL = API_URL + "sync/"; 

    // Web service oauth credentials
    // Production:
    public static final String CLIENT_ID = "0fd21388876007ecb3017a5d6cc3309898d4938866b535b55522a6611900d0f8";
    public static final String CLIENT_SECRET = "5d4f35194775716548d829b1ae2d0c4957f408d37b0f9ad2453e818c0064db5b";
    // Staging:
//    public static final String CLIENT_ID = "293a42e2e4e2caeff758bbe512059365002d2c6524c1f17c57154f332941e9cd";
//    public static final String CLIENT_SECRET = "fb47367332bac7633b9cb6f12b411327a1a8df257852c61feac75ff9d0311c53";

    
    public static final String GCM_REG_ID = "gcm_reg_id";
}
