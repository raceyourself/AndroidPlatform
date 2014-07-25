package com.raceyourself.platform.models;

import java.nio.ByteBuffer;
import java.util.Date;

import android.location.Location;
import android.location.LocationManager;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;
import com.roscopeco.ormdroid.Entity;

/**
 * Position sample.
 * Linked to track using foreign key (track_id, device_id)
 *
 * Consistency model: Client can add or delete.
 *                    Server can upsert/delete using compound key.
 */
public class Position extends Entity {

	// Globally unique compound key (orientation, device)
	public int position_id;
    public int device_id;
    // Globally unique foreign key (track, device)
    public int track_id; 
    // Encoded id for local db
    @JsonIgnore
    public long id = 0; 

	// Fields
	public int state_id; // 0=unknown, 1=stopped, 2=sensor_acc, 3=steady_gps, 4=coast, 5=sensor_dec  negative numbers are same but 'not important' for recreating shape of track
	public long gps_ts;
	public long device_ts;
	@JsonProperty("lat")
	public double latx; // Latitude
	@JsonProperty("lng")
	public double lngx; // longitude
	@JsonProperty("alt")
	public Double altitude; // can be null
	public Float bearing; // which way are we pointing? Can be null
	public Float corrected_bearing; // based on surrounding points. Can be null.
	public Float corrected_bearing_R; // correlation coefficient of bearing vector to recent positions
	public Float corrected_bearing_significance; // significance of fit of corrected bearing
	public Float epe; // estimated GPS position error, can be null
	public String nmea; // full GPS NMEA string
	public float speed; // speed in m/s
	@JsonIgnore
	private static double INV_R = 0.0000001569612306; // 1/earth's radius (meters)

    @JsonIgnore
    public boolean dirty = false;
    public Date deleted_at = null;
	
    public void setGpsTimestamp(long timestamp) {
        gps_ts = timestamp;
    }

    public long getGpsTimestamp() {
        return gps_ts;
    }

    public void setDeviceTimestamp(long timestamp) {
        device_ts = timestamp;
    }

    public long getDeviceTimestamp() {
        return device_ts;
    }
	
    public void setStateId(int stateId){
        this.state_id = stateId;
    }

    public int getStateId(){
		return state_id;
	}

	public Position() {
  }

	public Position(Track track, Location location) {
      if (track == null) {
          this.device_id = 0;
          this.track_id = 0;
      } else {
          this.device_id = track.device_id;
          this.track_id = track.track_id;
      }
      this.position_id = 0; // Set in save()
      gps_ts = location.getTime();
      device_ts = System.currentTimeMillis();
      latx = location.getLatitude();
      lngx = location.getLongitude();
      epe = location.getAccuracy();
      if (location.hasAltitude()) altitude = location.getAltitude();
      if (location.hasBearing()) bearing = location.getBearing();
      if (location.hasSpeed()) speed = location.getSpeed();
      dirty = true;
  }

	public Position(Position p) {
		this.device_id = p.device_id;
        this.track_id = p.track_id;
        this.position_id = 0; // Set in save()
        this.gps_ts = p.gps_ts;
        this.device_ts = p.device_ts;
        this.latx = p.latx;
        this.lngx = p.lngx;
        this.epe = p.epe;
        this.altitude = p.altitude;
        this.bearing = p.bearing;
        this.speed = p.speed;
        dirty = true;
	}

  public void setTrack(Track track) {
      this.device_id = track.device_id;
      this.track_id = track.track_id;
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
  
  public boolean hasValidCoordinates() {
	  return !Double.isNaN(lngx) && !Double.isNaN(latx);
  }
  
  public float getSpeed() {
      return speed;
  }

  public void setSpeed(float speed) {
      this.speed = speed;
  }

    public String toString() {
		return nmea == null ? "Position: lat " + latx + ", long " + lngx : nmea;
	}

	public static int elapsedTimeBetween(Position a, Position b) {
		// TODO: Verify this is correct code, even with differing time zones and whatnot
		return (int)Math.abs(a.getDeviceTimestamp() - b.getDeviceTimestamp());
	}

	public static double distanceBetween(Position a, Position b) {
        LatLng fromL = new LatLng(a.getLatx(), a.getLngx());
        LatLng toL = new LatLng(b.getLatx(), b.getLngx());
        return LatLngTool.distance(fromL, toL, LengthUnit.METER );
	}
	
	public float bearingTo(Position destination) {
	    Location la = this.toLocation();
	    Location lb = destination.toLocation();    
	    return la.bearingTo(lb);    
	}
	
	// Precise position prediction based on the last
    // position, bearing and speed
    public static Position predictPosition(Position aLastPosition, long milliseconds) {
       if (aLastPosition.getSpeed() < 0.01) {
           return aLastPosition;
       }
       if (aLastPosition.getBearing() == null) {
         return null;
       }

       Position next = new Position();
       double d = aLastPosition.getSpeed() * milliseconds / 1000.0f; // distance = speed(m/s) * time (s)

       double dR = d*INV_R;
       // Convert bearing to radians
       double brng = Math.toRadians(aLastPosition.getBearing());
       double lat1 = Math.toRadians(aLastPosition.getLatx());
       double lon1 = Math.toRadians(aLastPosition.getLngx());
       System.out.printf("d: %f, dR: %f; brng: %f\n", d, dR, brng);
       // Predict lat/lon
       double lat2 = Math.asin(Math.sin(lat1)*Math.cos(dR) + 
                    Math.cos(lat1)*Math.sin(dR)*Math.cos(brng) );
       double lon2 = lon1 + Math.atan2(Math.sin(brng)*Math.sin(dR)*Math.cos(lat1), 
                     Math.cos(dR)-Math.sin(lat1)*Math.sin(lat2));
       // Convert back to degrees
       next.setLatx((float)Math.toDegrees(lat2));
       next.setLngx((float)Math.toDegrees(lon2));
       next.setGpsTimestamp(aLastPosition.getGpsTimestamp() + milliseconds);
       next.setDeviceTimestamp(aLastPosition.getDeviceTimestamp() + milliseconds);
       next.setBearing(aLastPosition.getBearing());
       next.setSpeed(aLastPosition.getSpeed());
       
       return next;
    }
	
	public Location toLocation() {
        Location l = new Location(LocationManager.GPS_PROVIDER);
        l.setLatitude(getLatx());
        l.setLongitude(getLngx());
        return l;
	}
	
	@Override
	public int save() {
	    if (device_id == 0 && track_id == 0) throw new RuntimeException("Cannot store temporary position without track");
		if (position_id == 0) position_id = Sequence.getNext("position_id");
		if (id == 0) {
			ByteBuffer encodedId = ByteBuffer.allocate(8);
			encodedId.putInt(device_id);
			encodedId.putInt(position_id);
			encodedId.flip();
			this.id = encodedId.getLong();
		}
		return super.save();
	}

	
	@Override
	public void delete() {
		deleted_at = new Date();
		save();
	}
	
	public void flush() {
		if (deleted_at != null) {
			super.delete();		
			return;
		}
		if (dirty) {
			dirty = false;
			save();
		}
	}

    public static Position getMostRecent() {
        return (Position)Entity.query(Position.class).orderBy("device_ts desc").limit(1).execute();
    }	
}