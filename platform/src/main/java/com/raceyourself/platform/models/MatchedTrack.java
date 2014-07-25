package com.raceyourself.platform.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.Table;

import java.util.List;

import static com.roscopeco.ormdroid.Query.eql;

@Table(name = "matched_tracks")
public class MatchedTrack extends Entity {
    public int id; // auto-incremented
    public int device_id;
    public int track_id;

    @JsonIgnore
    public boolean dirty = false;

    public MatchedTrack() {}
    public MatchedTrack(Track track) {
        this.device_id = track.device_id;
        this.track_id = track.track_id;
        this.dirty = true;
    }

    // Clear synced matched tracks, as they are already filtering tracks on the server
    public static void clearSynced() {
        List<MatchedTrack> matches = query(MatchedTrack.class).where(eql("dirty", false)).executeMulti();
        for (MatchedTrack match : matches) {
            match.delete();
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Track) return equals((Track)other);
        return super.equals(other);
    }

    public boolean equals(Track track) {
        return (device_id == track.device_id && track_id == track.track_id);
    }

    public void flush() {
        if (dirty) {
            dirty = false;
            save();
        }
    }
}
