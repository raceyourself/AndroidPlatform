package com.glassfitgames.glassfitplatform.gpstracker;

import java.util.ArrayList;

import android.util.Log;

import com.glassfitgames.glassfitplatform.models.Position;
import com.glassfitgames.glassfitplatform.models.Track;

public class TrackTargetTracker implements TargetTracker {
    private Track track;
    private ArrayList<Position> trackPositions;
    
    private final long startTime; //the start time of the track in milliseconds from 1970
    
    // Cache variables used for performance reasons
    private long currentTime = 0;
    private int currentElement = 0;    
    private double distance = 0.0;
        
    public TrackTargetTracker(Track track) {
        this.track = track;
        this.trackPositions = new ArrayList<Position>(track.getTrackPositions());
        
        Log.i("TargetTracker", "Track " + this.track.getId() + " selected as target.");
        Log.d("TargetTracker", "Track " + track.getId() + " has " + trackPositions.size() + " position elements.");
        if (trackPositions.isEmpty()) {
            startTime = 0;
            return;
        }
        
        startTime = trackPositions.get(0).getDeviceTimestamp();
        Log.v("TargetTracker", "Track start time: " + startTime);
        Log.v("TargetTracker", "Track end time: " + trackPositions.get(trackPositions.size()-1).getDeviceTimestamp());
    }

    /**
     * Returns the speed of the target elapsedTime after the start of the track
     * 
     * @param elapsedTime since the start of the track in milliseconds. Often taken from a
     *            GPStracker.getElapsedTime().
     * @return speed in m/s
     */
    public float getCurrentSpeed(long elapsedTime) {
        if (trackPositions.isEmpty()) return 0;
        
        // otherwise we need to get the speed from the database
        // first, call the distance function to update currentElement
        getCumulativeDistanceAtTime(elapsedTime);
        // then return the speed at the currentElement
        Position currentPosition = trackPositions.get(currentElement);
        if (currentPosition == null) {
            throw new RuntimeException("TargetTracker: CurrentSpeed - cannot find position in track.");
        } else {
//            Log.v("TargetTracker", "The current target pace is " + currentPosition.getSpeed() + "m/s.");
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
        if (trackPositions.isEmpty()) return 0;
        
        // if using a previous track log, need to loop through its positions to find the one
        // with timestamp startTime + time
        Position currentPosition = trackPositions.get(currentElement);
        if (currentElement + 1 >= trackPositions.size()) return distance;  //check if we hit the end of the track
        Position nextPosition = trackPositions.get(currentElement + 1);

        // update to most recent position
        while (nextPosition != null && nextPosition.getDeviceTimestamp() - startTime <= time && currentElement + 1 < trackPositions.size()) {
            distance += Position.distanceBetween(currentPosition, nextPosition);
//            Log.v("TargetTracker", "The distance travelled by the target is " + distance + "m.");
            currentElement++;
            currentPosition = nextPosition;
            nextPosition = null;
        }
        
        //interpolate between most recent and upcoming (future) position 
        double interpolation = 0.0;
        if (currentElement + 1 < trackPositions.size()) {
            nextPosition = trackPositions.get(currentElement + 1);
        }
        if (nextPosition != null) {
            long timeBetweenPositions = nextPosition.getDeviceTimestamp() - currentPosition.getDeviceTimestamp();
            if (timeBetweenPositions != 0) {
                float proportion = ((float)time-(currentPosition.getDeviceTimestamp()-startTime))/timeBetweenPositions;
                interpolation = Position.distanceBetween(currentPosition, nextPosition) * proportion;
//                Log.v("TargetTracker", "interp: " + interpolation + " " + proportion + " t: " + (currentPosition.getDeviceTimestamp()-startTime) + " " + time + " x: " + timeBetweenPositions + " " + ((float)time-(currentPosition.getDeviceTimestamp()-startTime)));
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
        return this.currentElement >= trackPositions.size() - 1;
    }
    
    /**
     * Sets the track based on the user's selection
     */
    public void setTrack(Track track) {
    	this.track = track;
    	currentElement = 0;
    }
}
