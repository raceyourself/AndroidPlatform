package com.glassfitgames.glassfitplatform.models;

import static com.roscopeco.ormdroid.Query.and;
import static com.roscopeco.ormdroid.Query.eql;
import static com.roscopeco.ormdroid.Query.leq;
import static com.roscopeco.ormdroid.Query.geq;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.glassfitgames.glassfitplatform.models.EntityCollection.CollectionEntity;

/**
 * Track.
 * Links together various track data through 1:N relations.
 *
 * Consistency model: Client can add or delete.
 *                    Server can upsert/delete using compound key.
 */
public class Track extends CollectionEntity {
	
	// Globally unique compound key
	public int device_id; // The device that created the track
    public int track_id; // The device-local track id
	// Encoded id for local db
    @JsonIgnore
	public long id = 0;
    
	// Fields
    public Integer user_id; // The user who created the track (may be null)
    public String track_name; // user-entered description of the track
    public int track_type_id; // run, cycle etc..    
    public double distance; // Total distance travelled
    public long ts;
    public long time;
    
    // Metadata
    @JsonIgnore
    public boolean dirty = false;
    public Date deleted_at = null;
    
    // internal fields used for iterating through the positions in this track
    @JsonIgnore
    private final String LOGTAG = "GlassFitPlatform - Track";
    @JsonIgnore
    private List<Position> trackPositions;
    @JsonIgnore
    private int currentElement = 0;
    @JsonIgnore
    private double distanceAccumulator = 0.0;
    @JsonIgnore
    private long trackStartTime;
    @JsonIgnore
    private Metric metric;

    public Track() {
    }

    public Track(int userId, String track_name) {
    	this.user_id = userId;
    	this.device_id = Device.self().getId();
    	this.track_id = Sequence.getNext("track_id");
        this.track_name = track_name;
        this.ts = System.currentTimeMillis();
        this.dirty = true;
    }
    
    public static Track get(int device_id, int track_id) {
        return query(Track.class).where(and(eql("track_id", track_id), eql("device_id", device_id))).execute();
    }

    public static List<Track> getTracks() {
        return query(Track.class).executeMulti();
    }

    public static List<Track> getTracks(double maxDistance, double minDistance) {
    	return query(Track.class).where(and(leq("distance", maxDistance), geq("distance", minDistance))).executeMulti();
    }
    
    public void setPositions(List<Position> positions) {
        for (Position position : positions) {
            position.save();
            position.flush();
        }
    }
    
    public List<Position> getTrackPositions() {
    	return query(Position.class).where(and(eql("track_id", track_id), eql("device_id", device_id))).executeMulti();
    }

    public List<Orientation> getTrackOrientations() {
    	return query(Orientation.class).where(and(eql("track_id", track_id), eql("device_id", device_id))).executeMulti();
    }

    public String getName() {
        return track_name;
    }
    
    public String toString() {
        return "Track: name=" + track_name;
    }
    
    public int[] getIDs() {
    	int[] ids = new int[2];
    	ids[0] = device_id;
    	ids[1] = track_id;
    	
    	return ids;
    }
    
    public String getId() {
    	return device_id + "-" + track_id;
    }
        
	@Override
	public void delete() {		
		for(Position p : getTrackPositions()) {
			p.delete();
		}
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
	
	@Override
	public int save() {
		if (id == 0) {
			ByteBuffer encodedId = ByteBuffer.allocate(8);
			encodedId.putInt(device_id);
			encodedId.putInt(track_id);
			encodedId.flip();
			this.id = encodedId.getLong();
		}
		return super.save();				
	}
	
	@Override
	public void erase() {
            for(Position p : getTrackPositions()) {
                p.delete();
            }
	    super.erase();
	}
	
	public Position getPositionAtTime(long time) {
	    
	    // refresh list of track positions from database (may have updated if e.g. currently being recorded)
	    trackPositions = getTrackPositions();
	    if (trackPositions.size() == 0) {
	        Log.e(LOGTAG,"Cannot get position from track " + this.getId() + "because it has no position elements");
	        return null;
	    }
	    
	    // start from beginning of track if we've switched metrics
	    if (metric != Metric.TIME) {
	        currentElement = 0;
	        distanceAccumulator = 0.0;
	        trackStartTime = trackPositions.get(0).getDeviceTimestamp();
	        metric = Metric.TIME;
	    }
	    
	    // iterate through the track until we find the required position
	    Position p1 = null;
	    Position p2 = null;
	    while (true) {
	        
	        p1 = trackPositions.get(currentElement);
	        if (currentElement == trackPositions.size()-1) {
	            // if we're on the last element, extrapolate
	            return p1.predictPosition(p1, time);
	        }
	        
	        p2 = trackPositions.get(currentElement+1);
	        if (p2.getDeviceTimestamp() - trackStartTime > time) {
	            // if the next element is too far ahead, interpolate
	            float proportion = (float)(time - p1.getDeviceTimestamp()) / (p2.getDeviceTimestamp() - p1.getDeviceTimestamp());
	            return interpolate(p1, p2, proportion);
	        } else {
	            // increment current position & update cumulative numbers
	            distanceAccumulator += Position.distanceBetween(p1, p2);
	            currentElement++;
	        }
        }
	    
	}
	
	
	public Position getPositionAtDistance(double distance) {
	    
	    // refresh list of track positions from database (may have updated if e.g. currently being recorded)
        trackPositions = getTrackPositions();
        if (trackPositions.size() == 0) {
            Log.e(LOGTAG,"Cannot get position from track " + this.getId() + "because it has no position elements");
            return null;
        }
        
        // start from beginning of track if we've switched metrics
        if (metric != Metric.DISTANCE) {
            currentElement = 0;
            distanceAccumulator = 0.0;
            metric = Metric.DISTANCE;
        }
        
        // iterate through the track until we find the required position
        Position p1 = null;
        Position p2 = null;
        while (true) {
            
            p1 = trackPositions.get(currentElement);
            if (currentElement == trackPositions.size()-1) {
                // if we're on the last element, extrapolate
                return p1.predictPosition(p1, (long)(distance/p1.getSpeed()*1000.0));
            }
            
            p2 = trackPositions.get(currentElement+1);
            if (distanceAccumulator + Position.distanceBetween(p1,p2) > distance) {
                // if the next element is too far ahead, interpolate
                float proportion = (float)((distance-distanceAccumulator) / Position.distanceBetween(p1, p2));
                return interpolate(p1, p2, proportion);
            } else {
                // increment current position & update cumulative numbers
                distanceAccumulator += Position.distanceBetween(p1, p2);
                currentElement++;
            }
        }

	}
	
	
	private enum Metric {
        TIME,
        DISTANCE
    }
    
	// TODO: move to Position.interpolate
	// TODO: spline rather than linear interpolation
    private Position interpolate(Position p1, Position p2, float proportion) {
        
        double lat = p1.getLatx() + proportion*(p2.getLatx() - p1.getLatx());
        double lng = p1.getLngx() + proportion*(p2.getLngx() - p1.getLngx());
        long ts = p1.getDeviceTimestamp() + (long)proportion*(p2.getDeviceTimestamp() - p1.getDeviceTimestamp());
        float bng = p1.getBearing() + proportion*(p2.getBearing() - p1.getBearing());
        double dist = distanceAccumulator + proportion*Position.distanceBetween(p1, p2);
        float speed = p1.getSpeed() + proportion*(p2.getSpeed() - p1.getSpeed());
        
        // update current position
        Position p = new Position();
        p.setLatx(lat);
        p.setLngx(lng);
        p.setDeviceTimestamp(ts);
        p.setBearing(bng);
        p.setSpeed(speed);
        return p;
    }
    
}
