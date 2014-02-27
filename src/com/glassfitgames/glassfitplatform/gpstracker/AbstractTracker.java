package com.glassfitgames.glassfitplatform.gpstracker;

import android.content.Context;

import com.glassfitgames.glassfitplatform.models.Position;

public abstract class AbstractTracker {

    public abstract void init(Context ctx);
    
    public abstract void setIndoorMode(boolean indoor);
    public abstract boolean isIndoorMode();
    
    public abstract boolean hasPosition();
    
    public abstract void startTracking();
    public abstract void stopTracking();
    public abstract boolean isTracking();
    public abstract void startNewTrack();

    public abstract Position getCurrentPosition();
    public abstract float getCurrentSpeed();
    public abstract float getSampleSpeed();
    public abstract float getCurrentBearing();
    public abstract double getElapsedDistance();
    public abstract double getSampleDistance();
    public abstract long getElapsedTime();  
    
}
