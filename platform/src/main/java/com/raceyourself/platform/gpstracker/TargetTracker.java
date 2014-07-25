package com.raceyourself.platform.gpstracker;

public interface TargetTracker {
    /**
     * Returns the speed of the target elapsedTime after the start of the track, or simply the speed
     * value if it has been set with setSpeed.
     * 
     * @param elapsedTime since the start of the track in milliseconds. Often taken from a
     *            GPStracker.getElapsedTime().
     * @return speed in m/s
     */
    public float getCurrentSpeed(long elapsedTime);    
    /**
     * Calculates travelled distance on track between start and time
     * NOTE: Updates internal state (distance += elapsed distance since last call)
     * 
     * @param time in milliseconds
     * @return distance in meters 
     */
    public double getCumulativeDistanceAtTime(long time);
        
    /**
     * Previous track logs have a length, so will finish at some point. Use this method to find out
     * whether we've got to the end of the pre-recorded track.
     * 
     * @return true if the target track has played all the way through, false otherwise
     */
    public boolean hasFinished();    
}
