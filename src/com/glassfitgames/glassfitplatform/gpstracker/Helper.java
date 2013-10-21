package com.glassfitgames.glassfitplatform.gpstracker;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.glassfitgames.glassfitplatform.auth.AuthenticationActivity;
import com.glassfitgames.glassfitplatform.models.GameBlob;
import com.glassfitgames.glassfitplatform.models.Position;
import com.glassfitgames.glassfitplatform.models.Track;
import com.unity3d.player.UnityPlayerActivity;

/**
 * Helper exposes the public methods we'd expect the games to use. The basic
 * features include registering a listener for GPS locations, plus starting and
 * stopping logging of positions to the SQLite database.
 * 
 */
public class Helper extends UnityPlayerActivity {
    
    private static Helper helper;
    private GPSTracker gpsTracker;
    private TargetTracker targetTracker;
    private List<Track> trackList;
    private List<Position> numPositions;
    private Track currentTrack;
    private int currentID;
    
    private Helper() {
        super();
    }
    
    public static Helper getInstance() {
        if (helper == null) {
            helper = new Helper();
        }
        return helper;
    }
    
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
    public GPSTracker getGPSTracker(Context c) {
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
	public TargetTracker getTargetTracker() {
        if (targetTracker == null) {
            targetTracker = new TargetTracker();
        }
        return targetTracker;
	}

	/**
	 * Authenticate the user to our API
	 * 
	 * @param context
	 */
	public static void authenticate(Activity activity) {
		Log.i("platform.gpstracker.Helper", "authenticate() called");
        Intent intent = new Intent(activity.getApplicationContext(), AuthenticationActivity.class);
        activity.startActivity(intent);		
	}
	
	/**
	 * syncToServer syncs the local database with the server.
	 * 
	 */
	public static void syncToServer(Context context) {
		Log.i("platform.gpstracker.Helper", "syncToServer() called");
		long currentSyncTime = System.currentTimeMillis();
		new SyncHelper(context, currentSyncTime).start();
	}
	
	/**
	 * Load game blob from database.
	 * Attempts to load from assets if not in database.
	 * 
	 * @param id Blob identifier
	 * @return binary blob data
	 */
	public static byte[] loadBlob(String id) {
		GameBlob gb = GameBlob.loadBlob(id);
		if (gb == null) return GameBlob.loadDefaultBlob(id);
		return gb.getBlob();
	}

	/**
	 * Store or update game blob in database.
	 * 
	 * @param id Blob identifier
	 * @param blob Binary blob data
	 */
	public static void storeBlob(String id, byte[] blob) {
		GameBlob gb = GameBlob.loadBlob(id);
		if (gb == null) gb = new GameBlob(id);
		gb.setBlob(blob);
		gb.save();
	}
	
	/**
	 * Erase blob from database.
	 * 
	 * @param id Blob identifier
	 */
	public static void eraseBlob(String id) {
		GameBlob.eraseBlob(id);
	}

	/**
	 * Erase all blobs in the database.
	 */
	public static void resetBlobs() {
		List<GameBlob> dblobs = GameBlob.getDatabaseBlobs();
		for (GameBlob blob : dblobs) {
			GameBlob.eraseBlob(blob.getId());
		}
	}
	
    /**
     * Tell the platform classes that the device is currently pointing forwards. Used to set the
     * gyro offset. Particularly useful when we're not moving so don't have a GPS bearing.
     */
	public void resetGyros() {
	    if (gpsTracker != null) {
	        gpsTracker.resetGyros();
	    }
	}
	
	
	/**
	 * Get a list of all the tracks for the user
	 */
	public void getTracks() {
		trackList = Track.getTracks();
		Log.i("Track", "Getting Tracks");
		currentTrack = trackList.get(0);
		currentID = 0;
		boolean trackOK = false;
		
		if(currentTrack.getTrackPositions().size() > 0){
			trackOK = true;
		} 
		while(!trackOK) {
			if(currentID+1 < trackList.size()) {
				currentID++;
				currentTrack = trackList.get(currentID);
				if(currentTrack.getTrackPositions().size() > 0) {
					numPositions = currentTrack.getTrackPositions();
					Log.i("Track", "Track with positions found!");
					trackOK = true;
				}
			} else {
				Log.i("Track", "No Valid Tracks!!");
				break;
			}
		}
		
		numPositions = currentTrack.getTrackPositions();
	}
	
	public void deleteIndoorTracks() {
		
	}
	
	/**
	 * Get the next track for the user
	 */
	public void getNextTrack() {
		boolean trackOK = false;
		int startID = currentID;
		Log.i("Track", "Getting Next Track");
		while(!trackOK) {
			if(currentID+1 < trackList.size()) {
				currentID++;
				currentTrack = trackList.get(currentID);
				if(currentTrack.getTrackPositions().size() > 0) {
					numPositions = currentTrack.getTrackPositions();
					Log.i("Track", "Track with positions found!");
					trackOK = true;
				}
			} else {
				currentID = 0;
				currentTrack = trackList.get(currentID);
				if(currentTrack.getTrackPositions().size() > 0) {
					numPositions = currentTrack.getTrackPositions();
					Log.i("Track", "Track with positions found!");
					trackOK = true;
				}
			}
			
			if(startID == currentID) {
				Log.i("Track", "No Valid Tracks!!");
				break;
			}
		}
	}
	
	/**
	 * Get the previous track for the user
	 */
	public void getPreviousTrack() {
		boolean trackOK = false;
		Log.i("Track", "Getting Previous Track");
		int startID = currentID;
		while(!trackOK) {
			if(currentID-1 > 0) {
				currentID--;
				currentTrack = trackList.get(currentID);
				if(currentTrack.getTrackPositions().size() > 0) {
					numPositions = currentTrack.getTrackPositions();
					Log.i("Track", "Track with positions found!");
					trackOK = true;
				}
			} else {
				currentID = trackList.size() - 1;
				currentTrack = trackList.get(currentID);
				if(currentTrack.getTrackPositions().size() > 0) {
					numPositions = currentTrack.getTrackPositions();
					Log.i("Track", "Track with positions found!");
					trackOK = true;
				}
			}
			
			if(startID == currentID) {
				Log.i("Track", "No Valid Tracks!!");
				break;
			}
		}
	}
	
	/**
	 * sets the current track for the user
	 */
	public void setTrack() {
		targetTracker.setTrack(currentTrack);
	}
	
	/**
	 * Retrieves the number of positions
	 */
	public int getNumberPositions() {
		if(!numPositions.isEmpty())
			return numPositions.size();
		else
			return 0;
	}
	
	/**
	 * Retrieves the position from the list
	 */
	public Position getPosition(int i) {
		return numPositions.get(i);
	}
}
