package com.raceyourself.raceyourself.game.event_listeners;

import lombok.Getter;

/**
 * Created by benlister on 07/07/2014.
 */
public abstract class PlayerDistanceListener {

    @Getter private double firstTriggerDistance = Double.MIN_VALUE;
    @Getter private double recurrenceInterval = -1.0;

    /**
     * Called at a given elapsed time
     * @param requestedDistance the time, in milliseconds, at which the callback was registered to trigger
     * @param actualDistance the elapsed time, in milliseconds, at which the callback was actually triggered (there may be a small difference between this and requestedElapsedTime). Usually ignored.
     */
    public abstract void onDistance(double requestedDistance, double actualDistance);

    public PlayerDistanceListener setFirstTriggerDistance(double firstTriggerDistance) {
        this.firstTriggerDistance = firstTriggerDistance;
        return this;
    }

    public PlayerDistanceListener setRecurrenceInterval(double recurrenceInterval) {
        this.recurrenceInterval = recurrenceInterval;
        return this;
    }

}
