package com.raceyourself.platform.models;

import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.Table;

@Table(name = "matched_tracks")
public class MatchedTrack extends Entity {
    public int id; // auto-incremented
    public int device_id;
    public int track_id;

    public MatchedTrack() {}
    public MatchedTrack(Track track) {
        this.device_id = track.device_id;
        this.track_id = track.track_id;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Track) return equals((Track)other);
        return super.equals(other);
    }

    public boolean equals(Track track) {
        return (device_id == track.device_id && track_id == track.track_id);
    }
}
