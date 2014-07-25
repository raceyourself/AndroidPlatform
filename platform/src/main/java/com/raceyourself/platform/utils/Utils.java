
package com.raceyourself.platform.utils;

import com.raceyourself.platform.BuildConfig;

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

    public static final String API_URL = BuildConfig.WS_URL + "api/1/";

    // post url for position table
    public static final String POSITION_SYNC_URL = API_URL + "sync/";

    public static final String GCM_REG_ID = "gcm_reg_id";
    public static final String GCM_SENDER_ID = "892619514273";

    public static final String ACRA_REPORT_URL = "http://a.staging.raceyourself.com/acra/report";
}
