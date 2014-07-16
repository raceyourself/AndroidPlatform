package com.raceyourself.raceyourself.game.position_controllers;

import com.raceyourself.platform.models.Position;
import com.raceyourself.platform.utils.Stopwatch;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by benlister on 26/06/2014.
 */
public class FixedVelocityPositionController extends PositionController {

    @Getter @Setter private float speed = 1.0f;
    private Stopwatch stopwatch = new Stopwatch();

    @Override
    public void start() {
        stopwatch.start();
    }

    @Override
    public void stop() {
        stopwatch.stop();
    }

    @Override
    public void reset() {
        stopwatch.reset();
    }

    @Override
    public void close() { stop(); }

    @Override
    public double getRealDistance() {
        return speed*stopwatch.elapsedTimeMillis()/1000.0;
        //TODO: cater for speed changes mid-game
    }

    @Override
    public long getElapsedTime() {
        return stopwatch.elapsedTimeMillis();
    }

    @Override
    public float getCurrentSpeed() {
        return speed;
    }

    @Override
    public float getAverageSpeed() {
        // TODO: cater for speed changes mid-game
        return speed;
    }

}
