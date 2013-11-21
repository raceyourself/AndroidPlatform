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
    private Float speed = TargetSpeed.JOGGING.speed();
    
    private long startTime; //the start time of the track in milliseconds from 1970
    private long currentTime = 0;
    private int currentElement = 0;
    
    double distance = 0.0;
    
    /**
     * Enum of reference speeds from http://en.wikipedia.org/wiki/Orders_of_magnitude_(speed)
     *
     */
    public enum TargetSpeed {
        WALKING (1.25f),
        JOGGING (2.4f),
        MARATHON_RECORD (5.72f),
        USAIN_BOLT (10.438f);
        
        private float speed;
        
        TargetSpeed(float speed) { this.speed = speed; }
        
        public float speed() { return speed; }
    
    }
    
    /**
     * 
     * @return the current speed (in m/s) for this TargetTracker
     * @deprecated
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * Set the speed of this target using a member of the TargetSpeed enum
     * 
     * @param targetSpeed
     */
    public void setSpeed(TargetSpeed targetSpeed) {
        this.speed = targetSpeed.speed();
        Log.i("TargetTracker", "TargetTracker set to " + this.speed + "m/s.");
    }
    
    /**
     * Set the speed of this target in m/s
     * 
     * @param speed m/s
     */
    public void setSpeed(float speed) {
        this.speed = speed;
        Log.i("TargetTracker", "TargetTracker set to " + this.speed + "m/s.");
    }
    
    /**
     * Set the previous track log the target should use for speed/distance values.
     * 
     * @param trackId the track to use
     */
    public void setTrack(int trackId) {
        this.speed = null;
        this.track = Track.get(trackId);
       //this.track = Track.getMostRecent();
        while (track == null || track.getTrackPositions().isEmpty()) {
            if (track == null) {
                throw new IllegalArgumentException("There are no saved tracks.");
            } else {
                track.delete();  // delete any empty tracks
                this.track = Track.getMostRecent();  // try for next most recent
            }
        }
        
        Log.i("TargetTracker", "Track " + this.track.getId() + " selected as target.");
        Log.d("TargetTracker", "Track " + track.getId() + " has " + trackPositions.size() + " position elements.");
        startTime = trackPositions.get(0).getDeviceTimestamp();
        Log.v("TargetTracker", "Track start time: " + currentTime);
        Log.v("TargetTracker", "Track end time: " + trackPositions.get(trackPositions.size()-1).getDeviceTimestamp());
    }
        
	
    public TargetTracker() {
        Log.i("TargetTracker", "Target Tracker created with target speed of " + speed + "m/s.");
    }

    /**
     * Returns the speed of the target elapsedTime after the start of the track, or simply the speed
     * value if it has been set with setSpeed.
     * 
     * @param elapsedTime since the start of the track in milliseconds. Often taken from a
     *            GPStracker.getElapsedTime().
     * @return speed in m/s
     */
    public float getCurrentSpeed(long elapsedTime) {

        // if we have a set target speed, just return it
        if (speed != null) {
            Log.v("TargetTracker", "The current target pace is " + speed + "m/s.");
            return speed;
        }
        
        // otherwise we need to get the speed from the database
        // first, call the distance function to update currentElement
        getCumulativeDistanceAtTime(elapsedTime);
        // then return the speed at the currentElement
        Position currentPosition = trackPositions.get(currentElement);
        if (currentPosition == null) {
            throw new RuntimeException("TargetTracker: CurrentSpeed - cannot find position in track.");
        } else {
            Log.v("TargetTracker", "The current target pace is " + currentPosition.getSpeed() + "m/s.");
            return currentPosition.getSpeed();
        }
        
    }
    
    /**
     * Calculates travelled distance on track between start and time
     * NOTE: Updates internal state (distance += elapsed distance since last call)
     * 
     * @param time in milliseconds
     * @return distance in meters 
     */
    public double getCumulativeDistanceAtTime(long time) {
        
        if (speed == null && track == null) {
            throw new RuntimeException("TargetTracker: Cannot return distance when no speed or track set.");
        }
        
        // if we have a set target speed, just calculate the distance covered since time=0
        if (speed != null) {
            distance = (double)speed * time / 1000.0;
            Log.v("TargetTracker", "The distance travelled by the target is " + distance + "m.");
            return distance;
        }

        // if using a previous track log, need to loop through its positions to find the one
        // with timestamp startTime + time
        Position currentPosition = trackPositions.get(currentElement);
        if (currentElement + 1 >= trackPositions.size()) return 0;  //check if we hit the end of the track
        Position nextPosition = trackPositions.get(currentElement + 1);
        Position futurePosition = null;

        // update to most recent position
        while (nextPosition.getDeviceTimestamp() - startTime <= time && currentElement + 1 < trackPositions.size()) {
            distance += Position.distanceBetween(currentPosition, nextPosition);
            Log.v("TargetTracker", "The distance travelled by the target is " + distance + "m.");
            currentElement++;
            currentPosition = nextPosition;
            nextPosition = trackPositions.get(currentElement + 1);
        }
        
        //interpolate between most recent and upcoming (future) position 
        double interpolation = 0.0;
        if (currentElement + 2 < trackPositions.size()) {
            futurePosition = trackPositions.get(currentElement + 2);
        }
        if (futurePosition != null) {
            long timeBetweenPositions = futurePosition.getDeviceTimestamp() - nextPosition.getDeviceTimestamp();
            if (timeBetweenPositions != 0) {
                float proportion = ((float)time-nextPosition.getDeviceTimestamp())/timeBetweenPositions;
                interpolation = Position.distanceBetween(nextPosition, futurePosition) * proportion;
            }
        }
        
        // return up-to-the-millisecond distance
        // note the distance variable is just up to the most recent Position
        return distance + interpolation;

    }
    
    /**
     * Previous track logs have a length, so will finish at some point. Use this method to find out
     * whether we've got to the end of the pre-recorded track.
     * 
     * @return true if the target track has played all the way through, false otherwise
     */
    public boolean hasFinished() {
        return this.currentElement == trackPositions.size();
    }
    
    /**
     * Sets the track based on the user's selection
     */
    public void setTrack(Track track) {
    	this.track = track;
    	currentElement = 0;
    }
}
