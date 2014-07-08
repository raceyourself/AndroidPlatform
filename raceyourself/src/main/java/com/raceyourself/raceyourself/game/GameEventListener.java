package com.raceyourself.raceyourself.game;

/**
 * Created by benlister on 07/07/2014.
 */
public interface GameEventListener {

    /**
     * Called at a given elapsed time
     * @param eventTag set when the listener is registered, and returned when the event is triggered to allow the calling class to identify the event
     * @param requestedElapsedTime the time, in milliseconds, at which the callback was registered to trigger
     * @param actualElapsedTime the elapsed time, in milliseconds, at which the callback was actually triggered (there may be a small difference between this and requestedElapsedTime). Usually ignored.
     */
    public void onGameEvent(String eventTag, long requestedElapsedTime, long actualElapsedTime);

}
