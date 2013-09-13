package com.glassfitgames.glassfitplatform.gpstracker;

import java.util.ArrayList;

import android.util.Log;

import com.glassfitgames.glassfitplatform.models.Position;
import com.glassfitgames.glassfitplatform.models.Track;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.Query;


public class TargetTracker {
    private Track track;
    private ArrayList<Position> trackPositions;
    
    private long currentTime = 0;
    private int currentElement = 0;
    
    long distance = 0l;
        
	
    public TargetTracker(int trackId) {
    	do {
	    	track = Entity.query(Track.class).orderBy("id asc").execute();
//	    	track = Entity.query(Track.class).where(Query.eql("id", trackId)).execute();
	    	if (track == null) throw new IllegalArgumentException("No such track");
	    	Log.i("TargetTracker", "Track: " + track.getId());
	    	
	    	trackPositions = new ArrayList<Position>(track.getTrackPositions());
	    	if (trackPositions.isEmpty()) track.delete();
    	} while (trackPositions.isEmpty());
    	Log.i("TargetTracker", "Position elements: " + trackPositions.size());
    	
    	currentTime = trackPositions.get(0).getTimestamp();
    	Log.i("TargetTracker", "Start time: " + currentTime);
    	Log.i("TargetTracker", "Stop time: " + trackPositions.get(trackPositions.size()-1).getTimestamp());
    }

    /**
     * TODO: Implement according to spec
     * NOTE: Does not? update internal state
     * 
     * @param elapsedTime in milliseconds
     * @return pace in m/s
     */
    public float getCurrentPace(int elapsedTime) {
    	throw new RuntimeException("Not implemented");
    }
    
    /**
     * Calculates travelled distance on track between start and time
     * NOTE: Updates internal state (distance += elapsed distance since last call)
     * 
     * @param time in milliseconds
     * @return distance in meters 
     */
    public long getCumulativeDistanceAtTime(long time) {

        Position currentPosition = trackPositions.get(currentElement);
        if (currentElement + 1 >= trackPositions.size()) return 0;
        Position nextPosition = trackPositions.get(currentElement + 1);

        // long actualElapsed = elapsedTime + (currentTime -
        // currentPosition.getTimestamp());

        // Log.d("TargetTracker", "Current el: " + currentElement + ", time: " +
        // currentPosition.getTimestamp());
        while (nextPosition.getTimestamp() <= time && currentElement + 1 < trackPositions.size()) {
            distance += Position.distanceBetween(currentPosition, nextPosition);
            currentElement++;
            currentPosition = nextPosition;
            nextPosition = trackPositions.get(currentElement + 1);
        }
        // TODO: Wrap around?
        // Log.i("TargetTracker", "Element: " + currentElement);
        return distance;

    }
    
}
