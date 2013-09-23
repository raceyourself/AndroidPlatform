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
    private TargetSpeed targetSpeed = null;
    
    private long startTime; //the start time of the track in milliseconds from 1970
    private long currentTime = 0;
    private int currentElement = 0;
    
    long distance = 0l;
    
    /**
     * Enum of reference speeds from http://en.wikipedia.org/wiki/Orders_of_magnitude_(speed)
     *
     */
    public enum TargetSpeed {
        WALKING (1.25f),
        JOGGING (2.7f),
        MARATHON_RECORD (5.72f),
        USAIN_BOLT (10.438f);
        
        private float speed;
        
        TargetSpeed(float speed) { this.speed = speed; }
        
        public float speed() { return speed; }
    
    }
    
    /**
     * 
     * @return the current TargetSpeed enum for this TargetTracker, or null if not set
     */
    public TargetSpeed getTargetSpeed() {
        return targetSpeed;
    }

    /**
     * Set the target speed using a member of the TargetSpeed enum
     * If null, will default to the fist stored track in the database
     * 
     * @param targetSpeed
     */
    public void setTargetSpeed(TargetSpeed targetSpeed) {
        this.targetSpeed = targetSpeed;
    }
        
	
    public TargetTracker(TargetSpeed targetSpeed) {
        
        // if a target speed has been passed, the job of this class is easy, 
        // no database access required
        if (targetSpeed != null) {
            this.targetSpeed = targetSpeed;
            Log.i("TargetTracker", "Target Tracker created with target speed of " + targetSpeed.toString());
            return;
        }
        
        // if NO target speed was passed, default to using the first track in the database
        Log.i("TargetTracker", "Looking for first Track in database...");
    	do {
	    	track = Entity.query(Track.class).orderBy("id asc").execute();
//	    	track = Entity.query(Track.class).where(Query.eql("id", trackId)).execute();
	    	if (track == null) throw new IllegalArgumentException("No such track");
	    	Log.i("TargetTracker", "Track: " + track.getId() + " selected as target.");
	    	
	    	trackPositions = new ArrayList<Position>(track.getTrackPositions());
	    	if (trackPositions.isEmpty()) track.delete();
    	} while (trackPositions.isEmpty());
    	Log.i("TargetTracker", "Track: " + track.getId() + " has " + trackPositions.size() + " position elements.");
    	
    	startTime = trackPositions.get(0).getTimestamp();
    	Log.i("TargetTracker", "Start time: " + currentTime);
    	Log.i("TargetTracker", "Stop time: " + trackPositions.get(trackPositions.size()-1).getTimestamp());
    	Log.i("TargetTracker", "Stop time: " + trackPositions.get(trackPositions.size()-1).getTimestamp());
    	
    }

    /**
     * TODO: Implement according to spec
     * NOTE: Does not? update internal state
     * 
     * @param elapsedTime in milliseconds
     * @return pace in m/s
     */
    public float getCurrentPace(long elapsedTime) {

        // if we have a set target speed, just return it
        if (targetSpeed != null) {
            Log.d("TargetTracker", "The current target pace is " + targetSpeed.speed + "m/s.");
            return targetSpeed.speed();
        }
        
        // otherwise we need to get the speed from the database
    	throw new RuntimeException("Not implemented for previous tracks, try setting a target speed.");
    }
    
    /**
     * Calculates travelled distance on track between start and time
     * NOTE: Updates internal state (distance += elapsed distance since last call)
     * 
     * @param time in milliseconds
     * @return distance in meters 
     */
    public long getCumulativeDistanceAtTime(long time) {
        
        // if we have a set target speed, just calculate the distance covered since time=0
        if (targetSpeed != null) {
            distance = (long)(targetSpeed.speed() * time / 1000f);
            Log.d("TargetTracker", "The distance travelled by the target is " + distance + "m.");
            return distance;
        }

        // if using a previous track log, need to loop through its positions to find the one
        // with timestamp startTime + time
        Position currentPosition = trackPositions.get(currentElement);
        if (currentElement + 1 >= trackPositions.size()) return 0;  //check if we hit the end of the track
        Position nextPosition = trackPositions.get(currentElement + 1);

        while (nextPosition.getTimestamp() - startTime <= time && currentElement + 1 < trackPositions.size()) {
            distance += Position.distanceBetween(currentPosition, nextPosition);
            Log.d("GlassFitPlatform", "Cumulative distance = " + distance);
            currentElement++;
            currentPosition = nextPosition;
            nextPosition = trackPositions.get(currentElement + 1);
        }
        // TODO: Wrap around?
        // Log.i("TargetTracker", "Element: " + currentElement);
        return distance;

    }
    
}
