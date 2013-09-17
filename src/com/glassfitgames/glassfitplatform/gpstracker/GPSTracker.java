
package com.glassfitgames.glassfitplatform.gpstracker;

import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import com.glassfitgames.glassfitplatform.gpstracker.TargetTracker.TargetSpeed;
import com.glassfitgames.glassfitplatform.models.Position;
import com.glassfitgames.glassfitplatform.models.Track;
import com.roscopeco.ormdroid.ORMDroidApplication;
import com.unity3d.player.UnityPlayer;

public class GPSTracker implements LocationListener {

    private final Context mContext;

    // flag for GPS status
    boolean isGPSEnabled = false;

    // flag for network status
    boolean isNetworkEnabled = false;

    // flag for GPS status
    boolean canGetLocation = false;

    // flag for whether we're actively tracking
    boolean isTracking = false;

    boolean indoorMode = false; // if true, we generate fake GPS updates

    private float indoorSpeed = TargetSpeed.WALKING.speed(); // speed for fake GPS updates

    int trackId; // ID of the current track

    Position currentPosition; // Most recent position within tolerated accuracy

    Position lastPosition; // Previous position, used to calc accumulating stats

    long elapsedDistance; // distance so far in metres

    long startTime; // time so far in milliseconds

    Float currentBearing; // can be null if we don't know, e.g. before GPS has initialised

    Float currentSpeed; // can be null if we don't know, e.g. before GPS has initialised

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 1 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000; // 1 second

    private static final float MAX_TOLERATED_POSITION_ERROR = 21; // 21 metres

    // Declaring a Location Manager
    protected LocationManager locationManager;

    private Timer timer;

    private GpsTask task;

    public GPSTracker(Context context) {
        this.mContext = context;

        ORMDroidApplication.initialize(context);
        Log.i("ORMDroid", "Initalized");

        Track track = new Track("Test");
        track.save();
        trackId = track.getId();
        Log.i("GlassFitPlatform", "New track created with ID " + trackId);

        elapsedDistance = 0;
        startTime = 0;
        currentBearing = null;

        initGps();

    }

    public Position getCurrentPosition() {
        return currentPosition;
    }

    public void initGps() {
        // try {
        locationManager = (LocationManager)mContext.getSystemService(Service.LOCATION_SERVICE);

        // getting GPS status
        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        // getting network status
        // isNetworkEnabled =
        // locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGPSEnabled /* && !isNetworkEnabled */) {
            // no network provider is enabled
            showSettingsAlert();
        } else {
            this.canGetLocation = true;
            // if (isNetworkEnabled) {
            // locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
            // MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
            // Log.d("GlassFitPlatform", "Network position enabled");
            // }

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, (LocationListener)this);
            Log.d("GlassFitPlatform", "GPS location updates requested");

        }

        // } catch (Exception e) {
        // e.printStackTrace();
        // }
    }

    public void startTracking() {
        
        Log.d("GlassFitPlatform", "startTracking() called");
        isTracking = true;

        if (indoorMode == true) {
            // start TimerTask to generate fake position data once per second
            timer = new Timer();
            task = new GpsTask();
            timer.scheduleAtFixedRate(task, 0, 1000);
        }

    }

    public void stopTracking() {
        Log.d("GlassFitPlatform", "stopTracking() called");
        isTracking = false;
        if (task != null) task.cancel();
    }
    
    public boolean isIndoorMode() {
        return indoorMode;
    }

    public void setIndoorMode(boolean indoorMode) {
        this.indoorMode = indoorMode;
    }

    /**
     * Sets the speed for indoor mode from the TargetSpeed enum
     * of reference speeds.
     * @param TargetSpeed indoorSpeed
     */
    public void setIndoorSpeed(TargetSpeed indoorSpeed) {
        this.indoorSpeed = indoorSpeed.speed();
    }
    
    /**
     * Sets the speed for indoor mode to the supplied float value,
     * measured in m/s.
     * @param float indoorSpeed
     */
    public void setIndoorSpeed(float indoorSpeed) {
        this.indoorSpeed = indoorSpeed;
    }    


    /**
     * Task to regularly generate fake position data when in indoor mode. startLogging() triggers
     * starts the task, stopLogging() ends it.
     */
    private class GpsTask extends TimerTask {
        private double drift = 0f;

        public void run() {
                // Fake location
                Location location = new Location("");
                location.setTime(System.currentTimeMillis());
                location.setLatitude(location.getLatitude() + drift);
                location.setSpeed(indoorSpeed);

                // Fake movement due north at indoorSpeed (converted to degrees)
                drift += indoorSpeed / 111229d;
                
                // Broadcast the fake location the local listener only (otherwise risk
                // confusing other apps!)
                onLocationChanged(location);
            }
    }

    /**
     * Stop using GPS listener Calling this function will stop using GPS in your app
     */
    public void stopUsingGPS() {
        if (locationManager != null) {
            locationManager.removeUpdates(GPSTracker.this);
        }
    }

    /**
     * Function to check GPS enabled
     * 
     * @return boolean
     */
    public boolean canGetPosition() {
        return currentPosition != null;
    }

    /**
     * Function to show settings alert dialog On pressing Settings button will launch Settings
     * Options
     */
    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        // Setting Dialog Title
        alertDialog.setTitle("GPS is setAbstractTrackertings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    @Override
    public void onLocationChanged(Location location) {

        // save every position update for analysis
        Position gpsPosition = new Position(trackId, location);
        
        // stop here if we're not tracking
        if (!isTracking) return;
        
        gpsPosition.save();

        // if the latest gpsPosition meets our accuracy criteria, update the
        // cumulative distance and current speed and bearing estimates
        if (gpsPosition.getEpe() < MAX_TOLERATED_POSITION_ERROR) {

            Log.d("GPSTracker", "Using position with error " + gpsPosition.getEpe());
            lastPosition = currentPosition;
            currentPosition = gpsPosition;

            if (lastPosition == null) {
                startTime = currentPosition.getTimestamp();
                return;
            }
            
            // calculate distance between lastPosition and currentPosition; add to elapsedDistance 
            float[] delta = new float[1];
            Location.distanceBetween(lastPosition.getLatx(), lastPosition.getLngx(),
                    currentPosition.getLatx(), currentPosition.getLngx(), delta);
            elapsedDistance += delta[0];
            
            // Overwrite GPS bearing with bearing between lastPosition and currentPosition
            // This seems to be much more accurate, maybe because we sample less frequently than the underlying GPS
            currentBearing = lastPosition.bearingTo(currentPosition);
            lastPosition.setBearing(currentBearing);
            lastPosition.save();
            
            // Speed returned by GPS is pretty stable: use it.
            currentSpeed = currentPosition.getSpeed();
            
            // Broadcast current position to unity3D
            JSONObject data = new JSONObject();
            try {
                data.put("speed", currentSpeed);
                data.put("bearing", currentBearing);
                data.put("elapsedDistance", elapsedDistance);
            } catch (JSONException e) {
                Log.e("GPSTracker", e.getMessage());
            }
            // Sending the message needs the unity native library installed:
            try {
                UnityPlayer.UnitySendMessage("script holder", "NewGPSPosition", data.toString());
            } catch (UnsatisfiedLinkError e) {
                Log.i("GlassFitPlatform","Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
                Log.i("GlassFitPlatform",e.getMessage());
            }

            Log.d("GPSTracker", "New elapsed distance is: " + elapsedDistance);
            Log.d("GPSTracker", "Current speed estimate is: " + currentSpeed);
            Log.d("GPSTracker", "Current bearing estimate is: " + currentBearing);
            Log.d("GPSTracker", "New elapsed time is: " + (currentPosition.getTimestamp() - startTime));
            Log.d("GPSTracker", "\n");    

        } else {
            Log.d("GPSTracker", "Throwing away position with error " + gpsPosition.getEpe());
        }

    }

    public Float getCurrentBearing() {
        return currentBearing;
    }

    public void setCurrentBearing(Float currentBearing) {
        this.currentBearing = currentBearing;
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    // @Override
    // public IBinder onBind(Intent arg0) {
    // return null;
    // }

    public float getCurrentPace() {
        return currentSpeed;
    }

    public void saveTrack() {
        // TODO Auto-generated method stub

    }

    public long getElapsedDistance() {
        return elapsedDistance;
    }

    public long getElapsedTime() {
        return currentPosition.getTimestamp() - startTime;
    }

}
