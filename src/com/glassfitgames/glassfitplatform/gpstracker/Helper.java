package com.glassfitgames.glassfitplatform.gpstracker;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.glassfitgames.glassfitplatform.auth.AuthenticationActivity;
import com.glassfitgames.glassfitplatform.models.Action;
import com.glassfitgames.glassfitplatform.models.Authentication;
import com.glassfitgames.glassfitplatform.models.Challenge;
import com.glassfitgames.glassfitplatform.models.Device;
import com.glassfitgames.glassfitplatform.models.EntityCollection;
import com.glassfitgames.glassfitplatform.models.Event;
import com.glassfitgames.glassfitplatform.models.Friend;
import com.glassfitgames.glassfitplatform.models.Game;
import com.glassfitgames.glassfitplatform.models.GameBlob;
import com.glassfitgames.glassfitplatform.models.Notification;
import com.glassfitgames.glassfitplatform.models.Sequence;
import com.glassfitgames.glassfitplatform.models.Track;
import com.glassfitgames.glassfitplatform.models.User;
import com.glassfitgames.glassfitplatform.models.UserDetail;
import com.glassfitgames.glassfitplatform.sensors.Quaternion;
import com.glassfitgames.glassfitplatform.sensors.SensorService;
import com.glassfitgames.glassfitplatform.utils.FileUtils;
import com.roscopeco.ormdroid.ORMDroidApplication;
import com.unity3d.player.UnityPlayer;

/**
 * Helper exposes the public methods we'd expect the games to use. The basic
 * features include registering a listener for GPS locations, plus starting and
 * stopping logging of positions to the SQLite database.
 * 
 */
public class Helper {
    private static final boolean BETA = false;
    private static final boolean INVESTORS = true;
    private static final int[] INTERNAL_UIDS = new int[]{39,41,42};

    
    public final int sessionId;
    
    private Context context;
    private static Helper helper;
    private GPSTracker gpsTracker;
    private SensorService sensorService;
    private List<TargetTracker> targetTrackers;
    private static Thread fetch = null;
    private Process recProcess = null;;
    
    private Integer pluggedIn = null;
    
    private Helper(Context c) {
        super();
        targetTrackers = new ArrayList<TargetTracker>();   
        
        context = c;
        c.bindService(new Intent(context, SensorService.class), sensorServiceConnection,
                        Context.BIND_AUTO_CREATE);
        
        BroadcastReceiver receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                if (plugged == BatteryManager.BATTERY_PLUGGED_AC) {
                    // on AC power
                    pluggedIn = BatteryManager.BATTERY_PLUGGED_AC;
                    Log.w("HelperDebug", "Plugged into AC");
                } else if (plugged == BatteryManager.BATTERY_PLUGGED_USB) {
                    // on USB power
                    pluggedIn = BatteryManager.BATTERY_PLUGGED_USB;
                    Log.w("HelperDebug", "Plugged into USB");
                } else if (plugged == 0) {
                    // on battery power
                    pluggedIn = 0;
                    Log.w("HelperDebug", "On battery power");
                } else {
                    // intent didnt include extra info
                }
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        c.registerReceiver(receiver, filter);        
        
        ORMDroidApplication.initialize(context);
        // Make sure we have a device_id for guid generation (Unity may need to verify and show an error message)
        getDevice();
        // Generate a session id
        sessionId = Sequence.getNext("session_id");
    } 
    
    public synchronized static Helper getInstance(Context c) {
        if (helper == null) {
            helper = new Helper(c);
            logEvent("{\"helper\":\"created\"}");
        }
        return helper;
    }
    
    /**
     * Is app running on Google Glass?
     * 
     * @return yes/no
     */
    public static boolean onGlass() {
        return Build.MODEL.contains("Glass");
    }
    
    /**
     * Is device plugged into a charger?
     * 
     * @return yes/no
     */
    public boolean isPluggedIn() {
        if (pluggedIn == null) {
            Log.w("HelperDebug", "Do not know battery state");
            return false;
        }
        if (pluggedIn == BatteryManager.BATTERY_PLUGGED_AC || pluggedIn == BatteryManager.BATTERY_PLUGGED_USB) return true;
        else return false;
    }

    /**
     * Is device connected to the internet?
     * 
     * @return yes/no
     */
    public boolean hasInternet() {
        NetworkInfo info = (NetworkInfo) ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();        
        if (info != null && info.isConnected()) return true;
        else return false;
    }

    /**
     * Is device connected to Wifi?
     * 
     * @return yes/no
     */
    public boolean hasWifi() {
        ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifi = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifi != null && wifi.isConnected()) return true;
        else return false;
    }
    
    /**
     * Use this method from Unity to get a new instance of GPSTracker. 
     * <p>
     * TODO: This method should return a *singleton* instance of GPSTracker, as having more than one
     * makes no sense.
     * 
     * @param c current application context
     * @return new instance of GPSTracker
     */
    public synchronized GPSTracker getGPSTracker() {
        if (gpsTracker == null) {
            gpsTracker = new GPSTracker(context);
        }
        return gpsTracker;
    }
	
    public TargetTracker getFauxTargetTracker(float speed) {
        TargetTracker t = new FauxTargetTracker(speed);
        targetTrackers.add(t);
        return t;
	}
    
    public TargetTracker getTrackTargetTracker(int device_id, int track_id) {
        Track track = Track.get(device_id, track_id);
        if (track == null) return null;
        TargetTracker t = new TrackTargetTracker(track);
        targetTrackers.add(t);
        return t;
    }
    
    public List<TargetTracker> getTargetTrackers() {
        return targetTrackers;
    }
    
    public void resetTargets() {
    	targetTrackers.clear();
    }
    
    public List<Track> getTracks() {
        return Track.getTracks();
    }
    
    public List<Track> getTracks(double maxDistance, double minDistance) {
    	return Track.getTracks(maxDistance, minDistance);
    }
    
    public List<Game> getGames() {
        Log.d("platform.gpstracker.Helper","Getting Games...");
        List<Game> allGames = Game.getGames(context);
        Log.d("platform.gpstracker.Helper","Returning " + allGames.size() + " games to Unity.");
        return allGames;
    }
        
    public void loadDefaultGames() {
    	Log.i("platform.gpstracker.helper", "Loading games again from CSV");
    	try {
			Game.loadDefaultGames(context);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e("platform.gpstracker.helper", "Error loading games from CSV");
			e.printStackTrace();
		}
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
	 * Get device details. Register device with server if not registered.
	 * Messages Unity OnRegistration "Success" or "Failure" if no device in local db.
	 * 
	 * @return device or null if registering device
	 */
	private static Thread deviceRegistration = null;
        public static Device getDevice() {
            Device self = Device.self();
            if (self != null) return self;
            if (deviceRegistration != null && deviceRegistration.isAlive()) return null;
            
            // Register device and message unity when/if we have one
            deviceRegistration = new Thread(new Runnable() {
                @Override
                public void run() {
                    Device self;
                    try {
                        self = SyncHelper.registerDevice();
                        self.self = true;
                        self.save();
                        message("OnRegistration", "Success");
                    } catch (IOException e) {
                        message("OnRegistration", "Network error");
                    }
                }
            });
            deviceRegistration.start();
            return null;
        }
	
	/**
	 * Explicitly login with a username and password
	 * 
	 * @param username
	 * @param password
	 */
	public static void login(String username, String password) {
            Log.i("platform.gpstracker.Helper", "login(" + username + ") called");
            AuthenticationActivity.login(username, password);	    
	}
	
        /**
         * Authenticate the user to our API and authorize the API with provider permissions.
         * Messages Unity OnAuthentication "Success", "Failure" or "OutOfBand" if authorization 
         * needs to be done on the website or companion app.
         * 
         * @param activity
         * @param provider
         * @param permission(s)
         * @return boolean legacy
         */
        public boolean authorize(Activity activity, String provider, String permissions) {
                Log.i("platform.gpstracker.Helper", "authorize() called");
                UserDetail ud = UserDetail.get();
                // We do not need to authenticate if we have an API token 
                // and the correct permissions from provider
                if (ud.getApiAccessToken() != null && hasPermissions(provider, permissions)) {
                        message("OnAuthentication", "Success");
                        return false;
                }
                
                if (onGlass() || true) {
                    // On glass
                    
                    if ("any".equals(provider) || "raceyourself".equals(provider) || ud.getApiAccessToken() == null) {
                        AccountManager mAccountManager = AccountManager.get(context);
                        Account[] accounts = mAccountManager.getAccountsByType("com.google");
                        String email = null;
                        for (Account account : accounts) {
                            if (account.name != null && account.name.contains("@")) {
                                email = account.name;
                                break;
                            }
                        }
                        // Potential fault: Can there be multiple accounts? Do we need to sort or provide a selector?
                       
                        // TODO: Use static account token instead of hard-coded password.
                        login(email, "testing123");
                        return false;
                    } else {
                        try {
                            AuthenticationActivity.updateAuthentications(ud);
                        } catch (ClientProtocolException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (hasPermissions(provider, permissions)) {
                            message("OnAuthentication", "Success");
                            return false;
                        }
                        // TODO:
                        //  A) Pop up a message telling the user to link an account through the web interface/companion app
                        //  B) Use social SDK to fetch third-party access token and pass it to our server
                        message("OnAuthentication", "OutOfBand");
                        return false;
                    }
                    
                } else { 
                    // Off glass
                    
                    Intent intent = new Intent(activity.getApplicationContext(), AuthenticationActivity.class);
                    intent.putExtra("provider", provider);
                    intent.putExtra("permissions", permissions);
                    activity.startActivity(intent);
                    return false;

                }
        }
	
	/**
	 * Check provider permissions of current user.
	 * 
	 * @param provider
	 * @param permissions
	 * @return boolean 
	 */
	public static boolean hasPermissions(String provider, String permissions) {
	        UserDetail ud = UserDetail.get();
	        if (("any".equals(provider) || "raceyourself".equals(provider)) && ud != null && ud.getApiAccessToken() != null ) {
	            return true;
	        }
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
        if (INVESTORS) {
            // Investor demo: Internal accounts are friends
            List<Friend> friends = new ArrayList<Friend>();
            UserDetail ud = UserDetail.get();
            for (int uid : INTERNAL_UIDS) {
                if (ud != null && uid == ud.getGuid()) continue;
                // Pre-cached in syncToServer
                User user = User.get(uid);
                if (user == null) continue;
                // Synthesise friend
                String name = user.getName();
                if (name == null || name.length() == 0) name = user.getUsername();
                if (name == null || name.length() == 0) name = user.getEmail();
                if (name == null || name.length() == 0) name = "unknown";
                Friend friend = new Friend();
                friend.friend = String.format("{\"_id\" : \"user%d\","
                                + "\"user_id\" : %d,"
                                + "\"has_glass\" : true,"
                                + "\"email\" : \"%s\","
                                + "\"name\" : \"%s\","
                                + "\"username\" : \"%s\","
                                + "\"photo\" : \"\","
                                + "\"provider\" : \"raceyourself\"}", user.getGuid(), user.getGuid(), user.getEmail(), name, user.getUsername());
                friends.add(friend);
            }
            return friends;
        }
		if (BETA) {
            List<Friend> friends = new ArrayList<Friend>();
            // NOTE: Beta only! All users are friends. Users cache fetched in syncToServer
            EntityCollection cache = EntityCollection.get("users");
            List<User> users = cache.getItems(User.class);
            
            for (User user : users) {
                // Synthesise friend
                String name = user.getName();
                if (name == null || name.length() == 0) name = user.getUsername();
                if (name == null || name.length() == 0) name = user.getEmail();
                if (name == null || name.length() == 0) name = "unknown";
                Friend friend = new Friend();
                friend.friend = String.format("{\"_id\" : \"user%d\","
                                + "\"user_id\" : %d,"
                                + "\"has_glass\" : true,"
                                + "\"email\" : \"%s\","
                                + "\"name\" : \"%s\","
                                + "\"username\" : \"%s\","
                                + "\"photo\" : \"\","
                                + "\"provider\" : \"raceyourself\"}", user.getGuid(), user.getGuid(), user.getEmail(), name, user.getUsername());
                friends.add(friend);
            }
            return friends;
		}
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
         * NOTE: May return stale data if offline.
         * 
         * @return challenges
         */
        public static List<Challenge> fetchPublicChallenges() {
            Log.i("platform.gpstracker.Helper", "fetchPublicChallenges() called");
            return SyncHelper.getCollection("challenges", Challenge.class);
        }

        /**
         * Fetch a challenge from the server
         * NOTE: May return stale data if offline.
         * 
         * @return challenge
         */
        public static Challenge fetchChallenge(String id) {
            Log.i("platform.gpstracker.Helper", "fetchChallenge(" + id + ") called");
            Challenge challenge = Challenge.get(id);
            if (challenge != null && EntityCollection.getCollections(challenge).contains("default")) return challenge;
            return SyncHelper.get("challenges/" + id, Challenge.class);
        }
        
        public static User fetchUser(int id) {
            Log.i("platform.gpstracker.Helper", "fetchChallenge(" + id + ") called");
            User user = User.get(id);
            if (user == null || !EntityCollection.getCollections(user).contains("default"))
            	user = SyncHelper.get("users/" + id, User.class);
              
            if (user.name == null || user.name.length() == 0) user.name = user.getUsername();
            if (user.name == null || user.name.length() == 0) user.name = user.getEmail();
            if (user.name == null || user.name.length() == 0) user.name = "unknown";
            return user;
        }

        /**
         * Fetch a specific track from the server
         * NOTE: May return stale data if offline.
         * 
         * @param deviceId
         * @param trackId
         * @return track
         */
        public static Track fetchTrack(int deviceId, int trackId) {
            Log.i("platform.gpstracker.Helper", "fetchTrack(" + deviceId + "," + trackId + ") called");
            Track track = Track.get(deviceId, trackId);
            if (track != null && EntityCollection.getCollections(track).contains("default")) return track;
            return SyncHelper.get("tracks/" + deviceId + "-" + trackId, Track.class);
        }
        
        /**
         * Fetch a specific user's tracks from the server.
         * NOTE: May return stale data if offline.
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
         * Log an analytics event.
         * 
         * The request is queued until the next server sync.
         * 
         * @param event serialized as json
         */
        public static void logEvent(String json) {
                Log.i("platform.gpstracker.Helper", "logEvent() called");
                Event event = new Event(json);
                event.save();
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
	public synchronized static void syncToServer(Context context) {
		Log.i("platform.gpstracker.Helper", "syncToServer() called");
        if (INVESTORS) {
            for (int uid : INTERNAL_UIDS) {
                // Pre-cache for getFriends() investor functionality
                User user = fetchUser(uid);
            }
        }
        if (BETA) {
            // Populate users cache async; for getFriends() beta functionality
            if (fetch == null || !fetch.isAlive()) {
                fetch = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("platform.gpstracker.Helper", "Getting user collection");
                        SyncHelper.getCollection("users", User.class);
                        Log.i("platform.gpstracker.Helper", "User collection obtained");
                    }
                });
                fetch.start();
            }
        }
        SyncHelper.getInstance(context).start();
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
//        if (product.matches("glass.*")) {  // glass_1 is the original explorer edition, has a good magnetometer
//            return sensorService.getGyroDroidQuaternion().flipX().flipY().multiply(sensorService.getScreenRotation());
//        } else if (product.matches("(manta.*|crespo.*)")) {  // N10|S4|NS are best without magnetometer, jflte*=s4, mako=n4
//            return sensorService.getGlassfitQuaternion().flipX().flipY().multiply(sensorService.getScreenRotation());
//        } else {  // assume all sensors work and return the most accurate orientation
//            return sensorService.getGyroDroidQuaternion().flipX().flipY().multiply(sensorService.getScreenRotation());
//        }
        // always return native android orientation, as this is what works best on glass:
        return sensorService.getGyroDroidQuaternion().flipX().flipY().multiply(sensorService.getScreenRotation());
        
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
    	    
	
    public static void message(String handler, String text) {
        try {
            UnityPlayer.UnitySendMessage("Platform", handler, text);
        } catch (UnsatisfiedLinkError e) {
            Log.i("GlassFitPlatform","Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
            Log.i("GlassFitPlatform",e.getMessage());
        }            
        
    }
    
    public void screenrecord(Activity activity) {
    	final String PATH = new File(Environment.getExternalStorageDirectory(), "raceyourself_video.mp4").toString();
    	if (recProcess == null) {
	    	try {
				recProcess = Runtime.getRuntime().exec("su");
				DataOutputStream outputStream = new DataOutputStream(recProcess.getOutputStream());
				outputStream.writeBytes("screenrecord " + PATH + "\n");
				outputStream.flush();
			} catch (IOException e) {
	            Log.i("GlassFitPlatform","Failed to start adb shell screenrecord");
	            Log.i("GlassFitPlatform",e.getMessage());
			} 
    	} else {
    		recProcess.destroy();
    		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PATH));
    		intent.setDataAndType(Uri.parse(PATH), "video/mp4");
    		activity.startActivity(intent);    	
    		recProcess = null;
    	}
    }
    
    public void exportDatabaseToCsv() {
        ORMDroidApplication.initialize(context);
        File positionFile;
        File trackFile;
        File userFile;
        File associationFile;
        File ecFile;
        
        try {
            positionFile = FileUtils.createSdCardFile(context, "AllPositions.csv");
            trackFile = FileUtils.createSdCardFile(context, "AllTracks.csv");
            userFile = FileUtils.createSdCardFile(context, "AllUsers.csv");
            associationFile = FileUtils.createSdCardFile(context, "AllAssociations.csv");
            ecFile = FileUtils.createSdCardFile(context, "AllEntityCollections.csv");
            //(new Position()).allToCsv(positionFile);
            (new Track()).allToCsv(trackFile);
            (new User()).allToCsv(userFile);
            (new EntityCollection.Association()).allToCsv(associationFile);
            (new EntityCollection()).allToCsv(ecFile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
