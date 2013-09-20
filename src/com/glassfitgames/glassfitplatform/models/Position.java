package com.glassfitgames.glassfitplatform.models;

import static com.roscopeco.ormdroid.Query.eql;
import java.util.List;

import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import com.roscopeco.ormdroid.Entity;

//demo model, will be replaced soon
public class Position extends Entity {

	public int id; // ideally auto-increment
	public int track_id;
	public int state_id;
	public long ts;
	public double latx; // Latitude
	public double lngx; // longitude
	public Double altitude; // can be null
	public Float bearing; // which way are we pointing? Can be null
	public Float corrected_bearing; // based on surrounding points. Can be null.
	public Float corrected_bearing_R; // correlation coefficient of bearing vector to recent positions
	public Float corrected_bearing_significance; // significance of fit of corrected bearing
	public Float epe; // estimated GPS position error, can be null
	public String nmea; // full GPS NMEA string
	public float speed; // speed in m/s

	public long getTimestamp() {
		return ts;
	}
	
	public int getTrackId(){
		return track_id;
	}
	
	public int getStateId(){
		return state_id;
	}

	public Position() {
  }

  public Position(int trackId, Location location) {
      this.track_id = trackId;
      ts = location.getTime();
      latx = location.getLatitude();
      lngx = location.getLongitude();
      epe = location.getAccuracy();
      if (location.hasAltitude()) altitude = location.getAltitude();
      if (location.hasBearing()) bearing = location.getBearing();
      if (location.hasSpeed()) speed = location.getSpeed();
  }
  
  public Double getAltitude() {
      return altitude;
  }

  public void setAltitude(Double altitude) {
      this.altitude = altitude;
  }

	public Float getBearing() {
      return bearing;
  }

  public void setBearing(Float bearing) {
      this.bearing = bearing;
  }
  
    public Float getCorrectedBearing() {
        return corrected_bearing;
    }

    public void setCorrectedBearing(Float corrected_bearing) {
        this.corrected_bearing = corrected_bearing;
    }

  public Float getCorrectedBearingR() {
      return corrected_bearing_R;
    }

    public void setCorrectedBearingR(Float corrected_bearing_R) {
      this.corrected_bearing_R = corrected_bearing_R;
    }

    public Float getCorrectedBearingSignificance() {
      return corrected_bearing_significance;
    }

    public void setCorrectedBearingSignificance(Float corrected_bearing_significance) {
      this.corrected_bearing_significance = corrected_bearing_significance;
    }

  public Float getEpe() {
      return epe;
  }

  public void setEpe(Float epe) {
      this.epe = epe;
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
		Log.i("PositionCompare", a.getLatx() + "," + a.getLngx() + " vs " + b.getLatx() + "," + b.getLngx() + " => " + results[0]);
		return Float.valueOf(results[0]).longValue();
	}
	
	public float bearingTo(Position destination) {
	    
	    Location la = this.toLocation();
	    Location lb = destination.toLocation();    
	    return la.bearingTo(lb);    
	}
	
	public Location toLocation() {
        Location l = new Location(LocationManager.GPS_PROVIDER);
        l.setLatitude(getLatx());
        l.setLongitude(getLngx());
        return l;
	}

}