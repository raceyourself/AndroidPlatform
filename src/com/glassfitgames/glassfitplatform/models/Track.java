package com.glassfitgames.glassfitplatform.models;

import static com.roscopeco.ormdroid.Query.and;
import static com.roscopeco.ormdroid.Query.eql;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.roscopeco.ormdroid.Entity;

/**
 * Track.
 * Links together various track data through 1:N relations.
 *
 * Consistency model: Client can add or delete.
 *                    Server can upsert/delete using compound key.
 */
public class Track extends Entity {
	
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
    public long ts;
    
    // Metadata
    @JsonIgnore
    public boolean dirty = false;
    public Date deleted_at = null;

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
    
    public static Track get(int id) {
        return query(Track.class).where(eql("id",id)).execute();
    }

    public static Track getMostRecent() {
        return query(Track.class).orderBy("id desc").limit(1).execute();
    }
    
    public static List<Track> getTracks() {
        return query(Track.class).executeMulti();
    }

    public List<Position> getTrackPositions() {
    	return query(Position.class).where(and(eql("track_id", id), eql("device_id", device_id))).executeMulti();
    }

    public List<Orientation> getTrackOrientations() {
    	return query(Orientation.class).where(and(eql("track_id", id), eql("device_id", device_id))).executeMulti();
    }

    public String toString() {
        return track_name;
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
}
