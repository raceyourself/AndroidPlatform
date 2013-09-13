package com.glassfitgames.glassfitplatform.models;

import java.sql.Timestamp;
import java.util.List;

import com.roscopeco.ormdroid.Entity;

import static com.roscopeco.ormdroid.Query.eql;

//demo model, will be replaced soon
public class Position extends Entity {

	public int position_id; // ideally auto-increment
	public int track_id;
	public int state_id;
	public Timestamp ts;
	public double latx; // Latitude
	public double lngx; // longitude
	public float altitude;
	public float bearing; // which way are we pointing?
	public float epe; // estimated GPS position error
	public String nmea; // full GPS NMEA string

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

	public Position() {
	}

	public Position(int track_id /* , details from GPS sensor */) {
		/* set lat, long, altitude etc */
	}

	public List<Position> getPositions(int track_id) {
		return query(Position.class).where(eql("track_id", track_id))
				.executeMulti();
	}

	public String toString() {
		return nmea;
	}

}
