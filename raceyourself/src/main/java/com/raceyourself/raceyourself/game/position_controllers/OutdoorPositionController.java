package com.raceyourself.raceyourself.game.position_controllers;

import android.content.Context;

import com.raceyourself.platform.gpstracker.GPSTracker;
import com.raceyourself.platform.models.Device;
import com.raceyourself.platform.models.Position;
import com.raceyourself.platform.models.UserDetail;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by benlister on 26/06/2014.
 */
@Slf4j
public class OutdoorPositionController extends PositionController {

    private GPSTracker gpsTracker;

    @Getter
    @Setter
    private boolean localPlayer = true;

    public OutdoorPositionController(Context ctx) {

        // ensure we have a registered device and user before playing with a GpsTracker
        // TODO: do this in a non-blocking way
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() < startTime + 5000) {
            UserDetail ud = UserDetail.get();
            if (ud != null && ud.getApiAccessToken() != null) {
                Device d = Device.self();
                if (d != null) {
                    // all is good, break out of loop
                    break;
                }
            } else {
                // wait
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    // nothing, continue looping
                }
            }
        }

        gpsTracker = new GPSTracker(ctx);
        gpsTracker.setIndoorMode(false);
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

    @Override
    public long getElapsedTime() { return gpsTracker.getElapsedTime(); }

    @Override
    public float getCurrentSpeed() { return gpsTracker.getCurrentSpeed(); }

    @Override
    public float getAverageSpeed() { return (float) (1000.0 * gpsTracker.getElapsedDistance() / gpsTracker.getElapsedTime()); }
}
