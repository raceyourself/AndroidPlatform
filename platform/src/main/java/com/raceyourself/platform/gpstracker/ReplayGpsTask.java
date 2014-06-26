package com.raceyourself.platform.gpstracker;

import java.io.File;
import java.io.FileReader;
import java.util.TimerTask;

import com.raceyourself.platform.models.Position;

import android.location.Location;
import android.os.Environment;
import au.com.bytecode.opencsv.CSVReader;

/**
 * Task to replay fake position data from CSV file
 */
public class ReplayGpsTask extends TimerTask {
	
	private String filePath = Environment.getExternalStorageDirectory().getPath()+"/Downloads/track.csv";
	private GpsCsvReader reader = null;
	private GPSTracker gpsTracker;
	private boolean isActive;
	
	public ReplayGpsTask(GPSTracker gpsTracker) {
		this.gpsTracker = gpsTracker;
		isActive = new File(filePath).exists();
	}
	
	public void start() {
        try {
        	if (isActive) {
        		reader = new GpsCsvReader(new CSVReader(new FileReader(filePath)));
        	}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public boolean isActive() {
		return isActive; 
	}
	
    public void run() {
    	if (reader == null) {
    		return;
    	}
    	Position p = reader.readNextPosition();
    	if(p == null) {
    		return;
    	}
    	
        // Fake location
        Location location = new Location("");
        location.setTime(/*p.getDeviceTimestamp()*/System.currentTimeMillis());
        location.setLatitude(p.getLatx());
        location.setLongitude(p.getLngx());
        location.setSpeed(p.getSpeed());
        location.setBearing(p.getBearing());
        location.setAccuracy(p.getEpe());

        // Broadcast the fake location the local listener only (otherwise risk
        // confusing other apps!)
        gpsTracker.onLocationChanged(location);
    }
}
