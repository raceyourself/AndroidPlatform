package com.raceyourself.raceyourself.game.event_listeners;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by benlister on 07/07/2014.
 */
public abstract class RegularUpdateListener {

    @Getter private long recurrenceInterval = -1L;
    @Getter @Setter private long lastTriggerTime = Long.MIN_VALUE;

    /**
     * Called at a regular interval
     */
    public abstract void onRegularUpdate();

    public RegularUpdateListener setRecurrenceInterval(long recurrenceInterval) {
        this.recurrenceInterval = recurrenceInterval;
        return this;
    }

}
