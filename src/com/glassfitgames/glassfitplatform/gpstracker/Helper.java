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

	public float getCurrentPace() {
		Log.i("platform.gpstracker.Helper", "getCurrentPace() called");
		return 0f; // TODO - implement this!
	}

	public Position getCurrentPosition() {

		LocationListener mlocListener = new MyLocationListener();

		Log.i("platform.gpstracker.Helper", "getCurrentPosition() called");
		return new Position(); // TODO - implement this!
	}

	public float getTargetPace() {
		Log.i("platform.gpstracker.Helper", "getTargetPace() called");
		return 0f; // TODO - implement this!
	}

	public Position getTargetPosition() {
		Log.i("platform.gpstracker.Helper", "getTargetPosition() called");
		return new Position(); // TODO - implement this!
	}

	public void startLogging() {
		Log.i("platform.gpstracker.Helper", "startLogging() called");
	}

	public void stopLogging() {
		Log.i("platform.gpstracker.Helper", "stopLogging() called");
	}

	public void pauseLogging() {
		Log.i("platform.gpstracker.Helper", "pauseLogging() called");
	}

	public void syncToServer() {
		Log.i("platform.gpstracker.Helper", "syncToServer() called");
	}

	/* Class My Location Listener */

	public class MyLocationListener implements LocationListener {

		@Override
		public void onLocationChanged(Location loc) {
			// TODO Auto-generated method stub
			double lati = loc.getLatitude();
			double longi = loc.getLongitude();

		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub

		}
	}
}
