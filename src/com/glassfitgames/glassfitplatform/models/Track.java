package com.glassfitgames.glassfitplatform.models;

import static com.roscopeco.ormdroid.Query.eql;

import java.util.Collection;
import java.util.List;

import com.roscopeco.ormdroid.Entity;

//demo model, will be replaced soon
public class Track extends Entity {

    public int id; // id of this track
    public int user_id; // The user who created the track
    public String track_name; // user-entered description of the track
    public int track_type_id; // run, cycle etc..

    public Track() {
        this(null);
    }

    public Track(String track_name) {
        this.track_name = track_name;
    }
    
    public static Track get(int id) {
        return query(Track.class).where(eql("id",id)).execute();
    }

    public static Track getMostRecent() {
        return query(Track.class).orderBy("id desc").limit(1).execute();
    }
    
    public static List<Track> getTracks(int user_id) {
        return query(Track.class).where(eql("user_id", user_id)).executeMulti();
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
}
