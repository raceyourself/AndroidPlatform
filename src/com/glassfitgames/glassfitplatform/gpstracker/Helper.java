package com.glassfitgames.glassfitplatform.gpstracker;

import android.content.Context;
import android.util.Log;

import com.glassfitgames.glassfitplatform.gpstracker.TargetTracker.TargetSpeed;
import com.glassfitgames.glassfitplatform.models.Position;
import com.unity3d.player.UnityPlayerActivity;

/**
 * Helper exposes the public methods we'd expect the games to use. The basic
 * features include registering a listener for GPS locations, plus starting and
 * stopping logging of positions to the SQLite database.
 * 
 */
public class Helper extends UnityPlayerActivity {

    /**
     * 
     * @param c current context
     * @return singleton instance of GPSTracker
     */
	public static GPSTracker getGPSTracker(Context c) {
	    return new GPSTracker(c);
	}
	
	/**
	 * 
	 * @param tag - the previous track to use, or name of a TargetSpeed enum value
	 * @return a target tracker linked to the specified previous track or target speed
	 * @throws Exception if tag is not recognised
	 */
	public static TargetTracker getTargetTracker(String tag) throws Exception {
	    if (tag.equals("pb")) {
	        Log.i("GPSHelper","Target tracker set to personal best");
	        return new TargetTracker(TargetSpeed.USAIN_BOLT);
	    } else if (TargetSpeed.valueOf(TargetSpeed.class, tag) != null) {
	        Log.i("GPSHelper","Target tracker set to " + TargetSpeed.valueOf(TargetSpeed.class, tag).speed() + "m/s.");
	        return new TargetTracker(TargetSpeed.valueOf(TargetSpeed.class, tag));
	    } else {
	        throw new IllegalArgumentException("Tag " + tag + "not supported yet.");
	    }
	}

	/**
	 * syncToServer syncs the local database with the server.
	 * 
	 */
	public static void syncToServer(Context context) {
		Log.i("platform.gpstracker.Helper", "syncToServer() called");
		long currentSyncTime = System.currentTimeMillis();
		new GpsSyncHelper(context, currentSyncTime).start();
	}

}
