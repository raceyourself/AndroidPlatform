package com.glassfitgames.glassfitplatform.gpstracker;

import java.util.ArrayList;
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
import com.glassfitgames.glassfitplatform.models.Authentication;
import com.glassfitgames.glassfitplatform.models.Challenge;
import com.glassfitgames.glassfitplatform.models.Device;
import com.glassfitgames.glassfitplatform.models.Friend;
import com.glassfitgames.glassfitplatform.models.GameBlob;
import com.glassfitgames.glassfitplatform.models.Notification;
import com.glassfitgames.glassfitplatform.models.Position;
import com.glassfitgames.glassfitplatform.models.Track;
import com.glassfitgames.glassfitplatform.models.UserDetail;
import com.glassfitgames.glassfitplatform.sensors.Quaternion;
import com.glassfitgames.glassfitplatform.sensors.SensorService;
import com.roscopeco.ormdroid.ORMDroidApplication;
import com.unity3d.player.UnityPlayer;

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
    private List<TargetTracker> targetTrackers;
    private static SyncHelper sync;
    
    private Helper(Context c) {
        super();
        targetTrackers = new ArrayList<TargetTracker>();   
        
        context = c;
        c.bindService(new Intent(context, SensorService.class), sensorServiceConnection,
                        Context.BIND_AUTO_CREATE);
        
        ORMDroidApplication.initialize(context);
        // Make sure we have a device_id for guid generation
        Device self = Device.self();
        if (self == null) {
            // TODO: Force authentication and sync so that we can guarantee device_id uniqueness.
        	self = new Device();
        	self.id = (int)(System.currentTimeMillis()%Integer.MAX_VALUE);
        	Log.i("HelperDebug", "Generated id: " + self.id);
        	self.self = true;
        	self.save();
        }
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
    	Log.i("Helper", "Getting target tracker");
        TargetTracker t = new TargetTracker();
        Log.i("Helper", "Target obtained, adding to list");
        targetTrackers.add(t);
        Log.i("Helper", "There are " + targetTrackers.size() + " trackers in platform");
        return t;
	}
    
    public void resetTargets() {
    	targetTrackers = new ArrayList<TargetTracker>();
    	//Log.i("Helper", "There are " + targetTrackers.size() + " trackers in platform");
    }

	/**
	 * Get user details.
	 * 
	 * @return user details
	 */
	public static UserDetail getUser() {
		return UserDetail.get();
	} 
	
	public void setTrack(int trackID) {
		if(targetTrackers.size() > 0) {
			targetTrackers.get(0).setTrack(trackID);
		}
	}

	public String getTrackID() {
		return currentTrack.getId();
	}
	
        /**
         * Authenticate the user to our API and authorize the API with provider permissions.
         * 
         * @param activity
         * @param provider
         * @param permission(s)
         * @return boolean legacy
         */
        public static boolean authorize(Activity activity, String provider, String permissions) {
                Log.i("platform.gpstracker.Helper", "authorize() called");
                Authentication identity = Authentication.getAuthenticationByProvider(provider);
                UserDetail ud = UserDetail.get();
                // We do not need to authenticate if we have an API token 
                // and the correct permissions from provider
                if (ud.getApiAccessToken() != null 
                                && identity != null && identity.hasPermissions(permissions)) {
                        message("OnAuthentication", "Success");
                        return false;
                }
                // We do not need to authenticate if we have an API token and any provider is ok
                if (ud.getApiAccessToken() != null && "any".equals(provider)) {
                    message("OnAuthentication", "Success");
                    return false;
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
		Authentication identity = Authentication.getAuthenticationByProvider(provider);
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
	public static List<Friend> getFriends() {
		Log.i("platform.gpstracker.Helper", "getFriends() called");
		return Friend.getFriends();
	}
	
        /**
         * Get the user's personal/synced challenges
         * 
         * @return challenges
         */
        public static List<Challenge> getPersonalChallenges() {
            Log.i("platform.gpstracker.Helper", "getPersonalChallenges() called");
            return Challenge.getPersonalChallenges();
        }
        
        /**
         * Fetch all public challenges from the server
         * 
         * @return challenges
         */
        public static List<Challenge> fetchPublicChallenges() {
            Log.i("platform.gpstracker.Helper", "fetchPublicChallenges() called");
            return SyncHelper.getCollection("challenges", Challenge.class);
        }

        /**
         * Fetch a specific track from the server
         * 
         * @param deviceId
         * @param trackId
         * @return track
         */
        public static Track fetchTrack(int deviceId, int trackId) {
            Log.i("platform.gpstracker.Helper", "fetchTrack(" + deviceId + "," + trackId + ") called");
            return SyncHelper.get("tracks/" + deviceId + "-" + trackId, Track.class);
        }
        
        /**
         * Fetch a specific user's tracks from the server
         * 
         * @param userId
         * @return tracks
         */
        public static List<Track> fetchUserTracks(int userId) {
            Log.i("platform.gpstracker.Helper", "fetchUserTracks(" + userId + ") called");
            return SyncHelper.getCollection("users/" + userId + "/tracks", Track.class);
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
	public static List<Notification> getNotifications() {
		Log.i("platform.gpstracker.Helper", "getNotifications() called");
		return Notification.getNotifications();
	}
	
	/**
	 * syncToServer syncs the local database with the server.
	 * 
	 */
	public static void syncToServer(Context context) {
		Log.i("platform.gpstracker.Helper", "syncToServer() called");
		if (sync != null && sync.isAlive()) {
	            Log.i("platform.gpstracker.Helper", "syncHelper is already running");
		    return;
		}
		sync = new SyncHelper(context);
		sync.start();
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
	    if (sensorService != null) {
	        sensorService.resetGyros();
	    }
	}
	
    /**
     * Returns a quaternion describing the rotation required to get from real-wold co-ordinates to
     * the device's current orientation.
     * 
     * @return Quaternion
     */
    public Quaternion getOrientation() {
        // no rotation if sensorService not bound
        if (sensorService == null) {
            return Quaternion.identity();
        }
        // switch on device type
        // in each case we flip x,y axes to convert to Unity's LH co-ordinate system
        // and rotate to match device's screen orientation 
        String product = android.os.Build.PRODUCT;
        if (product.matches("glass.*")) {  // glass_1 is the original explorer edition, has a good magnetometer
            return sensorService.getGyroDroidQuaternion().flipX().flipY().multiply(sensorService.getScreenRotation());
        } else if (product.matches("(manta.*|crespo.*)")) {  // N10|S4|NS are best without magnetometer, jflte*=s4, mako=n4
            return sensorService.getGlassfitQuaternion().flipX().flipY().multiply(sensorService.getScreenRotation());
        } else {  // assume all sensors work and return the most accurate orientation
            return sensorService.getGyroDroidQuaternion().flipX().flipY().multiply(sensorService.getScreenRotation());
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
	
	
//	/**
//	 * Get a list of all the tracks for the user
//	 */
//	public void getTracks() {
//		trackList = Track.getTracks();
//		Log.i("Track", "Getting Tracks");
//		currentTrack = trackList.get(0);
//		currentID = 0;
//		boolean trackOK = false;
//		
//		if(currentTrack.getTrackPositions().size() > 0){
//			trackOK = true;
//		} 
//		while(!trackOK) {
//			if(currentID+1 < trackList.size()) {
//				currentID++;
//				currentTrack = trackList.get(currentID);
//				if(currentTrack.getTrackPositions().size() > 0) {
//					numPositions = currentTrack.getTrackPositions();
//					Log.i("Track", "Track with positions found!");
//					trackOK = true;
//				}
//			} else {
//				Log.i("Track", "No Valid Tracks!!");
//				break;
//			}
//		}
//		
//		numPositions = currentTrack.getTrackPositions();
//	}
//	
//	public void deleteIndoorTracks() {
//		
//	}
//	
//	/**
//	 * Get the next track for the user
//	 */
//	public void getNextTrack() {
//		boolean trackOK = false;
//		int startID = currentID;
//		Log.i("Track", "Getting Next Track");
//		while(!trackOK) {
//			if(currentID+1 < trackList.size()) {
//				currentID++;
//				currentTrack = trackList.get(currentID);
//				if(currentTrack.getTrackPositions().size() > 0) {
//					numPositions = currentTrack.getTrackPositions();
//					Log.i("Track", "Track with positions found!");
//					trackOK = true;
//				}
//			} else {
//				currentID = 0;
//				currentTrack = trackList.get(currentID);
//				if(currentTrack.getTrackPositions().size() > 0) {
//					numPositions = currentTrack.getTrackPositions();
//					Log.i("Track", "Track with positions found!");
//					trackOK = true;
//				}
//			}
//			
//			if(startID == currentID) {
//				Log.i("Track", "No Valid Tracks!!");
//				break;
//			}
//		}
//	}
//	
//	/**
//	 * Get the previous track for the user
//	 */
//	public void getPreviousTrack() {
//		boolean trackOK = false;
//		Log.i("Track", "Getting Previous Track");
//		int startID = currentID;
//		while(!trackOK) {
//			if(currentID-1 > 0) {
//				currentID--;
//				currentTrack = trackList.get(currentID);
//				if(currentTrack.getTrackPositions().size() > 0) {
//					numPositions = currentTrack.getTrackPositions();
//					Log.i("Track", "Track with positions found!");
//					trackOK = true;
//				}
//			} else {
//				currentID = trackList.size() - 1;
//				currentTrack = trackList.get(currentID);
//				if(currentTrack.getTrackPositions().size() > 0) {
//					numPositions = currentTrack.getTrackPositions();
//					Log.i("Track", "Track with positions found!");
//					trackOK = true;
//				}
//			}
//			
//			if(startID == currentID) {
//				Log.i("Track", "No Valid Tracks!!");
//				break;
//			}
//		}
//	}
//	
//	/**
//	 * sets the current track for the user
//	 */
//	public void setTrack() {
//		targetTracker.setTrack(currentTrack);
//	}
    
    public int getNumberTracks() {
    	trackList = Track.getTracks();
    	return trackList.size();
    }
	
    public Track getTrack(int i) {
    	return trackList.get(i);
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
	
        private static void message(String handler, String text) {
            try {
                UnityPlayer.UnitySendMessage("Platform", handler, text);
            } catch (UnsatisfiedLinkError e) {
                Log.i("GlassFitPlatform","Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
                Log.i("GlassFitPlatform",e.getMessage());
            }            
        
    }
}
