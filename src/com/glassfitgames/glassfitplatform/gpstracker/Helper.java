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

	public static float getCurrentPace() {
		Log.i("platform.gpstracker.Helper", "getCurrentPace() called");
		return 0f; // TODO - implement this!
	}

	public static Position getCurrentPosition(Context context) {

		GPSTracker gps = new GPSTracker(context);
		double lat = gps.getLatitude();
		double lon = gps.getLongitude();
		String lat1 = Double.toString(lat);
		String lon1 = Double.toString(lon);
		Float lat2 = Float.parseFloat(lat1);
		Float lon2 = Float.parseFloat(lon1);

		Position pos = new Position();
		pos.latx = lat2;
		pos.lngx = lon2;
		Log.i("platform.gpstracker.Helper", "getCurrentPosition() called");
		return new Position(); // TODO - implement this!
	}

	public static float getTargetPace() {
		Log.i("platform.gpstracker.Helper", "getTargetPace() called");
		return 0f; // TODO - implement this!
	}

	public static Position getTargetPosition() {
		Log.i("platform.gpstracker.Helper", "getTargetPosition() called");
		return new Position(); // TODO - implement this!
	}

	public static void startLogging() {
		Log.i("platform.gpstracker.Helper", "startLogging() called");
	}

	public static void stopLogging() {
		Log.i("platform.gpstracker.Helper", "stopLogging() called");
	}

	public static void pauseLogging() {
		Log.i("platform.gpstracker.Helper", "pauseLogging() called");
	}

	public static void syncToServer() {
		Log.i("platform.gpstracker.Helper", "syncToServer() called");
	}

	/* Class My Location Listener */

}
