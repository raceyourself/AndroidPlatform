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

    
    @Override
    public double getCumulativeDistanceAtTime(long time) {
        double distance = super.getCumulativeDistanceAtTime(time);
        if (super.hasFinished()) {
            // Simple dead reckoning
            Position tail = getTail();
            long recordedTime = tail.getDeviceTimestamp() - getHead().getDeviceTimestamp();
            long interp = time - recordedTime;
            if (interp > 5000) interp = 5000; // Lag limit
            return distance + (tail.getSpeed() * interp / 1000.0);
        }
        return distance;
    }

    @Override
    public boolean hasFinished() {
        return finished;
    }
    
    public void finish() {
        finished = true;
    }
    
}
