package com.glassfitgames.glassfitplatform.gpstracker;

import android.content.Context;
import android.util.Log;

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
	 * @param tag - the previous track to use
	 * @return a target tracker linked to the specified previous track
	 * @throws Exception if tag is not recognised
	 */
	public static TargetTracker getTargetTracker(String tag) throws Exception {
	    if (tag == "pb") {
	        return new TargetTracker(0);
	    } else {
	        throw new Exception("Tag " + tag + "not supported yet.");
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
