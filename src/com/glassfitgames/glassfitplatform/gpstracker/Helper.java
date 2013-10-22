package com.glassfitgames.glassfitplatform.gpstracker;

import java.util.List;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.glassfitgames.glassfitplatform.auth.AuthenticationActivity;
import com.glassfitgames.glassfitplatform.models.Action;
import com.glassfitgames.glassfitplatform.models.Friend;
import com.glassfitgames.glassfitplatform.models.GameBlob;
import com.glassfitgames.glassfitplatform.sensors.SensorService; 
import com.glassfitgames.glassfitplatform.models.Identity;
import com.glassfitgames.glassfitplatform.models.Notification;
import com.glassfitgames.glassfitplatform.models.Position;
import com.glassfitgames.glassfitplatform.models.Track;
import com.glassfitgames.glassfitplatform.models.UserDetail;
import com.unity3d.player.UnityPlayerActivity;

/**
 * Helper exposes the public methods we'd expect the games to use. The basic
 * features include registering a listener for GPS locations, plus starting and
 * stopping logging of positions to the SQLite database.
 * 
 */
public class Helper {
    
    private Context context;
    private static Helper helper;
    private GPSTracker gpsTracker;
    private TargetTracker targetTracker;
    private SensorService sensorService;
    private List<Track> trackList;
    private List<Position> numPositions;
    private Track currentTrack;
    private int currentID;
    
    private Helper(Context c) {
        super();
        context = c;
        c.bindService(new Intent(context, SensorService.class), sensorServiceConnection,
                        Context.BIND_AUTO_CREATE);
    }
    
    public static Helper getInstance(Context c) {
        if (helper == null) {
            helper = new Helper(c);
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
    public GPSTracker getGPSTracker() {
        if (gpsTracker == null) {
            gpsTracker = new GPSTracker(context);
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
	 * Get user details.
	 * 
	 * @return user details
	 */
	public static UserDetail getUser() {
		return UserDetail.get();
	}
	
	/**
	 * Authenticate the user to our API and authorize the API with provider permissions.
	 * 
	 * @param activity
	 * @param provider
	 * @param permission(s)
	 * @return boolean Already authenticated
	 */
	public static boolean authorize(Activity activity, String provider, String permissions) {
		Log.i("platform.gpstracker.Helper", "authorize() called");
		Identity identity = Identity.getIdentityByProvider(provider);
		UserDetail ud = UserDetail.get();
		// We do not need to authenticate if we have an API token 
		// and the correct permissions to the identity provider
		if (ud.getApiAccessToken() != null 
				&& identity != null && identity.hasPermissions(permissions)) {
			return true;
		}
        Intent intent = new Intent(activity.getApplicationContext(), AuthenticationActivity.class);
        intent.putExtra("provider", provider);
        intent.putExtra("permissions", permissions);
        activity.startActivity(intent);
        return false;
	}
	
	/**
	 * Check provider permissions of current user.
	 * 
	 * @param provider
	 * @param permissions
	 * @return boolean 
	 */
	public static boolean hasPermissions(String provider, String permissions) {
		Identity identity = Identity.getIdentityByProvider(provider);
		if (identity != null && identity.hasPermissions(permissions)) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Get the user's friends.
	 * 
	 * @return friends
	 */
	public static Friend[] getFriends() {
		Log.i("platform.gpstracker.Helper", "getFriends() called");
		return (Friend[])Friend.getFriends().toArray();
	}
	
	/**
	 * Queue a server-side action.
	 * 
	 * The request is queued until the next server sync.
	 * 
	 * @param action serialized as json
	 */
	public static void queueAction(String json) {
		Log.i("platform.gpstracker.Helper", "queueAction() called");
		Action action = new Action(json);
		action.save();
	}
	
	/**
	 * Get notifications.
	 * 
	 * @return notifications
	 */
	public static Notification[] getNotifications() {
		Log.i("platform.gpstracker.Helper", "getNotifications() called");
		return (Notification[])Notification.getNotifications().toArray();
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
     * Get a rotation vector (quaternion) describing the rotation required to get from the device's
     * current orientation to the orientation it was in when helper was first created.
     * 
     * @return float[4] quaternion
     */
	public float[] getGameRotationVector() {
	    if (sensorService != null) {
	        float[] quat = sensorService.getGameRotationVector();
	        quat[0] *= -1.0f;
	        quat[1] *= -1.0f;
	        quat[2] *= -1.0f;
	        return quat;
	    } else {
	        Log.d("Helper","Can't return GameRotationVector because SensorService is not bound yet.");
	        try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
	        float[] rot = {0.0f, 0.0f, 0.0f, 0.0f};
	        return rot;
	    }
	}
	
    public float[] getGameYpr() {
        if (sensorService != null) {
            return sensorService.getGameYpr();
        } else {
            Log.d("Helper","Can't return GameRotationVector because SensorService is not bound yet.");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            float[] rot = {1.0f, 0.0f, 0.0f};
            return rot;
        }
    }	
	
	private ServiceConnection sensorServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            sensorService = ((SensorService.SensorServiceBinder)binder).getService();
            Log.d("Helper", "Helper has bound to SensorService");
        }

        public void onServiceDisconnected(ComponentName className) {
            sensorService = null;
            Log.d("Helper", "Helper has unbound from SensorService");
        }
    };
	
	
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
