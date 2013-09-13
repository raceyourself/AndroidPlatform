package com.glassfitgames.glassfitplatform.models;

import static com.roscopeco.ormdroid.Query.eql;

import java.util.List;

import android.location.Location;

import com.roscopeco.ormdroid.Entity;

//demo model, will be replaced soon
public class Position extends Entity {

	public int id; // ideally auto-increment
	public int track_id;
	public int state_id;
	public long ts;
	public double latx; // Latitude
	public double lngx; // longitude
	public double altitude;
	public float bearing; // which way are we pointing?
	public float epe; // estimated GPS position error
	public String nmea; // full GPS NMEA string
	public float speed; // speed in m/s

	public long getTimestamp() {
		return ts;
	}
	
	public Position() {
    }

    public Position(int trackId, Location location) {
        track_id = 0;
        ts = location.getTime();
        latx = location.getLatitude();
        lngx = location.getLongitude();
        if (location.hasAltitude()) altitude = location.getAltitude();
        if (location.hasBearing()) bearing = location.getBearing();
        if (location.hasSpeed()) bearing = location.getSpeed();
    }
    
    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

	public float getBearing() {
        return bearing;
    }

    public void setBearing(float bearing) {
        this.bearing = bearing;
    }
    
    public double getLatx() {
        return latx;
    }

    public void setLatx(double latx) {
        this.latx = latx;
    }

    public double getLngx() {
        return lngx;
    }

    public void setLngx(double lngx) {
        this.lngx = lngx;
    }
    
    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public List<Position> getPositions(int track_id) {
		return query(Position.class).where(eql("track_id", track_id))
				.executeMulti();
	}

	public String toString() {
		return nmea;
	}
	
	public static int elapsedTimeBetween(Position a, Position b) {
		// TODO: Verify this is correct code, even with differing time zones and whatnot
		return (int)Math.abs(a.getTimestamp() - b.getTimestamp());
	}
	
	public static long distanceBetween(Position a, Position b) {
		float results[] = new float[1];
		Location.distanceBetween(a.getLatx(), a.getLngx(), b.getLatx(), b.getLngx(), results);
		return (long)results[0];
	}

}
