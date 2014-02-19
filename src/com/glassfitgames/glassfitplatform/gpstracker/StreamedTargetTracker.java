package com.glassfitgames.glassfitplatform.gpstracker;

import java.util.ArrayList;

import android.util.Log;

import com.glassfitgames.glassfitplatform.models.Position;

public class StreamedTargetTracker extends TrackTargetTracker {
    private boolean finished = false;

    public StreamedTargetTracker(Position startPosition) {
        super(fromPosition(startPosition));
    }
    
    private static ArrayList<Position> fromPosition(Position position) {
        Log.i("StreamedTargetTracker", "Tracker created from position: " + position.toCsv());
        ArrayList<Position> positions = new ArrayList<Position>();        
        positions.add(position);
        return positions;
    }
    
    public void addPosition(Position position) {
        Log.i("StreamedTargetTracker", "Position added: " + position.toCsv());
        trackPositions.add(position);
    }

    @Override
    public boolean hasFinished() {
        return finished;
    }
    
    public void finish() {
        finished = true;
    }
    
}
