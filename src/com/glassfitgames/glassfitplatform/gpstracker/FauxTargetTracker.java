package com.glassfitgames.glassfitplatform.gpstracker;

import android.util.Log;


public class FauxTargetTracker implements TargetTracker {
    private float speed = TargetSpeed.JOGGING.speed();
    
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
    
    public FauxTargetTracker(TargetSpeed speed) {
        this(speed.speed);
    }
    
    public FauxTargetTracker(float speed) {
        setSpeed(speed);
        Log.i("TargetTracker", "Target Tracker created with target speed of " + speed + "m/s.");
    }

    /**
     * Returns the speed value that has been set with setSpeed.
     * 
     * @param elapsedTime since the start of the track in milliseconds. Often taken from a
     *            GPStracker.getElapsedTime().
     * @return speed in m/s
     */
    public float getCurrentSpeed(long elapsedTime) {
        return speed;
    }
    
    /**
     * Calculates travelled distance on track between start and time
     * 
     * @param time in milliseconds
     * @return distance in meters 
     */
    public double getCumulativeDistanceAtTime(long time) {
        return (double)speed * time / 1000.0;
    }
    
    /**
     * Previous track logs have a length, so will finish at some point. Use this method to find out
     * whether we've got to the end of the pre-recorded track.
     * 
     * @return true if the target track has played all the way through, false otherwise
     */
    public boolean hasFinished() {
        return false;
    }
    
}
