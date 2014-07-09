
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
//    public static final String CLIENT_ID = "c9842247411621e35dbaf21ad0e15c263364778bf9a46b5e93f64ff2b6e0e17c";
//    public static final String CLIENT_SECRET = "75f3e999c01942219bea1e9c0a1f76fd24c3d55df6b1c351106cc686f7fcd819";

    public static final String GCM_REG_ID = "gcm_reg_id";
    public static final String GCM_SENDER_ID = "892619514273";

}
