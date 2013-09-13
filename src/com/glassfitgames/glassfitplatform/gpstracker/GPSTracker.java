package com.glassfitgames.glassfitplatform.gpstracker;

import java.util.Timer;
import java.util.TimerTask;

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

import com.glassfitgames.glassfitplatform.models.Position;

public class GPSTracker extends Service implements LocationListener, TrackerInterface {

	private final Context mContext;

	// flag for GPS status
	boolean isGPSEnabled = false;

	// flag for network status
	boolean isNetworkEnabled = false;

	// flag for GPS status
	boolean canGetLocation = false;

	Position currentPosition;
	Location location; // location
	double latitude; // latitude
	double longitude; // longitude

	// The minimum distance to change Updates in meters
	private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

	// The minimum time between updates in milliseconds
	private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

	// Declaring a Location Manager
	protected LocationManager locationManager;

	private Timer timer;
	private GpsTask task;

	public GPSTracker(Context context) {
		this.mContext = context;
	}

	public Position getCurrentPosition() throws Exception {
		currentPosition = new Position();
		if (isGPSEnabled) {
			if (location == null) {
				locationManager.requestLocationUpdates(
						LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES,
						MIN_DISTANCE_CHANGE_FOR_UPDATES,
						(LocationListener) this);
				Log.d("GPS Enabled", "GPS Enabled");
				if (locationManager != null) {
					location = locationManager
							.getLastKnownLocation(LocationManager.GPS_PROVIDER);
					if (location != null) {
						latitude = location.getLatitude();
						longitude = location.getLongitude();
						currentPosition.setLatx(latitude);
						currentPosition.setLngx(longitude);

					}
				}
			} else {
				latitude = location.getLatitude();
				longitude = location.getLongitude();
				currentPosition.setLatx(latitude);
				currentPosition.setLngx(longitude);

			}
		}
		if (currentPosition == null)
			throw new Exception();
		return currentPosition;
	}

	public void initGps() {
		try {
			locationManager = (LocationManager) mContext
					.getSystemService(LOCATION_SERVICE);

			// getting GPS status
			isGPSEnabled = locationManager
					.isProviderEnabled(LocationManager.GPS_PROVIDER);

			// getting network status
			isNetworkEnabled = locationManager
					.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

			if (!isGPSEnabled && !isNetworkEnabled) {
				// no network provider is enabled
			} else {
				this.canGetLocation = true;
				if (isNetworkEnabled) {
					locationManager.requestLocationUpdates(
							LocationManager.NETWORK_PROVIDER,
							MIN_TIME_BW_UPDATES,
							MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
					Log.d("Network", "Network");
					if (locationManager != null) {
						location = locationManager
								.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
						if (location != null) {
							latitude = location.getLatitude();
							longitude = location.getLongitude();
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void startTracking() {
        if (canGetLocation) {
            // start TimerTask to poll GPS once per second
            timer = new Timer();
            task = new GpsTask();
            timer.scheduleAtFixedRate(task, 0, 1000);
        }
	}

	public void stopTracking() {

		task.cancel();
	}

    private class GpsTask extends TimerTask {

        public void run() {

            // update currentPosition from GPS and save it
            if (canGetLocation) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                currentPosition = new Position(trackId, location);
                currentPosition.save();
            }
        }
    }

	/**
	 * Stop using GPS listener Calling this function will stop using GPS in your
	 * app
	 * */
	public void stopUsingGPS() {
		if (locationManager != null) {
			locationManager.removeUpdates(GPSTracker.this);
		}
	}

	/**
	 * Function to get latitude
	 * */
	public double getLatitude() {
		if (location != null) {
			latitude = location.getLatitude();

		}

		// return latitude
		return latitude;
	}

	/**
	 * Function to get longitude
	 * */
	public double getLongitude() {
		if (location != null) {
			longitude = location.getLongitude();
		}

		// return longitude
		return longitude;
	}

	/**
	 * Function to check GPS/wifi enabled
	 * 
	 * @return boolean
	 * */
	public boolean canGetLocation() {
		return this.canGetLocation;
	}

	/**
	 * Function to show settings alert dialog On pressing Settings button will
	 * lauch Settings Options
	 * */
	public void showSettingsAlert() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

		// Setting Dialog Title
		alertDialog.setTitle("GPS is setAbstractTrackertings");

		// Setting Dialog Message
		alertDialog
				.setMessage("GPS is not enabled. Do you want to go to settings menu?");

		// On pressing Settings button
		alertDialog.setPositiveButton("Settings",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(
								Settings.ACTION_LOCATION_SOURCE_SETTINGS);
						mContext.startActivity(intent);
					}
				});

		// on pressing cancel button
		alertDialog.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});

		// Showing Alert Message
		alertDialog.show();
	}

	@Override
	public void onLocationChanged(Location location) {
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

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	public float getCurrentPace() {
		return currentPosition.getSpeed();
	}

	public void saveTrack() {
		// TODO Auto-generated method stub

	}

    @Override
    public long getElapsedDistance() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getElapsedTime() {
        // TODO Auto-generated method stub
        return 0;
    }

}