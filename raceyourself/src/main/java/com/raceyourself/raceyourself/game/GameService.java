package com.raceyourself.raceyourself.game;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.raceyourself.platform.utils.Stopwatch;
import com.raceyourself.raceyourself.game.position_controllers.PositionController;

import java.util.List;

import lombok.Getter;
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
    @Getter private List<PositionController> positionControllers;
    private Stopwatch stopwatch = new Stopwatch();
    @Getter private GameConfiguration gameConfiguration;

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
            log.warn("re-initializing Game Service, throwing away existing game data");
            //stop?
        }
        this.positionControllers = positionControllers;
        this.gameConfiguration = gameConfiguration;
        this.initialized = true;
        this.reset();
    }

    // start the game
    public void start() {
        if (!initialized) throw new RuntimeException("GameService must be initialized before use");
        for (PositionController p : positionControllers) {
            p.start();
        }
        stopwatch.start();
    }

    // stop/pause the game. Use start() to restart or reset() to go back to the beginning
    public void stop() {
        if (!initialized) throw new RuntimeException("GameService must be initialized before use");
        for (PositionController p : positionControllers) {
            p.stop();
        }
        stopwatch.stop();
    }

    // reset the game to the beginning
    public void reset() {
        if (!initialized) throw new RuntimeException("GameService must be initialized before use");
        for (PositionController p : positionControllers) {
            p.reset();
        }
        stopwatch.reset(-gameConfiguration.getCountdown());
    }

    public long getElapsedTime() {
        if (!initialized) throw new RuntimeException("GameService must be initialized before use");
        return stopwatch.elapsedTimeMillis();
    }

    // TODO: remove this
    public long getRemainingTimeX() {
        if (!initialized) throw new RuntimeException("GameService must be initialized before use");
        if (gameConfiguration.getGameType() == GameConfiguration.GameType.TIME_CHALLENGE) {
            return gameConfiguration.getTargetTime() - stopwatch.elapsedTimeMillis();
        } else {
            throw new RuntimeException("Remaining time is not a valid concept unless GameType is TIME_CHALLENGE");
        }
    }

    public GameState getGameState() {
        if (!initialized) throw new RuntimeException("GameService must be initialized before use");
        if (stopwatch.elapsedTimeMillis() <= 0) {
            return GameState.PRE_START;
        } else if (getRemainingTimeX() <= 0) {
            stop();
            return GameState.FINISHED;
        } else if (stopwatch.isRunning()) {
            return GameState.IN_PROGRESS;
        } else {
            return GameState.PAUSED;
        }
    }

    public enum GameState {
        PRE_START,
        IN_PROGRESS,
        PAUSED,
        FINISHED
    }

}
