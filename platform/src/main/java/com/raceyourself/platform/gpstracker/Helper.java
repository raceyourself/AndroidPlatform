package com.raceyourself.platform.gpstracker;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Criteria;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.raceyourself.platform.auth.AuthenticationActivity;
import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.Action;
import com.raceyourself.platform.models.Device;
import com.raceyourself.platform.models.EntityCollection;
import com.raceyourself.platform.models.Event;
import com.raceyourself.platform.models.Friend;
import com.raceyourself.platform.models.Game;
import com.raceyourself.platform.models.GameBlob;
import com.raceyourself.platform.models.Notification;
import com.raceyourself.platform.models.Position;
import com.raceyourself.platform.models.Sequence;
import com.raceyourself.platform.sensors.Quaternion;
import com.raceyourself.platform.sensors.SensorService;
import com.raceyourself.platform.utils.FileUtils;
import com.raceyourself.platform.models.Authentication;
import com.raceyourself.platform.models.Challenge;
import com.raceyourself.platform.models.EnhancedPosition;
import com.raceyourself.platform.models.Track;
import com.raceyourself.platform.models.User;
import com.raceyourself.platform.networking.SocketClient;
import com.raceyourself.platform.utils.MessagingInterface;
import com.roscopeco.ormdroid.ORMDroidApplication;

/**
 * Helper exposes the public methods we'd expect the games to use. The basic
 * features include registering a listener for GPS locations, plus starting and
 * stopping logging of positions to the SQLite database.
 * 
 */
public class Helper {
    public final int sessionId;
    private static final boolean onGlass = Build.MODEL.contains("Glass");
    private static boolean remoteDisplay = onGlass; // Glass is by defaulta remote display
    
    private Context context;
    private static Helper helper;
    private GPSTracker gpsTracker;
    private SensorService sensorService;
    private List<TargetTracker> targetTrackers;
    private static Thread fetch = null;
    private Process recProcess = null;;
    
    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> bluetoothPairedDevices;
    private Integer pluggedIn = null;
    
    private SocketClient socketClient = null;
    
    private Helper(Context c) {
        super();
        targetTrackers = new ArrayList<TargetTracker>();   
        
        context = c;
        c.bindService(new Intent(context, SensorService.class), sensorServiceConnection,
                        Context.BIND_AUTO_CREATE);
        
        
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothPairedDevices = bluetoothAdapter.getBondedDevices();
        
        BroadcastReceiver receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                
                // listen for plugged-in / unplugged intents
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
                
                // listen for bluetooth pair/unpair intents
                String action = intent.getAction();
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(action.equals("android.bluetooth.device.action.ACL_CONNECTED")) {
                    // refresh set of paired devices
                    bluetoothPairedDevices = bluetoothAdapter.getBondedDevices();
                } else if (action.equals("android.bluetooth.device.action.ACL_DISCONNECTED")) {  
                    // refresh set of paired devices
                    bluetoothPairedDevices = bluetoothAdapter.getBondedDevices();
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
        return onGlass;
    }

    /**
     * Internal method for setting remote display state
     * 
     * @param on/off
     */
    public static void setRemoteDisplay(boolean display) {
        remoteDisplay = true;
    }
    
    /**
     * Internal method for setting remote display state
     * 
     * @param on/off
     */
    public static boolean isRemoteDisplay() {
        return remoteDisplay;
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
     * Are GPS location services enabled?
     * 
     */
    public boolean hasGps() {
        LocationManager locationManager = (LocationManager)context.getSystemService(Service.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        String provider = locationManager.getBestProvider(criteria, true);
        if (locationManager.isProviderEnabled(provider)) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Are GPS location services enabled?
     * 
     */
    public List<String> getGpsProviderNames() {
        LocationManager locationManager = (LocationManager)context.getSystemService(Service.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        List<String> providers = locationManager.getProviders(criteria, true);
        return providers;
    }
    
    /**
     * Are we paired to any bluetooth devices?
     * 
     */
    
    public boolean isBluetoothBonded() {
        return bluetoothAdapter.getState() == BluetoothDevice.BOND_BONDED;
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
	public static AccessToken getUser() {
		return AccessToken.get();
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
                AccessToken ud = AccessToken.get();
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
	        AccessToken ud = AccessToken.get();
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
        public static List<Challenge> fetchPublicChallenges() throws SyncHelper.UnauthorizedException, SyncHelper.CouldNotFetchException {
            Log.i("platform.gpstracker.Helper", "fetchPublicChallenges() called");
            return SyncHelper.getCollection("challenges", Challenge.class);
        }

        /**
         * Fetch a challenge from the server
         * NOTE: May return stale data if offline.
         * 
         * @return challenge
         */
        public static Challenge fetchChallenge(int deviceId, int challengeId) throws SyncHelper.UnauthorizedException, SyncHelper.CouldNotFetchException {
            Log.i("platform.gpstracker.Helper", "fetchChallenge(" + deviceId + "," + challengeId + ") called");
            Challenge challenge = Challenge.get(deviceId, challengeId);
            if (challenge != null && EntityCollection.getCollections(challenge).contains("default")) return challenge;
            return SyncHelper.get("challenges/" + deviceId + "-" + challengeId, Challenge.class);
        }
        
        public static User fetchUser(int id) throws SyncHelper.UnauthorizedException, SyncHelper.CouldNotFetchException {
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
        public static Track fetchTrack(int deviceId, int trackId) throws SyncHelper.UnauthorizedException, SyncHelper.CouldNotFetchException {
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
        public static List<Track> fetchUserTracks(int userId) throws SyncHelper.UnauthorizedException, SyncHelper.CouldNotFetchException {
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
        SyncHelper syncHelper = SyncHelper.getInstance(context);
        syncHelper.init();
        syncHelper.requestSync();
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
	
    public float getAzimuth() {
        // no azimuth if sensorService not bound
        if (sensorService == null) {
            return 0.0f;
        }
        // return azimuth ( in a clockwise direction ) to be consistent with GPS bearing
        return sensorService.getAzimuth();
    }
    
    // Called by Unity to notify about completing auto-correction for bearing
    public void notifyAutoBearing() {
    	if (gpsTracker != null) {
    		gpsTracker.notifyAutoBearing(getAzimuth());
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
    	    
	
    public static void message(String handler, String text) {
        MessagingInterface.sendMessage("Platform", handler, text);
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
        File enhancedPositionFile;
        File trackFile;
        File userFile;
        File associationFile;
        File ecFile;
        
        try {
            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HHmmss");
            String datestamp = sdfDate.format(new Date());           
            positionFile = FileUtils.createSdCardFile(context, "AllPositions_" + datestamp + ".csv");
            enhancedPositionFile = FileUtils.createSdCardFile(context, "EnhancedPositions_" + datestamp + ".csv");
            //trackFile = FileUtils.createSdCardFile(context, "AllTracks_" + datestamp + ".csv");
            //userFile = FileUtils.createSdCardFile(context, "AllUsers_" + datestamp + ".csv");
            //associationFile = FileUtils.createSdCardFile(context, "AllAssociations_" + datestamp + ".csv");
            //ecFile = FileUtils.createSdCardFile(context, "AllEntityCollections_" + datestamp + ".csv");
            System.out.print("ENH POSITIONS FILE: " + enhancedPositionFile.getAbsolutePath());
            (new Position()).allToCsv(positionFile);
            (new EnhancedPosition()).allToCsv(enhancedPositionFile);
            //(new Track()).allToCsv(trackFile);
            //(new User()).allToCsv(userFile);
            //(new EntityCollection.Association()).allToCsv(associationFile);
            //(new EntityCollection()).allToCsv(ecFile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Connect (or return existing connection) to Glassfit socket server.
     * 
     * @return socket client
     */
    public synchronized SocketClient getSocket() {
        if (socketClient == null || !socketClient.isRunning()) {
            Log.d("Helper", "Connecting to socket server");
            final AccessToken ud = AccessToken.get();
            if (ud == null || ud.getApiAccessToken() == null) return null;
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            socketClient = new SocketClient(ud.getApiAccessToken());
                        } catch (Throwable t) {
                            Log.e("Helper", "Could not connect to socket server", t);
                            socketClient = null;
                        }
                    }
                });
                t.start();
                try {
                    t.join();
                } catch (InterruptedException e) {
                    Log.e("Helper", "Could not join thread", e);
                }
        }
        if (socketClient == null || !socketClient.isRunning()) return null;
        return socketClient;
    }
    
    public void disconnectSocket() {
        Log.d("Helper", "Disconnecting from socket server");
        if (socketClient != null && socketClient.isRunning()) socketClient.shutdown();
    }
}
