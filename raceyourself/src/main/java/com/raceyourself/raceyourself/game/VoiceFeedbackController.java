package com.raceyourself.raceyourself.game;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.SoundPool;

import com.raceyourself.platform.utils.UnitConversion;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.game.event_listeners.ElapsedTimeListener;
import com.raceyourself.raceyourself.game.event_listeners.GameEventListener;
import com.raceyourself.raceyourself.game.event_listeners.PlayerDistanceListener;
import com.raceyourself.raceyourself.game.position_controllers.PositionController;
import com.raceyourself.raceyourself.game.position_controllers.RecordedTrackPositionController;

import java.util.LinkedList;
import java.util.Queue;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by benlister on 07/07/2014.
 */
@Slf4j
public class VoiceFeedbackController {

    private Context context;
    private GameService gameService;
    private PositionController player;
    private PositionController opponent;
    private MediaPlayer mediaPlayer = new MediaPlayer();
    //private SoundPool soundpool = new SoundPool(2, AudioManager.STREAM_NOTIFICATION, 0);
    //private SparseArray<Integer> loadedSounds = new SparseArray<Integer>();  // resourceId -> soundpoolSoundId

    public VoiceFeedbackController(Context context) {
        this.context = context;
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
            }.setFirstTriggerTime(-2999));

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
                    sayDistanceDelta();
                    sayOutlook();
                }
            }.setFirstTriggerTime(15000));

            gameService.registerElapsedTimeListener(new ElapsedTimeListener() {
                @Override
                public void onElapsedTime(long requestedElapsedTime, long actualElapsedTime) {
                    log.debug("30-sec callback");
                    sayDistanceDelta();
                    sayOutlook();
                }
            }.setFirstTriggerTime(30000).setRecurrenceInterval(30000));

            gameService.registerPlayerDistanceListener(new PlayerDistanceListener() {
                @Override
                public void onDistance(double requestedDistance, double actualDistance) {
                    log.debug("mile callback");
                    sayDistance();
                }
            }.setFirstTriggerDistance(1609.344).setRecurrenceInterval(1609.344));  // every mile

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

    public synchronized void sayDistance() {
        if (!isReady()) return;  // don't play (or crash) if not ready

        double distance = player.getRealDistance();
        if (distance < 1609.344) {
            playNumber((int)distance);
            play(R.raw.metres);
        } else {
            playNumber((int)UnitConversion.miles(distance));
            play(R.raw.miles);  // TODO: record singular
        }
    }

    private final float SIMILAR_DISTANCE_THRESHOLD = 5;  // m ... may need to use % too
    private final float LARGE_UNIT_THRESHOLD = 1.0f;  //mi
    public synchronized void sayDistanceDelta() {
        if (!isReady()) return;  // don't play (or crash) if not ready

        double delta = player.getRealDistance() - opponent.getRealDistance();
        if (Math.abs(delta) < 1600.0) {
            playNumber((int)Math.abs(delta));
            play(R.raw.metres); // TODO: record singular
        } else {
            playNumber((int)UnitConversion.miles(Math.abs(delta)));
            play(R.raw.miles);  // TODO: record singular
        }
        play(delta > 0 ? R.raw.ahead3 : R.raw.behind3);
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

    public void sayOutlook() {
        if (!isReady()) return;
        // work out if player will be ahead or behind at the finish
        // calculated by extrapolating current speed
        long targetTime = gameService.getGameConfiguration().getTargetTime();
        double playerOutlook = player.getExpectedDistanceAtTime(targetTime);
        double opponentOutlook = opponent.getExpectedDistanceAtTime(targetTime);
        log.debug("Estimated player distance at finish = " + playerOutlook + "m, opponent = " + opponentOutlook + "m");

        if (playerOutlook > opponentOutlook) {
            play(R.raw.this_is_a_winning_pace);
        } else {
            // nothing, don't want to say they are losing
        }
     }


    private Queue<MediaPlayer> playQueue = new LinkedList<MediaPlayer>();
    public synchronized void play(int resourceId) {
        log.trace("Play called");
        if (!isReady() || gameService.getGameState() != GameService.GameState.IN_PROGRESS) return;  // don't play if not ready, or if game is paused

        // load the sound from disk and add to the queue (loading takes a moment, so better to do it up front)
        log.debug("Adding sound ID " + resourceId + " to the play queue");
        MediaPlayer mp = MediaPlayer.create(context, resourceId);
        //SubtitleController controller = new SubtitleController(context, mp.getMediaTimeProvider(), mp);  //kitkat only
        //mp.setSubtitleAnchor(controller, this);
        playQueue.add(mp);
        play();
    }

    private boolean playing = false;
    private void play() {
        if (playing) return;  // play() will be called when the prev sound completes
        playing = true;
        MediaPlayer p = playQueue.remove();
        p.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                playing = false;
                if (playQueue.size() > 0) {
                    play();
                }
                mediaPlayer.reset();
                mediaPlayer.release();
            }
        });
        p.start();
    }

    public void playNumber(int number) {

        // hundreds
        switch (number / 100) {
            case 0:
                break;
            case 1:
                play(R.raw._100);
                break;
            case 2:
                play(R.raw._200);
                break;
            case 3:
                play(R.raw._300);
                break;
            case 4:
                play(R.raw._400);
                break;
            case 5:
                play(R.raw._500);
                break;
        }
        if (number/100 > 0 && number % 100 > 0) {
            play(R.raw.and);
        }

        // tens
        switch ((number % 100) / 10) {
            case 0:
                break;  // 0-9
            case 1:
                break;  // 10-19
            case 2:
                play(R.raw._20);
                break;
            case 3:
                play(R.raw._30);
                break;
            case 4:
                play(R.raw._40);
                break;
            case 5:
                play(R.raw._50);
                break;
            case 6:
                play(R.raw._60);
                break;
            case 7:
                play(R.raw._70);
                break;
            case 8:
                play(R.raw._80);
                break;
            case 9:
                play(R.raw._90);
                break;
        }

        // units
        int remainder = number % 100;
        if (remainder > 19 || remainder < 10) {
            switch (number % 10) {
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
                case 4:
                    play(R.raw._4);
                    break;
                case 5:
                    play(R.raw._5);
                    break;
                case 6:
                    play(R.raw._6);
                    break;
                case 7:
                    play(R.raw._7);
                    break;
                case 8:
                    play(R.raw._8);
                    break;
                case 9:
                    play(R.raw._9);
                    break;
            }
        } else {  // must be a teen
            switch (number % 10) {
                case 0:
                    break;
                case 1:
                    play(R.raw._11);
                    break;
                case 2:
                    play(R.raw._12);
                    break;
                case 3:
                    play(R.raw._13);
                    break;
                case 4:
                    play(R.raw._14);
                    break;
                case 5:
                    play(R.raw._15);
                    break;
                case 6:
                    play(R.raw._16);
                    break;
                case 7:
                    play(R.raw._17);
                    break;
                case 8:
                    play(R.raw._18);
                    break;
                case 9:
                    play(R.raw._19);
                    break;
            }
        }
    }

    private synchronized boolean isReady() {

        if (gameService == null || !gameService.isInitialized()) {
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
