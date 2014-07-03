package com.raceyourself.raceyourself.game.position_controllers;

import com.raceyourself.platform.models.Track;

import lombok.Getter;

/**
 * Created by Amerigo on 03/07/2014.
 */
public class TrackPositionController extends PositionController {
    @Getter
    private Track opponentTrack;

    public TrackPositionController(Track track) {
        opponentTrack = track;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void reset() {

    }

    @Override
    public double getRealDistance() {
        return 0;
    }

    @Override
    public long getElapsedTime() {
        return 0;
    }

    @Override
    public float getCurrentSpeed() {
        return 0;
    }

    @Override
    public float getAverageSpeed() {
        return 0;
    }
}
