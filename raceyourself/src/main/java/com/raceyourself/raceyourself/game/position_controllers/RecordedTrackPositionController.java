package com.raceyourself.raceyourself.game.position_controllers;

import android.util.Log;

import com.raceyourself.platform.models.Position;
import com.raceyourself.platform.models.Track;
import com.raceyourself.platform.utils.Stopwatch;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by benlister on 26/06/2014.
 */
@Slf4j
public class RecordedTrackPositionController extends PositionController {

    @Getter
    private Track track;
    private ArrayList<Position> trackPositions;
    private Stopwatch stopwatch = new Stopwatch();

    // Cache variables used for performance reasons
    private final long startTime; //the start time of the track in milliseconds from 1970
    private int currentElement = 0;
    private double distance = 0.0;

    public RecordedTrackPositionController(Track track) {
        this.track = track;
        this.trackPositions = new ArrayList<Position>(track.getTrackPositions());

        log.info("Track " + this.track.getId() + " selected as target.");
        log.debug("Track " + track.getId() + " has " + trackPositions.size() + " position elements.");
        if (trackPositions.isEmpty()) {
            startTime = 0;
            return;
        }

        startTime = trackPositions.get(0).getDeviceTimestamp();
        log.trace("Track start time: " + startTime);
        log.trace("Track end time: " + trackPositions.get(trackPositions.size()-1).getDeviceTimestamp());
    }

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
    public void close() {
        stop();
    }

    @Override
    public long getElapsedTime() {
        return stopwatch.elapsedTimeMillis();
    }

    @Override
    public float getAverageSpeed() {
        // TODO: cater for speed changes mid-game
        return getElapsedTime() == 0 ? 0 : (float)(1000.0f * getRealDistance() / getElapsedTime());
    }

    /**
     * Returns the speed of the player at the current point in the track
     *
     * @return speed in m/s
     */
    public float getCurrentSpeed() {
        if (trackPositions.isEmpty()) return 0;

        // otherwise we need to get the speed from the database
        // first, call the distance function to update currentElement
        getRealDistance();
        // then return the speed at the currentElement
        Position currentPosition = trackPositions.get(currentElement);
        if (currentPosition == null) {
            throw new RuntimeException("TargetTracker: CurrentSpeed - cannot find position in track.");
        } else {
//            log.trace("The current target pace is " + currentPosition.getSpeed() + "m/s.");
            return currentPosition.getSpeed();
        }

    }

    /**
     * Calculates travelled distance on track between start and time
     * NOTE: Updates internal state (distance += elapsed distance since last call)
     *
     * @return distance in meters
     */
    public double getRealDistance() {
        if (trackPositions.isEmpty()) return 0;

        // if using a previous track log, need to loop through its positions to find the one
        // with timestamp startTime + time
        Position currentPosition = trackPositions.get(currentElement);
        if (currentElement + 1 >= trackPositions.size()) return distance;  //check if we hit the end of the track
        Position nextPosition = trackPositions.get(currentElement + 1);

        // update to most recent position
        while (nextPosition != null && nextPosition.getDeviceTimestamp() - startTime <= stopwatch.elapsedTimeMillis() && currentElement + 1 < trackPositions.size()) {
            distance += Position.distanceBetween(currentPosition, nextPosition);
//            log.trace("The distance travelled by the target is " + distance + "m.");
            currentElement++;
            currentPosition = nextPosition;
            nextPosition = null;
        }

        //interpolate between most recent and upcoming (future) position
        double interpolation = 0.0;
        if (currentElement + 1 < trackPositions.size()) {
            nextPosition = trackPositions.get(currentElement + 1);
        }
        if (nextPosition != null) {
            long timeBetweenPositions = nextPosition.getDeviceTimestamp() - currentPosition.getDeviceTimestamp();
            if (timeBetweenPositions != 0) {
                float proportion = ((float)stopwatch.elapsedTimeMillis()-(currentPosition.getDeviceTimestamp()-startTime))/timeBetweenPositions;
                interpolation = Position.distanceBetween(currentPosition, nextPosition) * proportion;
//                log.trace("interp: " + interpolation + " " + proportion + " t: " + (currentPosition.getDeviceTimestamp()-startTime) + " " + time + " x: " + timeBetweenPositions + " " + ((float)time-(currentPosition.getDeviceTimestamp()-startTime)));
            }
        }

        // return up-to-the-millisecond distance
        // note the distance variable is just up to the most recent Position
        return distance + interpolation;
    }

    public double getExpectedDistanceAtTime(long elapsedMillis) {
        return getTrack().getDistanceAtTime(elapsedMillis);
    }

    /**
     * Previous track logs have a length, so will finish at some point. Use this method to find out
     * whether we've got to the end of the pre-recorded track.
     *
     * @return true if the target track has played all the way through, false otherwise
     */
    public boolean hasFinished() {
        return this.currentElement >= trackPositions.size() - 1;
    }

}
