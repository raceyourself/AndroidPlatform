package com.glassfitgames.glassfitplatform.models;

import java.util.List;

import com.roscopeco.ormdroid.Entity;

import static com.roscopeco.ormdroid.Query.eql;

//demo model, will be replaced soon
public class Track extends Entity {

    public int track_id; // id of this track
    public int user_id; // The user who created the track
    public String track_name; // user-entered description of the track
    public int track_type_id; // run, cycle etc..

    public Track() {
        this(null);
    }

    public Track(String track_name) {
        this.track_name = track_name;
    }

    public List<Track> getTracks(int user_id) {
        return query(Track.class).where(eql("user_id", user_id)).executeMulti();
    }

    public String toString() {
        return track_name;
    }
}
