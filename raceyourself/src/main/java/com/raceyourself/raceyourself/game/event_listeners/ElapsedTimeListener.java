package com.raceyourself.raceyourself.game.event_listeners;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by benlister on 07/07/2014.
 */
public abstract class ElapsedTimeListener {

    @Getter private long firstTriggerTime = Long.MIN_VALUE;
    @Getter private long recurrenceInterval = -1L;

    /**
     * Called at a given elapsed time
     * @param requestedElapsedTime the time, in milliseconds, at which the callback was registered to trigger
     * @param actualElapsedTime the elapsed time, in milliseconds, at which the callback was actually triggered (there may be a small difference between this and requestedElapsedTime). Usually ignored.
     */
    public abstract void onElapsedTime(long requestedElapsedTime, long actualElapsedTime);

    public ElapsedTimeListener setFirstTriggerTime(long firstTriggerTime) {
        this.firstTriggerTime = firstTriggerTime;
        return this;
    }

    public ElapsedTimeListener setRecurrenceInterval(long recurrenceInterval) {
        this.recurrenceInterval = recurrenceInterval;
        return this;
    }

}
