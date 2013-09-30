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
    
    private static GPSTracker gpsTracker;
    private static TargetTracker targetTracker;

    /**
     * Use this method from Unity to get a new instance of GPSTracker. Only required because we
     * believe Unity can only interact with UnityPlayerActivity classes.
     * <p>
     * TODO: This method should return a *singleton* instance of GPSTracker, as having more than one
     * makes no sense.
     * 
     * @param c current application context
     * @return new instance of GPSTracker
     */
    public static GPSTracker getGPSTracker(Context c) {
        if (gpsTracker == null) {
            gpsTracker = new GPSTracker(c);
        }
        return gpsTracker;
    }
	
    /**
     * Use this method from Unity to get a new instance of TargetTracker. Only required because we
     * believe Unity can only interact with UnityPlayerActivity classes.
     * 
     * @return an empty TargetTracker with a default (constant) speed
     */
	public static TargetTracker getTargetTracker() {
        if (targetTracker == null) {
            targetTracker = new TargetTracker();
        }
        return targetTracker;
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
