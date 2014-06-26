package com.raceyourself.raceyourself.game.position_controllers;

import android.content.Context;

import com.raceyourself.platform.gpstracker.GPSTracker;
import com.raceyourself.platform.models.Position;

/**
 * Created by benlister on 26/06/2014.
 */
public class OutdoorPositionController extends PositionController {

    private GPSTracker gpsTracker;

    public OutdoorPositionController(Context ctx) {
        gpsTracker = new GPSTracker(ctx);
    }

    @Override
    public void start() {
        gpsTracker.startTracking();
    }

    @Override
    public void stop() {
        gpsTracker.stopTracking();
    }

    @Override
    public void reset() {
        if (gpsTracker.isTracking()) gpsTracker.stopTracking();
        gpsTracker.startNewTrack();
    }

    @Override
    public double getRealDistance() {
        return gpsTracker.getElapsedDistance();
    }
}
