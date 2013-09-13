package com.glassfitgames.glassfitplatform.gpstracker;

import java.util.ArrayList;

import android.util.Log;

import com.glassfitgames.glassfitplatform.models.Position;
import com.glassfitgames.glassfitplatform.models.Track;
import com.roscopeco.ormdroid.Entity;


public class TargetTracker {
    private Track track;
    private ArrayList<Position> trackPositions;
    
    private long currentTime = 0;
    private int currentElement = 0;
        
	
    public TargetTracker(int trackId) {
    	do {
	    	track = Entity.query(Track.class).orderBy("id desc").execute();
	//    	track = Entity.query(Track.class).where(Query.eql("id", trackId)).execute();
	//    	if (track == null) throw new IllegalArgumentException("No such track");
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
     * Calculates travelled distance on track between currentTime and currentTime+elapsedTime
     * NOTE: Updates internal state (currentTime += elapsedTime)
     * 
     * @param elapsedTime in milliseconds
     * @return distance in meters 
     */
    public long getElapsedDistance(int elapsedTime) {    	
    	long distance = 0l;
    	int elapsed = 0;
    	
    	Position currentPosition = trackPositions.get(currentElement);
    	long actualElapsed = elapsedTime + (currentTime - currentPosition.getTimestamp());
    	
    	Log.d("TargetTracker", "Current el: " + currentElement + ", time: " + currentPosition.getTimestamp());
    	while (elapsed <= actualElapsed && currentElement + 1 < trackPositions.size()) {
        	Position nextPosition = trackPositions.get(currentElement + 1);
        	Log.d("TargetTracker", "Next el: " + (currentElement+1) + ", time: " + nextPosition.getTimestamp());
        	elapsed += Position.elapsedTimeBetween(currentPosition, nextPosition);
        	Log.d("TargetTracker", "Elapsed: " + elapsed);
        	// Only increment element and add distance if ahead of position
    		if (elapsed <= actualElapsed) {
    			distance += Position.distanceBetween(currentPosition, nextPosition);
        		currentElement++;
    		}
    		
    		// Store current for next iteration
    		currentPosition = nextPosition;
    	}
    	// NOTE: Time keeps running while currentElement is at tail
    	// TODO: Wrap around?
    	currentTime += elapsedTime;
    	
    	Log.i("TargetTracker", "Time: " + currentTime + ", element: " + currentElement);
        return distance;
    }
    
}
