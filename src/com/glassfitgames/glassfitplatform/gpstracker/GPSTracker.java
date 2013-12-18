
package com.glassfitgames.glassfitplatform.gpstracker;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.glassfitgames.glassfitplatform.gpstracker.FauxTargetTracker.TargetSpeed;
import com.glassfitgames.glassfitplatform.models.Position;
import com.glassfitgames.glassfitplatform.models.Track;
import com.glassfitgames.glassfitplatform.models.UserDetail;
import com.glassfitgames.glassfitplatform.sensors.SensorService;
import com.roscopeco.ormdroid.ORMDroidApplication;
import com.unity3d.player.UnityPlayer;

public class GPSTracker implements LocationListener {

    private final Context mContext;
    
    // current state of device - stopped, accelerating etc
    private State state = State.STOPPED;
    
    // ordered list of recent positions, last = most recent
    private ArrayDeque<Position> recentPositions = new ArrayDeque<Position>(10);

    // position predictor (based on few last positions)
    private PositionPredictor positionPredictor = new PositionPredictor();
        
    // last known position, just for when we're not tracking
    Position gpsPosition = null;

    // flag for whether we're actively tracking
    private boolean isTracking = false;

    private boolean indoorMode = true; // if true, we generate fake GPS updates

    private float minIndoorSpeed = 1.0f; // speed to fake with no user-stimulation
    private float maxIndoorSpeed = 3.0f; // speed to fake with continuous stimulation
    private float outdoorSpeed = 0.0f; // speed based on GPS & sensors, updated regularly

    private Track track; // The current track

    private double distanceTravelled = 0.0; // distance so far using speed/time in metres
    private double gpsDistance = 0.0; // distance so far between GPS points in metres

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
    public Tick tick;
    
    private ServiceConnection sensorServiceConnection;
    private SensorService sensorService;
    

    /**
     * Creates a new GPSTracker object.
     * <p>
     * Initialises the database to store track logs and checks that the device has GPS enabled.
     * <p>
     */
    public GPSTracker(Context context) {
        this.mContext = context;

        // makes sure the database exists, if not - create it
        ORMDroidApplication.initialize(context);
        Log.i("ORMDroid", "Initalized");
        
        // set elapsed time/distance to zero
        reset();
        
        // locationManager allows us to request/cancel GPS updates
        locationManager = (LocationManager)mContext.getSystemService(Service.LOCATION_SERVICE);
        
        // connect to the sensor service
        sensorServiceConnection = new ServiceConnection() {

            public void onServiceConnected(ComponentName className, IBinder binder) {
                sensorService = ((SensorService.SensorServiceBinder)binder).getService();
                Log.d("GPSTracker", "Bound to SensorService");
            }

            public void onServiceDisconnected(ComponentName className) {
                sensorService = null;
                Log.d("GPSTracker", "Unbound from SensorService");
            }
        };
        
        // Connect to sensorService (Needs doing each time the activity is resumed)
        onResume();
        
        // trigger either real (false) or fake (true) GPS updates
        setIndoorMode(false);
        
        tick = new Tick();

    }
    
    /**
     * Starts / re-starts the methods that poll GPS and sensors.
     * This is NOT an override - needs calling manually by containing Activity on app. resume
     * Safe to call repeatedly
     * 
     */
    public void onResume() {
        
        if (isIndoorMode()) {
            
            // stop listening for real GPS signals (doesn't matter if called repeatedly)
            locationManager.removeUpdates(this);
            
            // generate fake GPS updates if not already happening
            if (task == null) {
                Log.d("GPSTracker", "Requesting fake GPS updates");
                task = new GpsTask();
                timer.scheduleAtFixedRate(task, 0, 1000);
            }
 
            // initialise speed to minIndoorSpeed
            outdoorSpeed = minIndoorSpeed;
            Log.i("GPSTracker", "Indoor mode active");
            
        } else {

            // stop generating fake GPS updates
            if (task != null) {
                task.cancel();
                task = null;
            }

            // request real GPS updates (doesn't matter if called repeatedly)
            List<String> l = locationManager.getAllProviders();
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            String provider = locationManager.getBestProvider(criteria, true);
            if (locationManager.isProviderEnabled(provider)) {
                locationManager.requestLocationUpdates(provider, MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, (LocationListener) this);
                Log.i("GPSTracker", "Outdoor mode active, using " + provider + " provider.");
            } else {
                Log.e("GPSTracker", "GPS provider not enabled, cannot start outdoor mode.");
            }

        }
        
        // start polling the sensors
        mContext.bindService(new Intent(mContext, SensorService.class), sensorServiceConnection,
                        Context.BIND_AUTO_CREATE);
        if (tick == null) {
            tick = new Tick();
            timer.scheduleAtFixedRate(tick, 0, 50);
        }
    }

    /**
     * Stops the methods that poll GPS and sensors
     * This is NOT an override - needs calling manually by containing Activity on app. resume
     * Safe to call repeatedly
     */
    public void onPause() {
        
        // stop requesting real GPS updates (doesn't matter if called repeatedly)
        locationManager.removeUpdates(this);
        
        // stop generating fake GPS updates
        if (task != null) {
            task.cancel();
            task = null;
        }
        
        // stop polling the sensors
        if (tick != null) {
            tick.cancel();
            tick = null;
        }
        mContext.unbindService(sensorServiceConnection);
        
    }
    
    
    
    public void reset() {
        
        Log.d("GPSTracker", "GPS tracker reset");
        
        trackStopwatch.stop();
        trackStopwatch.reset();
        interpolationStopwatch.stop();
        interpolationStopwatch.reset();
        isTracking = false;
        distanceTravelled = 0.0;
        gpsDistance = 0.0;
        outdoorSpeed = 0.0f;
        state = State.STOPPED;
        recentPositions.clear();
        
        track = null;
    }
    

    /**
     * Returns the position of the device as a Position object, whether or not we are tracking.
     * 
     * @return position of the device
     */
    public Position getCurrentPosition() {
        return gpsPosition;
        //TODO: need to extrapolate based on sensors/bike-wheel/SLAM
    }
  

    /**
     * Start recording distance and time covered by the device.
     * <p>
     * Ideally this should only be called once the device has a GPS fix, which can be checked using
     * hasPosition().
     */
    public void startTracking() {
        
        Log.d("GPSTracker", "startTracking() called, hasPosition() is " + hasPosition());
        
        
        UserDetail me = UserDetail.get();        
        track = new Track(me.getGuid(), "Test");
        Log.v("GPSTracker", "New track created");        
        track.save();
        Log.d("GPSTracker", "New track ID is " + track.getId());                

        // Set track for temporary position
        if (gpsPosition != null) gpsPosition.setTrack(track);
        
        // if we already have a position, start the stopwatch, if not it'll
        // be triggered when we get our first decent GPS fix
        if (hasPosition()) {
            trackStopwatch.start();
            interpolationStopwatch.start();
        }
        isTracking = true;

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
     * indoorMode == false => Listen for real GPS positions
     * indoorMode == true => Generate fake GPS positions
     * See also setIndoorSpeed().
     * 
     * @param indoorMode: true for indoor, false for outdoor.
     */
    public void setIndoorMode(boolean indoorMode) {
        
        // if we are changing mode, clear the position buffer so we don't record
        // a huge jump from a fake location to a real one or vice-versa
        if (indoorMode != this.indoorMode) {
            recentPositions.clear();
            this.indoorMode = indoorMode;
        }
        
        // start listening to relevant GPS/sensors for new mode
        onResume();
    }

    /**
     * Sets the speed for indoor mode from the TargetSpeed enum
     * of reference speeds. See also isIndoorMode().
     * 
     * @param indoorSpeed enum
     */
    public void setIndoorSpeed(TargetSpeed indoorSpeed) {
        this.maxIndoorSpeed = indoorSpeed.speed();
    }
    
    /**
     * Sets the speed for indoor mode to the supplied float value,
     * measured in m/s. See also isIndoorMode().
     * 
     * @param indoorSpeed in m/s
     */
    public void setIndoorSpeed(float indoorSpeed) {
        this.maxIndoorSpeed = indoorSpeed;
    }    
    
    /**
     * Call when the user wants to reset which way is forward. Useful when device is not moving (as
     * GPS bearing doesn't work in that case). Used to find the forward-backward axis in the
     * acceleration calc.
     */
    public void resetGyros() {
        // empty for now, until we introduce sensor code
    }

    /**
     * Task to regularly generate fake position data when in indoor mode.
     * startLogging() triggers starts the task, stopLogging() ends it.
     */
    private class GpsTask extends TimerTask {

        private double[] drift = { 0f, 0f }; // lat, long
        private float yaw = 0.0f;

        public void run() {

            // Fake movement in direction device is pointing at a speed
            // determined by how much the user is shaking it (uses same sensor
            // logic / state machine as outdoor mode)
            if (sensorService != null) {
                // get device yaw to work out direction to move in
                yaw = (float) (sensorService.getYprValues()[0] * 180 / Math.PI);
                drift[0] += outdoorSpeed * Math.cos(yaw) / 111229d;
                drift[1] += outdoorSpeed * Math.sin(yaw) / 111229d;
            }

            // Fake location
            Location location = new Location("");
            location.setTime(System.currentTimeMillis());
            location.setLatitude(location.getLatitude() + drift[0]);
            location.setLongitude(location.getLongitude() + drift[1]);
            location.setSpeed(outdoorSpeed);
            location.setBearing(yaw);

            // Broadcast the fake location the local listener only (otherwise
            // risk confusing other apps!)
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
                // we check EPE==0 to discard any real positions left from
                // before an indoorMode switch
                // Log.v("GPSTracker", "We have a fake position ready to use");
                return true;
            }
            if (!isIndoorMode() && gpsPosition.getEpe() > 0
                            && gpsPosition.getEpe() < MAX_TOLERATED_POSITION_ERROR) {
                // we check EPE>0 to discard any fake positions left from before
                // an indoorMode switch
                // Log.v("GPSTracker", "We have a real position ready to use");
                return true;
            }
        }

        // Log.v("GPSTracker", "We don't currently have a valid position");
        return false;

    }
    // Broadcast new state to unity3D and to the log
    //broadcastToUnity();
    
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
     * Called by the android system when new GPS data arrives.
     * <p>
     * This method does all the clever processing of the raw GPS data to provide an accurate
     * location and bearing of the device. It also increments the elapsedDistance when isTracking is
     * true and the device is moving.
     */
    @Override
    public void onLocationChanged(Location location) {
     
        // get the latest GPS position
        Position tempPosition = new Position(track, location);
        //Log.i("GPSTracker", "New position with error " + tempPosition.getEpe());
        
        // if the latest gpsPosition doesn't meets our accuracy criteria, throw it away
        if (tempPosition.getEpe() > MAX_TOLERATED_POSITION_ERROR) {
            Log.d("GPSTracker", "Throwing away position as error is > " + MAX_TOLERATED_POSITION_ERROR);
            return;
        }       
        
        // update current position
        // TODO: kalman filter to smooth GPS points?
        Position lastPosition = gpsPosition;
        gpsPosition = tempPosition;
        
        // stop here if we're not tracking
        if (!isTracking) {
            //broadcastToUnity();
            return;
        }
        
        // keep track of the pure GPS distance moved
        if (lastPosition != null && state != State.STOPPED) {
            // add dist between last and current position
            // don't add distance if we're stopped, it's probably just drift 
            gpsDistance += Position.distanceBetween(lastPosition, gpsPosition);
        }
        interpolationStopwatch.reset();

        // add position to the buffer for later use
//        Log.d("GPSTracker", "Using position as part of track");
        if (recentPositions.size() >= 10) {
            // if the buffer is full, discard the oldest element
            recentPositions.removeFirst();
        }
        recentPositions.addLast(gpsPosition); //recentPositions.getLast() now points at gpsPosition.
        
        // calculate corrected bearing
        // this is more accurate than the raw GPS bearing as it averages several recent positions
        correctBearing(gpsPosition);
        
        gpsPosition.save();
        //logPosition();
        
    }
    
    // calculate corrected bearing
    // this is more accurate than the raw GPS bearing as it averages several recent positions
    private void correctBearing(Position gpsPosition) {
        // interpolate last few positions 
        positionPredictor.updatePosition(gpsPosition);
        Float correctedBearing = positionPredictor.predictBearing(gpsPosition.getDeviceTimestamp());
        if (correctedBearing != null) {
          gpsPosition.setCorrectedBearing(correctedBearing);
          // TODO: remove these fields from Position class
          gpsPosition.setCorrectedBearingR((float)1.0);
          gpsPosition.setCorrectedBearingSignificance((float)1.0);
        }    
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
     * @return true/false - is the device moving on a known bearing?
     */
    public boolean hasBearing() {
        // TODO: is this function still needed? getCurrentBearing() may be used instead
        return (positionPredictor.predictBearing(System.currentTimeMillis()) != null);
    }
    
    /**
     * Calculates the device's current bearing based on the last few GPS positions. If unknown (e.g.
     * the device is not moving) returns -999.0f.
     * 
     * @return bearing in degrees
     */
    public float getCurrentBearing() {
        Float bearing = positionPredictor.predictBearing(System.currentTimeMillis());
        if (bearing != null) {
            return bearing;
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
        return outdoorSpeed; // calculated regularly using GPS & sensors
    }
    
    /**
     * Computes forward-vector in real-world co-ordinates
     * Uses GPS bearing if available (needs device to be moving forward), otherwise magnetometer (assumes device is facing forward)
     * @return [x,y,x] forward vector
     */
    private float[] getForwardVector() {
        float[] forwardVector = {0f,1f,0f};
        if (sensorService == null) return forwardVector;
        float bearing;
        //if (hasBearing()) {
        //    bearing = getCurrentBearing(); // based on GPS points
        //} else {
            bearing = (float)(sensorService.getYprValues()[0]);  // based on device orientation/magnetometer, converted to degrees            
        //}
        forwardVector[0] = (float)Math.sin(bearing); //note bearing is in radians
        forwardVector[1] = (float)Math.cos(bearing);        
        return forwardVector;
    }
    
    public float getYaw() {
        if (sensorService == null) return 0.0f;
        return (float)(sensorService.getYprValues()[0]*180/Math.PI) % 360;  // based on device orientation/magnetometer, converted to degrees
    }
    
    public float getForwardAcceleration() {
        if (sensorService == null) return 0.0f;
        return sensorService.getAccelerationAlongAxis(getForwardVector());
    }
    
    public float getTotalAcceleration() {
        if (sensorService == null) return 0.0f;
        return sensorService.getTotalAcceleration();
    }
    


    /**
     * Returns the distance covered by the device (in metres) since startTracking was called
     * 
     * @return Distance covered, in metres
     */
    public double getElapsedDistance() {
        return distanceTravelled;
    }
    
    public double getGpsDistance() {
        return gpsDistance;
    }
    
    public float getGpsSpeed() {
        if (gpsPosition != null) {
            return gpsPosition.getSpeed();
        }
        return 0.0f;
    }
    
    public State getState() {
        return state;
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
    
    
    private float meanDfa = 0.0f; // mean acceleration change in the forward-backward axis
    private float meanDta = 0.0f; // mean acceleration change (all axes combined)
    private float sdTotalAcc = 0.0f; // std deviation of total acceleration (all axes)
    private float maxDta = 0.0f; // max change in total acceleration (all axes)
    private double extrapolatedGpsDistance = 0.0; // extraploated distance travelled (based on sensors) to add to GPS distance
    
    public float getMeanDfa() {
        return meanDfa;
    }
    
    public float getMeanDta() {
        return meanDta;
    }
    
    public float getSdTotalAcc() {
        return sdTotalAcc;
    }
    
    public float getMaxDta() {
        return maxDta;
    }
    
    public double getExtrapolatedGpsDistance() {
        return extrapolatedGpsDistance;
    }
    
    public Track getTrack() {
        return track;
    }
    
    public class Tick extends TimerTask {

        private long tickTime;
        private long lastTickTime;
        private float gpsSpeed = 0.0f;
        private float lastForwardAcc = 0.0f;
        private float lastTotalAcc = 0.0f;
        private DescriptiveStatistics dFaStats = new DescriptiveStatistics(5);
        private DescriptiveStatistics dTaStats = new DescriptiveStatistics(5);
        private DescriptiveStatistics taStats = new DescriptiveStatistics(5);

        public void run() {
            
            // need to wait for sensorService to bind
            if (sensorService == null) return;

            if (lastTickTime == 0) {
                lastTickTime = System.currentTimeMillis();
                return;
            }
            tickTime = System.currentTimeMillis();

            // update buffers with most recent sensor sample
            dFaStats.addValue(Math.abs(getForwardAcceleration()-lastForwardAcc));
//            rmsForwardAcc = (float)Math.sqrt(0.95*Math.pow(rmsForwardAcc,2) + 0.05*Math.pow(getForwardAcceleration(),2));
            taStats.addValue(sensorService.getTotalAcceleration());
            dTaStats.addValue(Math.abs(sensorService.getTotalAcceleration()-lastTotalAcc));
            
            // compute some stats on the buffers
            // TODO: frequency analysis
            meanDfa = (float)dFaStats.getMean();
            meanDta = (float)dTaStats.getMean();
            maxDta = (float)dTaStats.getMax();
            sdTotalAcc = (float)taStats.getStandardDeviation();
            gpsSpeed = getGpsSpeed();
            
            // update state
            // gpsSpeed = -1.0 for indoorMode to prevent entry into
            // STEADY_GPS_SPEED (just want sensor_acc/dec)
            state = state.nextState(meanDta, (isIndoorMode() ? -1.0f : gpsSpeed));
            
            // save for next loop
            lastForwardAcc = getForwardAcceleration();
            lastTotalAcc = sensorService.getTotalAcceleration();
            
            // adjust speed
            switch (state) {
                case STOPPED:
                    // speed is zero!
                    outdoorSpeed = 0.0f;
                    break;
                case SENSOR_ACC:
                    // increase speed at 1.0m/s/s (typical walking acceleration)
                    float increment = 1.0f * (tickTime - lastTickTime) / 1000.0f;

                    // cap speed at 1.0 m/s walking pace (or maxIndoorSpeed in
                    // indoorMode)
                    // TODO: freq analysis to identify running => increase speed cap
                    if (outdoorSpeed + increment < (isIndoorMode() ? maxIndoorSpeed : 1.0f)) {
                        outdoorSpeed += increment;
                    } else {
                        outdoorSpeed = (isIndoorMode() ? maxIndoorSpeed : 1.0f);
                    }
                    break;
                case STEADY_GPS_SPEED:
                    // smoothly adjust speed toward the GPS speed
                    // TODO: maybe use acceleration sensor here to make this more responsive?
                    outdoorSpeed = 0.9f * outdoorSpeed + 0.1f * gpsSpeed;
                    break;
                case COAST:
                    // maintain constant speed
                    break;
                case SENSOR_DEC:
                    // decrease speed at 2.0 m/s/s till we are stopped (or 
                    // minIndoorSpeed in indoorMode)
                    float decrement = 2.0f * (tickTime - lastTickTime) / 1000.0f;
                    if (outdoorSpeed -decrement > (isIndoorMode() ? minIndoorSpeed : 0.0f)) {
                        outdoorSpeed -= decrement;
                    } else {
                        outdoorSpeed = (isIndoorMode() ? minIndoorSpeed : 0.0f);
                    }
                    break;
            }

            // update elapsed distance
            if (isTracking()) {
                switch (state) {
                    case STOPPED:
                    case STEADY_GPS_SPEED:
                    case SENSOR_DEC:
                        // adjust distance steadily towards GPS distance to keep it accurate
                        // need to extrapolate GPS dist from last fix based on speed/time
                        // TODO: switch exp-smoothing to spline/bezier
                        
                        // extrapolate based on last known fix + outdoor speed
                        // accurate but not responsive
                        extrapolatedGpsDistance = gpsDistance
                                + (interpolationStopwatch.elapsedTimeMillis()) * outdoorSpeed / 1000.0f;
                       
                        // increment distance travelled by integrating current
                        // speed. Responsive but drifts, so use complimentary filter
                        // to make it tend slowly towards the more accurate
                        // extrapolatedGpsDistance
                        distanceTravelled = 
                            0.95f * (distanceTravelled + outdoorSpeed * (tickTime - lastTickTime) / 1000.0f) 
                          + 0.05f * extrapolatedGpsDistance;
                        
                        break;
                    case SENSOR_ACC:
                    case COAST:
                        // GPS distance cannot be trusted here, so use our speed to estimate
                        // distance
                        distanceTravelled += outdoorSpeed * (tickTime - lastTickTime) / 1000.0f;
                        break;
                }
            }
            
            lastTickTime = tickTime;

        }
    }
    
    public enum State {
        STOPPED {
            @Override
            public State nextState(float rmsForwardAcc, float gpsSpeed) {
                if (rmsForwardAcc > ACCELERATE_THRESHOLD) {
                    return State.SENSOR_ACC.setEntryTime(System.currentTimeMillis());
                } else {
                    return this;
                }
            }
        },
        
        SENSOR_ACC {
            @Override
            public State nextState(float rmsForwardAcc, float gpsSpeed) {
                if (gpsSpeed > 0.0f) {
                    return State.STEADY_GPS_SPEED.setEntryTime(System.currentTimeMillis());
                } else if (rmsForwardAcc < DECELERATE_THRESHOLD) {
                    return State.SENSOR_DEC.setEntryTime(System.currentTimeMillis());
                } else {
                    return this;
                }
            }
        },
        
        STEADY_GPS_SPEED {
            public State nextState(float rmsForwardAcc, float gpsSpeed) {
                if (rmsForwardAcc < DECELERATE_THRESHOLD) {
                    // if the sensors suggest the device has stopped moving, decelerate
                    // TODO: pick up when we're in a tunnel and need to coast
                    return State.SENSOR_DEC.setEntryTime(System.currentTimeMillis());
                } else if (gpsSpeed == 0.0f) {
                    // if we've picked up a dodgy GPS position, maintain const speed
                    return State.COAST.setEntryTime(System.currentTimeMillis());
                } else {
                    return this;
                }                
            }
        },
        
        COAST {
            public State nextState(float rmsForwardAcc, float gpsSpeed) {
                if (rmsForwardAcc < DECELERATE_THRESHOLD) {
                    // if sensors suggest the device has stopped moving, decelerate
                    return State.SENSOR_DEC.setEntryTime(System.currentTimeMillis());
                } else if (gpsSpeed > 0.0f) {
                    // we've picked up GPS again
                    return State.STEADY_GPS_SPEED.setEntryTime(System.currentTimeMillis());
                } else {
                    return this;
                }                
            }            
        },
        
        SENSOR_DEC{
            public State nextState(float rmsForwardAcc, float gpsSpeed) {
                if (gpsSpeed == 0.0f) {
                    return State.STOPPED.setEntryTime(System.currentTimeMillis());
                } else if (getTimeInState() > 3000) {
                    return State.STEADY_GPS_SPEED.setEntryTime(System.currentTimeMillis());
                } else if (rmsForwardAcc > ACCELERATE_THRESHOLD) {
                    return State.SENSOR_ACC.setEntryTime(System.currentTimeMillis());
                } else {
                    return this;
                }
            }
        };
        
        private long entryTime;
        private final static float ACCELERATE_THRESHOLD = 0.45f;
        private final static float DECELERATE_THRESHOLD = 0.35f;
        
        public abstract State nextState(float rmsForwardAcc, float gpsSpeed);
        
        public State setEntryTime(long t) {
            this.entryTime = t;
            return this;
        }
        
        public long getTimeInState() {
            return System.currentTimeMillis() - entryTime;
        }
    }

}
