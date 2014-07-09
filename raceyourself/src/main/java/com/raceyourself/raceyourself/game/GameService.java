package com.raceyourself.raceyourself.game;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.raceyourself.platform.utils.Stopwatch;
import com.raceyourself.raceyourself.game.event_listeners.ElapsedTimeListener;
import com.raceyourself.raceyourself.game.event_listeners.GameEventListener;
import com.raceyourself.raceyourself.game.event_listeners.RegularUpdateListener;
import com.raceyourself.raceyourself.game.position_controllers.PositionController;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Responsible for keeping the game running in the background when the user switches out of the app
 * or turns the screen off.
 * Can be seen as a Game Controller, orchestrating:
 * - start/stop/reset of all players
 * - maintaining current state of the game
 * - running a timer to measure progress of the game
 * - voice feedback
 */
@Slf4j
public class GameService extends Service {

    private final IBinder gameServiceBinder = new GameServiceBinder();

    @Getter private boolean initialized = false;
    @Getter private GameState gameState = GameState.PAUSED;
    @Getter private GameState positionTrackerState = GameState.PAUSED;
    @Getter private List<PositionController> positionControllers;
    @Getter private PositionController localPositionController;  // shortcut to local player's position controller in the list
    private Stopwatch stopwatch = new Stopwatch();
    @Getter private GameConfiguration gameConfiguration;
    private List<GameEventListener> gameEventListeners = new CopyOnWriteArrayList<GameEventListener>();
    private List<ElapsedTimeListener> elapsedTimeListeners = new CopyOnWriteArrayList<ElapsedTimeListener>();
    private List<RegularUpdateListener> regularUpdateListeners = new CopyOnWriteArrayList<RegularUpdateListener>();

    // timer and task to regularly refresh UI
    private Timer timer = new Timer();
    private GameMonitorTask task;

    public GameService() {

    }

    public class GameServiceBinder extends Binder {
        public GameService getService() {
            return GameService.this;
        }
    }

    /**
     * Called by the system when one of our app's activities binds to the service.
     * Returns a binder that can be used to communicate with the services methods.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return gameServiceBinder;
    }

    /**
     * Called by the system when the service is explicitly started by our app
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (initialized) stop();
    }

    /**
     * Initialise the service, typically after binding but before use.
      */
    public void initialize(List<PositionController> positionControllers, GameConfiguration gameConfiguration) {
        if (initialized) {
            log.warn("Re-initializing Game Service, throwing away existing game data");
            stop();
        }
        this.positionControllers = positionControllers;
        for (PositionController p : positionControllers) {
            p.reset();
            if (p.isLocalPlayer()) { localPositionController = p; }  // save shortcut to local one
        }
        this.gameConfiguration = gameConfiguration;
        stopwatch.reset(-gameConfiguration.getCountdown());
        this.initialized = true;

        // start monitoring state - first call to run starts the positionTrackers, stopwatch etc
        log.debug("Starting game monitor loop");
        if (task != null) task.cancel();
        task = new GameMonitorTask();
        timer.scheduleAtFixedRate(task, 0, 50);  // pretty quick loops, need to be short enough that humans don't notice
    }

    /**
     * Register a callback to be triggered at key game events
     */
    public void registerGameEventListener(GameEventListener gameEventListener) {
        gameEventListeners.add(gameEventListener);
    }

    public void unregisterGameEventListener(GameEventListener gameEventListener) {
        gameEventListeners.remove(gameEventListener);
    }

    /**
     * Register a callback to be triggered at firstTriggerTime milliseconds elapsed time.
     */
    public void registerElapsedTimeListener(ElapsedTimeListener elapsedTimeListener) {
        elapsedTimeListeners.add(elapsedTimeListener);
    }

    public void unregisterElapsedTimeListener(ElapsedTimeListener elapsedTimeListener) {
        elapsedTimeListeners.remove(elapsedTimeListener);
    }

    /**
     * Register a callback to be triggered at regular intervals throughout the lifetime of the service
     */
    public void registerRegularUpdateListener(RegularUpdateListener regularUpdateListener) {
        regularUpdateListeners.add(regularUpdateListener);
    }

    public void unregisterRegularUpdateListener(RegularUpdateListener regularUpdateListener) {
        regularUpdateListeners.remove(regularUpdateListener);
    }


    // start the game
    public void start() {
        if (!initialized) throw new RuntimeException("GameService must be initialized before use");

        // start the game
        this.gameState = GameState.IN_PROGRESS;
    }

    // stop/pause the game. Use start() to restart or reset() to go back to the beginning
    public void stop() {
        if (!initialized) throw new RuntimeException("GameService must be initialized before use");

        // stop the game
        this.gameState = GameState.PAUSED;

        // stop monitoring state, pause everything & cancel task on last call to run()
        if (task != null) {
            task.run();
        }
    }

    private void finish() {
        //stop();
        // TODO: save track, register as challenge attempt
    }

    public long getElapsedTime() {
        if (!initialized) throw new RuntimeException("GameService must be initialized before use");
        return stopwatch.elapsedTimeMillis();
    }

    public enum GameState {
        IN_PROGRESS,
        PAUSED
    }

    long lastLoopElapsedTime = Long.MIN_VALUE;
    private class GameMonitorTask extends TimerTask {
        public void run() {

            log.trace("GameMonitor run() called");

            // can't do much if we don't know what kind of game we have
            if (gameConfiguration == null) return;

            // check progress and finish game if appropriate
            // TODO: move into isFinished() method of positionController
            switch (gameConfiguration.getGameType()) {
                case DISTANCE_CHALLENGE: {
                    if (localPositionController.getRealDistance() >= gameConfiguration.getTargetDistance()) {
                        log.info("Game finished, pausing");
                        gameState = GameState.PAUSED;
                        for (GameEventListener gel : gameEventListeners) {
                            gel.onGameEvent("Finished");
                        }
                    }
                }
                case TIME_CHALLENGE: {
                    if (getElapsedTime() >= gameConfiguration.getTargetTime()) {
                        log.info("Game finished, pausing");
                        gameState = GameState.PAUSED;
                        for (GameEventListener gel : gameEventListeners) {
                            gel.onGameEvent("Finished");
                        }
                    }
                }
            }

            // start/stop stopwatch if necessary
            if (gameState == GameState.IN_PROGRESS && !stopwatch.isRunning()) {
                log.info("Starting stopwatch");
                stopwatch.start();
            } else if (gameState == GameState.PAUSED && stopwatch.isRunning()) {
                log.info("Stopping stopwatch");
                stopwatch.stop();
            }

            // start/stop position controllers (only when stopwatch is positive, i.e. not during countdown)
            if (gameState == GameState.IN_PROGRESS && stopwatch.elapsedTimeMillis() > 0 && positionTrackerState == GameState.PAUSED) {
                log.info("Starting position controllers");
                for (PositionController p : positionControllers) { p.start(); }
                positionTrackerState = GameState.IN_PROGRESS;
            } else if (gameState == GameState.PAUSED && positionTrackerState == GameState.IN_PROGRESS) {
                log.info("Stopping position controllers");
                for (PositionController p : positionControllers) { p.stop(); }
                positionTrackerState = GameState.PAUSED;
            }

            // TODO: generate voice feedback / motivational messages

            // fire elapsed time listeners
            long thisLoopElapsedTime = stopwatch.elapsedTimeMillis();
            if (thisLoopElapsedTime > lastLoopElapsedTime) {
                log.trace("Checking for elapsed time listeners to fire");
                for (ElapsedTimeListener etl : elapsedTimeListeners) {
                    if (etl.getFirstTriggerTime() >= lastLoopElapsedTime && etl.getFirstTriggerTime() < thisLoopElapsedTime) {
                        // fire the event
                        etl.onElapsedTime(etl.getFirstTriggerTime(), thisLoopElapsedTime);
                        // update next fire time if it's a recurring event
                        if (etl.getRecurrenceInterval() > 0) {
                            etl.setFirstTriggerTime(etl.getFirstTriggerTime() + etl.getRecurrenceInterval());
                        }
                    }
                }
                lastLoopElapsedTime = thisLoopElapsedTime;
            }

            // fire regular update listeners
            long thisLoopSystemTime = System.currentTimeMillis();
            log.trace("Checking for regular update listeners to fire");
            for (RegularUpdateListener rel : regularUpdateListeners) {
                if (thisLoopSystemTime >= rel.getLastTriggerTime() + rel.getRecurrenceInterval()) {
                    rel.onRegularUpdate();  // fire the event
                    rel.setLastTriggerTime(thisLoopSystemTime);  // save the time it fired
                }
            }

            // stop the task running if we've paused
//            if (gameState == GameState.PAUSED) {
//                this.cancel();
//            }
        }
    }

    private class GameEventListenerWrapper {
        @Getter @Setter private long firstTriggerTime;
        @Getter private long recurrenceInterval;
        @Getter private GameEventListener gameEventListener;
        @Getter private String tag;

        public GameEventListenerWrapper(long firstTriggerTime, long recurrenceInterval, String tag, GameEventListener l) {
            this.firstTriggerTime = firstTriggerTime;
            this.recurrenceInterval = recurrenceInterval;
            this.tag = tag;
            this.gameEventListener = l;
        }
    }

}
