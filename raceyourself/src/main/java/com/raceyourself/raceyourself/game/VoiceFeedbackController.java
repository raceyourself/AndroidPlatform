package com.raceyourself.raceyourself.game;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.SparseArray;

import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.game.event_listeners.ElapsedTimeListener;
import com.raceyourself.raceyourself.game.event_listeners.GameEventListener;
import com.raceyourself.raceyourself.game.position_controllers.PositionController;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by benlister on 07/07/2014.
 */
@Slf4j
public class VoiceFeedbackController {

    private GameService gameService;
    private PositionController player;
    private PositionController opponent;
    private SoundPool soundpool = new SoundPool(2, AudioManager.STREAM_NOTIFICATION, 0);
    private SparseArray<Integer> loadedSounds = new SparseArray<Integer>();  // resourceId -> soundpoolSoundId

    public VoiceFeedbackController(Context context) {

        // load all the sounds in the res/raw directory
       /* Field[] fields=R.raw.class.getFields();
        for(int i=0; i < fields.length; i++){
            try {
                int resourceId = fields[i].getInt(R.raw);
                log.trace("Loading voice feedback resource " + fields[i].getName());
                AssetFileDescriptor afd = context.getResources().openRawResourceFd(resourceId);
                loadedSounds.put(resourceId, soundpool.load(afd, 1));
            } catch (IllegalAccessException e) {
                log.warn("Skipping load of " + fields[i].getName());
            }
        }
*/
        soundpool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                log.debug("Playing sound ID (onLoadComplete) " + sampleId);
                soundpool.play(sampleId, 1.0f, 1.0f, 0, 0, 1.0f);
            }
        });

    }

    private boolean initialised = false;
    public synchronized void setGameService(GameService gameService) {

        this.gameService = gameService;

        if (gameService != null && !initialised) {

            initialised = true;  // no need to re-register if we unbind/rebind to the service

            // register listeners - just the first time we see the service
            log.debug("Registering voice feedback events with Game Service");

            gameService.registerElapsedTimeListener(new ElapsedTimeListener() {
                @Override
                public void onElapsedTime(long requestedElapsedTime, long actualElapsedTime) {
                    log.debug("Three callback");
                    playNumber(3);
                }
            }.setFirstTriggerTime(-3000));

            gameService.registerElapsedTimeListener(new ElapsedTimeListener() {
                @Override
                public void onElapsedTime(long requestedElapsedTime, long actualElapsedTime) {
                    log.debug("Two callback");
                    playNumber(2);
                }
            }.setFirstTriggerTime(-2000));

            gameService.registerElapsedTimeListener(new ElapsedTimeListener() {
                @Override
                public void onElapsedTime(long requestedElapsedTime, long actualElapsedTime) {
                    log.debug("One callback");
                    playNumber(1);
                }
            }.setFirstTriggerTime(-1000));

            gameService.registerElapsedTimeListener(new ElapsedTimeListener() {
                @Override
                public void onElapsedTime(long requestedElapsedTime, long actualElapsedTime) {
                    log.debug("Go callback");
                    play(R.raw.go);
                }
            }.setFirstTriggerTime(0));

            gameService.registerElapsedTimeListener(new ElapsedTimeListener() {
                @Override
                public void onElapsedTime(long requestedElapsedTime, long actualElapsedTime) {
                    log.debug("15-sec callback");
                    sayPaceDelta();
                }
            }.setFirstTriggerTime(15000));

            gameService.registerElapsedTimeListener(new ElapsedTimeListener() {
                @Override
                public void onElapsedTime(long requestedElapsedTime, long actualElapsedTime) {
                    log.debug("30-sec callback");
                    sayDistanceDelta();
                }
            }.setFirstTriggerTime(30000).setRecurrenceInterval(30000));

            if (gameService.getGameConfiguration().getGameType() == GameConfiguration.GameType.TIME_CHALLENGE) {
                gameService.registerElapsedTimeListener(new ElapsedTimeListener() {
                    @Override
                    public void onElapsedTime(long requestedElapsedTime, long actualElapsedTime) {
                        log.debug("30-sec to go callback");
                        play(R.raw.the_finish_line_is_in_sight);
                    }
                }.setFirstTriggerTime(gameService.getGameConfiguration().getTargetTime() - 30000));
            }

            if (gameService.getGameConfiguration().getGameType() == GameConfiguration.GameType.TIME_CHALLENGE) {
                gameService.registerGameEventListener(new GameEventListener() {
                    @Override
                    public void onGameEvent(String event) {
                        if (event.equals("Finished")) {
                            log.debug("Finish callback");
                            synchronized (VoiceFeedbackController.this) {
                                if (player.getRealDistance() > opponent.getRealDistance()) {
                                    play(R.raw.you_have_won);
                                } else {
                                    play(R.raw.better_luck_next_time);
                                }
                            }
                        }
                    }
                });
            }
        }
    }

    private final float SIMILAR_DISTANCE_THRESHOLD = 5;  // m ... may need to use % too
    public synchronized void sayDistanceDelta() {
        if (!isReady()) return;  // don't play (or crash) if not ready

        if (player.getRealDistance() > opponent.getRealDistance() + SIMILAR_DISTANCE_THRESHOLD)
            play(R.raw.looking_good);
        else if (player.getRealDistance() < opponent.getRealDistance() - SIMILAR_DISTANCE_THRESHOLD)
            play(R.raw.pick_up_the_pace_a_little);
        else
            play(R.raw.keep_pushing);
    }

    private final float SIMILAR_SPEED_THRESHOLD = 0.1f;  // m/s
    public synchronized void sayPaceDelta() {
        if (!isReady()) return;  // don't play (or crash) if not ready

        if (player.getCurrentSpeed() > opponent.getCurrentSpeed() + SIMILAR_SPEED_THRESHOLD)
            play(R.raw.this_is_a_winning_pace);
        else if (player.getCurrentSpeed() < opponent.getCurrentSpeed() - SIMILAR_SPEED_THRESHOLD)
            play(R.raw.you_are_off_the_winning_pace);
        else
            play(R.raw.pick_up_the_pace_a_little);
    }

    private void sayOutlook() {
        // play something useful depending on track
    }

    public synchronized void play(int resourceId) {
        log.trace("Play called");
        if (!isReady() || gameService.getGameState() != GameService.GameState.IN_PROGRESS) return;  // don't play if not ready, or if game is paused

        // if we've not seen the sound before, load it into the sound pool
        if (loadedSounds.get(resourceId) == null) {
            // TODO: check resource is a sound
            log.debug("Loading sound ID " + resourceId);
            int soundIndex = soundpool.load(gameService.getResources().openRawResourceFd(resourceId), 1);
            // sound will be played on loadComplete callback
            loadedSounds.put(resourceId, soundIndex);
            return;
        }

        // play the sound
        // TODO: queue the sound if something is already playing
        log.debug("Playing sound ID " + resourceId);
        soundpool.play(loadedSounds.get(resourceId), 1.0f, 1.0f, 0, 0, 1.0f);
    }

    public void playNumber(int number) {
        switch (number) {
            case 0:
                break;
            case 1:
                play(R.raw._1);
                break;
            case 2:
                play(R.raw._2);
                break;
            case 3:
                play(R.raw._3);
                break;
        }
    }

    private synchronized boolean isReady() {
        if (gameService == null) {
            log.warn("Not playing feedback - game service not available");
            return false;
        }
        if (player == null || opponent == null) {
            log.debug("Retrieving player and opponent from game service");
            for (PositionController pc : gameService.getPositionControllers()) {
                if (pc.isLocalPlayer()) {
                    log.debug("Found player");
                    player = pc;
                } else {
                    log.debug("Found opponent");
                    opponent = pc; // just the last one we find, for now
                }
            }
        }
        if (player == null) {
            log.warn("Not playing feedback - no local player found");
            return false;
        }
        if (opponent == null) {
            log.warn("Not playing feedback - no opponent found");
            return false;
        }
        return true;
    }
}
