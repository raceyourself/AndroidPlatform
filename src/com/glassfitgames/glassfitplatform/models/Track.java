package com.glassfitgames.glassfitplatform.models;

import static com.roscopeco.ormdroid.Query.and;
import static com.roscopeco.ormdroid.Query.eql;
import static com.roscopeco.ormdroid.Query.geq;
import static com.roscopeco.ormdroid.Query.leq;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.Query;

//demo model, will be replaced soon
public class Track extends Entity {

	@JsonProperty("track_id")
    public int id; // id of this track
    public int user_id; // The user who created the track
    public String track_name; // user-entered description of the track
    public int track_type_id; // run, cycle etc..
    public long ts;

    public Track() {
        this(null);
    }

    public Track(String track_name) {
        this.track_name = track_name;
        this.ts = System.currentTimeMillis();
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
    	return query(Position.class).where(eql("track_id", id)).executeMulti();
    }

    public int getId() {
    	return id;
    }
    
    public String toString() {
        return track_name;
    }
    
	public static List<Track> getData(long lastSyncTime, long currentSyncTime) {
		return Query
				.query(Track.class)
				.where(and(geq("ts", lastSyncTime), leq("ts", currentSyncTime)))
				.executeMulti();
	}
	
	@Override
	public void delete() {
		for(Position p : getTrackPositions()) {
			p.delete();
		}
		super.delete();
	}
}
