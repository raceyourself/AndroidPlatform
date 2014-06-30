
package com.raceyourself.platform.utils;

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

    public static final String WS_URL = "https://api.raceyourself.com/";
//    public static final String WS_URL = "http://a.staging.raceyourself.com/";
    public static final String API_URL = WS_URL + "api/1/";

    // post url for position table
    public static final String POSITION_SYNC_URL = API_URL + "sync/"; 

    // Web service oauth credentials
    // Production:
    public static final String CLIENT_ID = "98f985cd4fca00aefda3f31c10b3d994eaa496d882fdf3db936fad76e4dae236";
    public static final String CLIENT_SECRET = "9ca4b4f56b518deca0c2200b92a3435f05cb4e0b3d52b0a5a1608f39d004750e";
    // Staging:
//    public static final String CLIENT_ID = "293a42e2e4e2caeff758bbe512059365002d2c6524c1f17c57154f332941e9cd";
//    public static final String CLIENT_SECRET = "fb47367332bac7633b9cb6f12b411327a1a8df257852c61feac75ff9d0311c53";

    
    public static final String GCM_REG_ID = "gcm_reg_id";
}
