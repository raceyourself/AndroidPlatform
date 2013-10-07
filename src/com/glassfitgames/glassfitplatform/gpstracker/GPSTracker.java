
package com.glassfitgames.glassfitplatform.gpstracker;

import java.util.ArrayDeque;
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
import android.provider.Settings;
import android.util.Log;

import com.glassfitgames.glassfitplatform.gpstracker.TargetTracker.TargetSpeed;
import com.glassfitgames.glassfitplatform.models.Position;
import com.glassfitgames.glassfitplatform.models.Track;
import com.roscopeco.ormdroid.ORMDroidApplication;
import com.unity3d.player.UnityPlayer;
import org.apache.commons.math3.stat.regression.SimpleRegression;

public class GPSTracker implements LocationListener {

    private final Context mContext;
    
    // ordered list of recent positions, last = most recent
    private ArrayDeque<Position> recentPositions = new ArrayDeque<Position>(10);
    
    // last known position, just for when we're not tracking
    Position gpsPosition = null;

    // flag for whether we're actively tracking
    private boolean isTracking = false;

    private boolean indoorMode = true; // if true, we generate fake GPS updates

    private float indoorSpeed = TargetSpeed.WALKING.speed(); // speed for fake GPS updates

    private int trackId; // ID of the current track

    private double elapsedDistance = 0.0; // distance so far in metres

    private Stopwatch trackStopwatch = new Stopwatch(); // time so far in milliseconds
    private Stopwatch interpolationStopwatch = new Stopwatch(); // time so far in milliseconds

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; // 1 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000; // 1 second

    private static final float MAX_TOLERATED_POSITION_ERROR = 21; // 21 metres

    // Declaring a Location Manager
    protected LocationManager locationManager;

    private Timer timer = new Timer();

    private GpsTask task;

    /**
     * Creates a new GPSTracker object.
     * <p>
     * Initialises the database to store track logs and checks that the device has GPS enabled.
     * <p>
     * If the GPS is disabled, the devices location settings dialog will be shown so the user can
     * enable it.
     */
    public GPSTracker(Context context) {
        this.mContext = context;

        // makes sure the database exists, if not - create it
        ORMDroidApplication.initialize(context);
        Log.i("ORMDroid", "Initalized");
        
        // check if the GPS is enabled on the device
        locationManager = (LocationManager)mContext.getSystemService(Service.LOCATION_SERVICE);
        Log.v("GPSTracker", "Location manager retrieved");
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.i("GPSTracker", "GPS not enabled, trying to show location settings dialog");
            showSettingsAlert();
        }
        
        // set elapsed time/distance to zero
        reset();
        
        // trigger either real (false) or fake (true) GPS updates
        setIndoorMode(false);

    }
    
    public void reset() {
        
        Log.d("GPSTracker", "GPS tracker reset");
        
        trackStopwatch.stop();
        trackStopwatch.reset();
        interpolationStopwatch.stop();
        interpolationStopwatch.reset();
        isTracking = false;
        elapsedDistance = 0.0;
        recentPositions.clear();
        
        Track track = new Track("Test");
        Log.v("GPSTracker", "New track created");
        
        track.save();
        Log.v("GPSTracker", "New track saved");
        
        trackId = track.getId();
        Log.d("GPSTracker", "New track ID is " + trackId);
        
    }

    /**
     * Returns the position of the device as a Position object, whether or not we are tracking.
     * 
     * @return position of the device
     */
    public Position getCurrentPosition() {
        return gpsPosition;
    }
  

    /**
     * Start recording distance and time covered by the device.
     * <p>
     * Ideally this should only be called once the device has a GPS fix, which can be checked using
     * hasPosition().
     */
    public void startTracking() {
        
        Log.d("GPSTracker", "startTracking() called, hasPosition() is " + hasPosition());
        isTracking = true;
        
        // if we already have a position, start the stopwatch, if not it'll
        // be triggered when we get our first decent GPS fix
        if (hasPosition()) {
            trackStopwatch.start();
            interpolationStopwatch.start();
        }

    }

    /**
     * Stop recording distance and time covered by the device.
     * <p>
     * This will not reset the cumulative distance/time values, so it can be used to pause (e.g.
     * when the user is stopped to do some stretches). Call startTracking() to continue from where
     * we left off, or create a new GPSTracker object if a full reset is required.
     */
    public void stopTracking() {
        Log.v("GPSTracker", "stopTracking() called");
        isTracking = false;
        if (trackStopwatch != null) {
            trackStopwatch.stop();
            interpolationStopwatch.stop();
        }
    }
    
    /**
     * Is the GPSTracker in indoor mode? If so, it'll fake GPS positions. See also setIndoorMode().
     * 
     * @return true if in indoor mode, false otherwise. Default is false.
     */
    public boolean isIndoorMode() {
        return indoorMode;
    }

    /**
     * By default, GPSTracker expects to be outside. For indoor testing/demo purposes, set indoor
     * mode to true and fake GPS data will be generated as if the device was moving. See also
     * setIndoorSpeed().
     * 
     * @param indoorMode true for indoor, false for outdoor. Default is false.
     */
    public void setIndoorMode(boolean indoorMode) {
        
        // if no change, don't need to do anything
        if (this.isIndoorMode() == indoorMode) return;
        
        // if we are changing, clear the position buffer so we don't record a huge jump from a fake
        // location to a real one or vice-versa
        recentPositions.clear();
        this.indoorMode = indoorMode;
        
        if (indoorMode) {
            
            // stop listening for real GPS signals
            locationManager.removeUpdates(this); 

            // start TimerTask to generate fake position data once per second
            Log.d("GPSTracker", "Requesting fake GPS updates...");
            task = new GpsTask();
            timer.scheduleAtFixedRate(task, 0, 1000);
            Log.d("GPSTracker", "...success");
            
            Log.i("GPSTracker", "Now in indoor mode");
        
        } else {
            
            // stop generating fake GPS signals
            if (task != null) {
                task.cancel();
            }
            timer.purge();
            
            // start listening for real GPS again
            Log.d("GPSTracker", "Requesting GPS updates...");
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, (LocationListener)this);
            Log.d("GPSTracker", "...success");
            
            Log.i("GPSTracker", "Now in outdoor mode");
            
        }
    }

    /**
     * Sets the speed for indoor mode from the TargetSpeed enum
     * of reference speeds. See also isIndoorMode().
     * 
     * @param indoorSpeed enum
     */
    public void setIndoorSpeed(TargetSpeed indoorSpeed) {
        this.indoorSpeed = indoorSpeed.speed();
    }
    
    /**
     * Sets the speed for indoor mode to the supplied float value,
     * measured in m/s. See also isIndoorMode().
     * 
     * @param indoorSpeed in m/s
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
     * Function to check GPS enabled
     * 
     * @return boolean
     * @deprecated
     */
    public boolean canGetPosition() {
        return recentPositions.size() > 0;
    }
    
    /**
     * Do we have a GPS fix yet? If false, wait until it is true before expecting some of the other
     * functions in this class to work - specifically startTracking() and getCurrentPosition()
     * 
     * @return true if we have a position fix
     */
    public boolean hasPosition() {
        // if the latest position is within tolerance and fresh, return true
        if (gpsPosition != null) {
            if (isIndoorMode() && gpsPosition.getEpe() == 0) {
                // we check EPE==0 to discard any real positions left from before an indoorMode switch
                Log.v("GPSTracker", "We have a fake position ready to use");
                return true;
            }
            if (!isIndoorMode() && gpsPosition.getEpe() > 0
                            && gpsPosition.getEpe() < MAX_TOLERATED_POSITION_ERROR) {
                // we check EPE>0 to discard any fake positions left from before an indoorMode switch
                Log.v("GPSTracker", "We have a real position ready to use");
                return true;
            }
        }

        Log.v("GPSTracker", "We don't currently have a valid position");
        return false;

    }

    /**
     * Is the GPS tracker currently recording the device's movement? See also startTracking() and
     * stopTracking().
     * 
     * @return true if the device is recording elapsed distance/time and position data. False
     *         otherwise.
     */
    public boolean isTracking() {
        return isTracking;
    }

    /**
     * Function to show settings alert dialog On pressing Settings button will launch Settings
     * Options
     */
    private void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        // Setting Dialog Title
        alertDialog.setTitle("GPS is settings");

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

    /**
     * Called by the android system when new GPS data arrives.
     * <p>
     * This method does all the clever processing of the raw GPS data to provide an accurate
     * location and bearing of the device. It also increments the elapsedDistance when isTracking is
     * true and the device is moving.
     */
    @Override
    public void onLocationChanged(Location location) {

        // get the latest GPS position
        Position tempPosition = new Position(trackId, location);
        Log.i("GPSTracker", "New position with error " + tempPosition.getEpe());
        
        // if the latest gpsPosition doesn't meets our accuracy criteria, throw it away
        if (tempPosition.getEpe() > MAX_TOLERATED_POSITION_ERROR) {
            Log.d("GPSTracker", "Throwing away position as error is > " + MAX_TOLERATED_POSITION_ERROR);
            return;
        }
        
        // update current position
        Position lastPosition = gpsPosition;
        gpsPosition = tempPosition;
        
        // stop here if we're not tracking
        if (!isTracking) {
            //broadcastToUnity();
            return;
        }

        // otherwise, add to the buffer for later use
        Log.d("GPSTracker", "Using position as part of track");
        if (recentPositions.size() >= 10) {
            // if the buffer is full, discard the oldest element
            recentPositions.removeFirst();
        }
        recentPositions.addLast(gpsPosition); //recentPositions.getLast() now points at gpsPosition.

        // work out the distance moved
        if (elapsedDistance == 0) {
            // dist to 1st position of track is just interpolation 
            elapsedDistance += getElapsedDistance();
            interpolationStopwatch.reset();
        } else if (lastPosition != null) {
            // add dist between last and current position
            elapsedDistance += Position.distanceBetween(lastPosition, gpsPosition);
            interpolationStopwatch.reset();
        } else {
            // lastPosition == null but already recorded some distance
            Log.e("GPSTracker", "Likely error: lost the previsou GPS fix so not updating elapsedDistance");
        }
        
        // calculate corrected bearing
        // this is more accurate than the raw GPS bearing as it averages several recent positions
        float[] correctedBearing = calculateCurrentBearing();
        if (correctedBearing != null) {
          gpsPosition.setCorrectedBearing(correctedBearing[0]);
          gpsPosition.setCorrectedBearingR(correctedBearing[1]);
          gpsPosition.setCorrectedBearingSignificance(correctedBearing[2]);
        }
        gpsPosition.save();

        // Broadcast new state to unity3D and to the log
        //broadcastToUnity();
        //logPosition();

    }
    
    /**
     * calculateCurrentBearing uses a best-fit line through the Positions in recentPositions to
     * determine the bearing the user is moving on. We know the raw GPS bearings jump around quite a
     * bit, causing the avatars to jump side to side, and this is an attempt to correct that. There
     * may be some inaccuracies when the bearing is close to due north or due south, as the
     * gradient numbers get close to infinity. We should consider using e.g. polar co-ordinates to
     * correct for this.
     * 
     * @return [corrected bearing, R^2, significance] or null if we're not obviously moving in a direction 
     */
    private float[] calculateCurrentBearing() {
        
        // calculate user's course by drawing a least-squares best-fit line through the last 10 positions
        SimpleRegression linreg = new SimpleRegression();
        for (Position p : recentPositions) {
            linreg.addData(p.getLatx(), p.getLngx());
        }
        
        // if there's a significant chance we don't have a good fit, don't calc a bearing
        if (linreg.getSignificance() > 0.05) return null;
        
        // use course to predict next position of user, and hence current bearing
        Position next = new Position();
        // extrapolate latitude in same direction as last few points
        next.setLatx(2*recentPositions.getLast().getLatx() - recentPositions.getFirst().getLatx());
        // use regression model to predict longitude for the new point
        next.setLngx(linreg.predict(next.getLatx()));
        // return bearing to new point and some stats
        float[] bearing = {
            recentPositions.getLast().bearingTo(next) % 360,  // % 360 converts negative angles to bearings
            (float)linreg.getR(),
            (float)linreg.getSignificance()
        };
        return bearing;

    }
    

    private void broadcastToUnity() {
        JSONObject data = new JSONObject();
        try {
            data.put("hasPosition", hasPosition());
            data.put("currentSpeed", getCurrentSpeed());  
            
            data.put("isTracking", isTracking());
            data.put("elapsedDistance", getElapsedDistance());
            data.put("elapsedTime", getElapsedTime());     
            
            data.put("hasBearing", hasBearing());     
            data.put("currentBearing", getCurrentBearing());            
        } catch (JSONException e) {
            Log.e("GPSTracker", e.getMessage());
        }
        // Sending the message needs the unity native library installed:
        try {
            UnityPlayer.UnitySendMessage("script holder", "NewGPSPosition", data.toString());
        } catch (UnsatisfiedLinkError e) {
            Log.i("GPSTracker","Failed to send unity message, probably because Unity native libraries aren't available (e.g. you are not running this from Unity");
            Log.i("GPSTracker",e.getMessage());
        }
    }
    
    private void logPosition() {
        Log.d("GPSTracker", "New elapsed distance is: " + getElapsedDistance());
        Log.d("GPSTracker", "Current speed estimate is: " + getCurrentSpeed());
        if (hasBearing()) Log.d("GPSTracker", "Current bearing estimate is: " + getCurrentBearing());
        Log.d("GPSTracker", "New elapsed time is: " + getElapsedTime());
        Log.d("GPSTracker", "\n");  
    }

    /**
     * Returns a boolean describing whether the device is moving on a known bearing. Use before getCurrentBearing() if you don't want to handle the -999.0f values returned by that method.
     * 
     * @return true/false - is the deveice moving on a known bearing?
     */
    public boolean hasBearing() {
        if (recentPositions.size() == 0) return false;
        return recentPositions.getLast().getCorrectedBearing() != null;
    }
    
    /**
     * Calculates the device's current bearing based on the last few GPS positions. If unknown (e.g.
     * the device is not moving) returns -999.0f.
     * 
     * @return bearing in degrees
     */
    public float getCurrentBearing() {
        if (recentPositions.size() > 0 && recentPositions.getLast().getCorrectedBearing() != null && !recentPositions.getLast().getCorrectedBearing().isNaN()) {
            return recentPositions.getLast().getCorrectedBearing();
        } else {
            return -999.0f;
        }
    }

    /**
     * Called internally by android. Not currently used.
     */
    @Override
    public void onProviderDisabled(String provider) {
    }

    /**
     * Called internally by android. Not currently used.
     */
    @Override
    public void onProviderEnabled(String provider) {
    }
    
    /**
     * Called internally by android. Not currently used.
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    /**
     * Returns the current speed of the device in m/s, or zero if we think we're stopped.
     * 
     * @return speed in m/s
     */
    public float getCurrentSpeed() {
        if (isIndoorMode()) {
            // need this case to return a speed before the 1st fake position has been generated
            return indoorSpeed;
        } else if (gpsPosition != null) {
            // speed at last GPS fix
            return gpsPosition.getSpeed();
        } else {
            // if no position, we don't know how fast we're moving
            return 0.0f;
        }
    }

    /**
     * Returns the distance covered by the device (in metres) since startTracking was called
     * 
     * @return Distance covered, in metres
     */
    public double getElapsedDistance() {
        // if we're moving, extrapolate distance from the last GPS fix based on the current speed
        // without this, the avatars jump between GPS fix locations
        // this interpolation will probably be moved to unity at some point
        // Log.v("GPSTracker","Interpolation time = " + (double)interpolationStopwatch.elapsedTimeMillis()/1000.0);
        // Log.v("GPSTracker","local elapsedDistance = " + elapsedDistance);
        // Log.v("GPSTracker","getCurrentSpeed returns = " + getCurrentSpeed());
        return elapsedDistance + interpolationStopwatch.elapsedTimeMillis()*getCurrentSpeed()/1000.0;

    }

    /**
     * Returns the cumulative time the isTracking() has been true. See also startTracking() and stopTracking(). 
     * 
     * @return cumulative time in milliseconds
     */
    public long getElapsedTime() {
        return trackStopwatch.elapsedTimeMillis();
    }
    

    private class Stopwatch { 

        private boolean running = false;
        private long elapsedMillis = 0;
        private long lastResumeMillis;

        public Stopwatch() {
        } 
        
        public void start() {
            if (running) {
                return;
            } else {
                running = true;
                lastResumeMillis = System.currentTimeMillis();
            }
        }
        
        public void stop() {
            if (!running) {
                return;
            } else {
                elapsedMillis = elapsedTimeMillis();
                running = false;
            }
        }
        
        public void reset() {
            elapsedMillis = 0;
            lastResumeMillis = System.currentTimeMillis();
        }

        // return time (in seconds) since this object was created
        public long elapsedTimeMillis() {
            if (running) {
                return elapsedMillis + (System.currentTimeMillis() - lastResumeMillis);
            } else {
                return elapsedMillis;
            }            
        } 
        
    } //Stopwatch class

}
