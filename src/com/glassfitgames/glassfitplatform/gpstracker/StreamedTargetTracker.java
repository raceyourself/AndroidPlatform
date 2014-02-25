package com.glassfitgames.glassfitplatform.gpstracker;

import java.util.ArrayList;

import android.util.Log;

import com.glassfitgames.glassfitplatform.models.Position;

public class StreamedTargetTracker extends TrackTargetTracker {
    private final long start;
    private boolean finished = false;
    private long updated;

    public StreamedTargetTracker(Position startPosition) {
        super(fromPosition(startPosition));
        updated = System.currentTimeMillis();
        start = System.currentTimeMillis();
    }
    
    private static ArrayList<Position> fromPosition(Position position) {
        Log.i("StreamedTargetTracker", "Tracker created from position: " + position.toCsv());
        ArrayList<Position> positions = new ArrayList<Position>();        
        positions.add(position);
        return positions;
    }

    /**
     * Add a position to the tracker and return the lag (difference between recorded time and actual)
     * Lag may be negative when positions have been delayed (ie. buffered somewhere and sent in a bundle)
     * 
     * @param position to be added
     * @return lag in milliseconds
     */
    public long addPosition(Position position) {
        Log.i("StreamedTargetTracker", "Position added: " + position.toCsv());
        Position tail = getTail();
        trackPositions.add(position);
        
        // Recorded difference between position samples
        long step = position.getDeviceTimestamp() - tail.getDeviceTimestamp();
        if (step < 0) {
            step = 0;
            Log.e("StreamedTargetTracker", "Negative difference in consecutive position timestamps");
        }
        // Difference between position adds
        long dt = System.currentTimeMillis() - updated;
        updated = System.currentTimeMillis();
        
        // Time difference between recorded diff and actual diff
        return dt - step;
    }
    
    public Position getHead() {
        return trackPositions.get(0);
    }
    
    public Position getTail() {
        return trackPositions.get(trackPositions.size()-1);
    }
    
    public long getRealtimeMillis() {
        return System.currentTimeMillis() - start;
    }

    private double extrapolatedRemoteDistance = 0;  // dead reckoning based on points from remote device
    private double extrapolatedLocalDistance = 0;  // smooth version used to control avatar
    private long tickTime = 0;  // time of this invocation
    private long lastTickTime = 0; // time of last invocation
    private long lastSampleTime = 0; // time of last remote point
    private static final long DISTANCE_CORRECTION_MILLISECONDS = 1500;   // time period over which to converge smooth local distance with remote distance
    
    /**
     * Extrapolates a smooth distance based on streamed positions from a remote device
     * Currently assumes all remote positions have timestamps < startOfTrackTime + time
     */
    @Override
    public double getCumulativeDistanceAtTime(long time) {
        
        // if we're just starting, set the local & remote distances to the last sample
        if (lastTickTime == 0) {
            extrapolatedRemoteDistance = super.getCumulativeDistanceAtTime(time); // distance at last sample
            extrapolatedLocalDistance = extrapolatedRemoteDistance;
            tickTime = getTail().getDeviceTimestamp();  // time of last sample
        }
        
        // lastTickTime = time of last invocation, tickTime = time of this invocation
        lastSampleTime = getTail().getDeviceTimestamp();
        lastTickTime = tickTime;
        tickTime = getHead().getDeviceTimestamp() + time;
        if (tickTime < lastSampleTime) Log.w("StreamedTargetTracker","Remote position arrived with future timestamp");
        long timeSinceLastSample = tickTime - lastSampleTime;
        long timeSinceLastInvocation = tickTime - lastTickTime;
        if (timeSinceLastSample > 5000) {
            timeSinceLastSample = 5000; // Lag limit, pauses extrapolation of remote distance
            timeSinceLastInvocation = 0; // Pauses extrapolation of local distance
        }
        
        // calculate the speed we need to move at to make our smooth local
        // distance converge with the non-continuous remote distance over
        // a period of DISTANCE_CORRECTION_MILLISECONDS
        // Will be identical to speed at last sample if this is the first invocation
        double correctiveSpeed = super.getCurrentSpeed(time)
                + (extrapolatedRemoteDistance - extrapolatedLocalDistance) * 1000.0
                / DISTANCE_CORRECTION_MILLISECONDS;
        
        // Simple dead reckoning for remote player for next invocation. Non-continuous when a new sample comes in.
        extrapolatedRemoteDistance = super.getCumulativeDistanceAtTime(time);
        if (super.hasFinished()) {
            extrapolatedRemoteDistance += (super.getCurrentSpeed(time) * timeSinceLastSample / 1000.0);
        }
        
        // extrapolate smooth, continuous internal distance for next invocation & return. Always continuous.
        extrapolatedLocalDistance += correctiveSpeed * timeSinceLastInvocation / 1000.0;
        return extrapolatedLocalDistance;
    }
    

    @Override
    public boolean hasFinished() {
        return finished;
    }
    
    public void finish() {
        finished = true;
    }
    
}
