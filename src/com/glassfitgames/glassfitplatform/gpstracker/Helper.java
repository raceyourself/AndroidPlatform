package com.glassfitgames.glassfitplatform.gpstracker;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
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
    
    GPSTracker gps;
    TargetTracker targetTracker;
    
    /**
     * getCurentPace returns the device's current speed in m/s.
     * 
     * Intended for use by the UI to feed the data to the user.
     * @return current speed in m/s
     */
	public float getCurrentPace() throws LocationNotAvailableException {
		Log.i("platform.gpstracker.Helper", "getCurrentPace() called");
		
		return gps.getCurrentPace();
	}

	/**
	 * getCurentPosition returns a position object containing the device's current lat and long.
	 * @param context
	 * @return
	 */
	public Position getCurrentPosition() {

	    Log.i("platform.gpstracker.Helper", "getCurrentPosition() called");
	    if (gps == null || !gps.canGetLocation()) throw new LocationNotAvailableException();
	    return gps.getCurrentPosition();
	    
	}

	/**
	 * getTargetPace returns the pace from a saved track in m/s.
     * 
     * Intended for use by the UI to feed the data to the user.
     * @return target speed in m/s
	 */
	public float getTargetPace() {
		Log.i("platform.gpstracker.Helper", "getTargetPace() called");
		if (targetTracker == null) throw new TargetNotSetException();
		return targetTracker.getPace();
	}
	
	/**
     * getTargetPosition returns a position object containing the current position of the target user in the target track.
     * @param 
     * @return Position containing the lat/long of the target user
     */
	public Position getTargetPosition() {
		Log.i("platform.gpstracker.Helper", "getTargetPosition() called");
		if (targetTracker == null) throw new TargetNotSetException();
		return targetTracker.getPosition();
	}

	
	
	
	 /**
	 * call initGps before startTracking to give the GPS some time to establish a position, ideally before the user wants it 
	 */
	 public void initGps() {
	     gps = new GPSTracker(getApplicationContext());
	 }
	 
	 /**
	 * startLogging is called by a button on the UI.
	 * 
	 * It registers a locationListener and starts writing a Position
	 * to the database once per second. It keeps a local copy of the latest position
	 * for quick access by the getCurrentLocation method.
	 */
	public void startTracking() {
		Log.i("platform.gpstracker.Helper", "startLogging() called");
		
		//start GPS logging
		if (gps == null || gps.canGetLocation() == false) {
		    throw new LocationNotAvailableException();
		} else {
		    gps.startTracking();
		}
		
		//start playing through the saved track if the user has specified one
		if (targetTracker != null) {
		    targetTracker.startTracking();
		}
	}
	
	/**
	 * setTargetTracker allows the UI to choose which track they want as their target.
	 * 
	 * It creates a TargetTracker object which is responsible for returning positions, 
	 * orientations etc on the target track.
	 * 
	 * Developers should call this before startLogging if they want a target.
	 * 
	 * @param trackId
	 */
	public void setTargetTrack(Integer trackId) {
	    targetTracker = new TargetTracker(trackId);
	}

	/** 
	 * stopLogging is called by a button on the UI. 
	 */
	public void stopTracking() {
	    gps.stopTracking();
	    gps.saveTrack();
	    targetTracker.stopTracking();
		Log.i("platform.gpstracker.Helper", "stopLogging() called");
	}
	
	/**
	 *
	 */
	public void pauseTracking() {
	    gps.stopTracking();
	    targetTracker.stopTracking();
		Log.i("platform.gpstracker.Helper", "pauseLogging() called");
	}

	/**
	 * syncToServer syncs the local database with the server.
	 * 
	 */
	public void syncToServer() {
		Log.i("platform.gpstracker.Helper", "syncToServer() called");
	}
	
	/** get Helper is a static method allowing unity3D to get an instance of the class
	 * 
	 * @return new Helper instance
	 */
	public static Helper getHelper() {
	    return new Helper();
	}

}
