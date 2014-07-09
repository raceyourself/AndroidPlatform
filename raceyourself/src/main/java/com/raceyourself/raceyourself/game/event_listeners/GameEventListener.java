package com.raceyourself.raceyourself.game.event_listeners;

/**
 * Created by benlister on 07/07/2014.
 */
public interface GameEventListener {

    /**
     * Called at a given elapsed time
     * @param eventTag set when the listener is registered, and returned when the event is triggered to allow the calling class to identify the event
     */
    public void onGameEvent(String eventTag);

}
