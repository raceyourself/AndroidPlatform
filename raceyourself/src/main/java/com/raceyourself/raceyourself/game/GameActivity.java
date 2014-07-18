package com.raceyourself.raceyourself.game;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.raceyourself.platform.models.Challenge;
import com.raceyourself.platform.models.MatchedTrack;
import com.raceyourself.platform.models.Track;
import com.raceyourself.platform.utils.Format;
import com.raceyourself.platform.utils.UnitConversion;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.BaseFragmentActivity;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.game.event_listeners.GameEventListener;
import com.raceyourself.raceyourself.game.event_listeners.RegularUpdateListener;
import com.raceyourself.raceyourself.game.position_controllers.FixedVelocityPositionController;
import com.raceyourself.raceyourself.game.position_controllers.OutdoorPositionController;
import com.raceyourself.raceyourself.game.position_controllers.PositionController;
import com.raceyourself.raceyourself.game.position_controllers.RecordedTrackPositionController;
import com.raceyourself.raceyourself.home.feed.ChallengeDetailBean;
import com.raceyourself.raceyourself.home.ChallengeSummaryActivity;
import com.squareup.picasso.Picasso;
import com.viewpagerindicator.CirclePageIndicator;
import com.raceyourself.raceyourself.home.feed.TrackSummaryBean;


import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GameActivity extends BaseFragmentActivity {

    private GameService gameService;
    private ServiceConnection gameServiceConnection;

    private List<PositionController> positionControllers = new ArrayList<PositionController>();
//    private GameConfiguration gameConfiguration;
    private int positionAccuracy = 1; // 1=gps_disabled, 2=no_fix, 3=bad_fix, 4=good_fix
    private boolean isFirstBindDone = false;

    // UI components
    private RelativeLayout gameActivityVerticalLayout;
    private ViewPager mPager;
    private GameStatsPagerAdapter mPagerAdapter;
    private GameStickMenFragment stickMenFragment;

    // top bar
    private View gameGoalView;
    private ImageView gameGoalProfilePic;
    private TextView gameGoalText;
    private View gameMessageView;
    private TextView gameMessageText;
    private ImageView gameMessageIcon;

    // bottom bar
    private boolean locked = true; // is the UI locked?
    private ImageButton musicButton;
    private ImageButton glassButton;
    private ImageButton lockButton;
    private ImageButton pauseButton;
    private ImageButton quitButton;
    private TextView raceYourselfWords;

    // Overlays
    private View gameOverlayGps;
    private View gameOverlayPause;
    private View gameOverlayQuit;
    private TextView gameOverlayGpsTitle;
    private TextView gameOverlayGpsDescription;
    private ImageView gameOverlayGpsImage;
    private TextView gameOverlayGpsAction;
    private Button gameOverlayGpsCancelButton;
    private Button gameOverlayGpsActionButton;
    private ImageButton gameOverlayPauseContinueButton;
    private ImageButton gameOverlayPauseQuitButton;
    private ImageButton gameOverlayQuitContinueButton;
    private ImageButton gameOverlayQuitQuitButton;

    private Button overlayHomeGlassButton;
    private ImageView overlayHomeGlassIcon;
    private TextView overlayHomeGlassLabelConnecting;
    private TextView overlayHomeGlassLabelConnected;

    private ChallengeDetailBean challengeDetail;

    // Sound
    private VoiceFeedbackController voiceFeedbackController = new VoiceFeedbackController(this);

    // Glass
    private final static boolean BROADCAST_TO_GLASS = true;
    private GlassController glassController = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        getActionBar().hide();  // no action-bar on the in-game screens
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  // keep the screen on during this activity
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        log.trace("onCreate");

        // savedInstanceState will be null on the 1st invocation of onCreate only
        // important to only do this stuff once, otherwise we end up with multiple copies of each fragment
        if (savedInstanceState == null) {

            // initialise glass controller & start listening for connections
            if (BROADCAST_TO_GLASS) {
                glassController = new GlassController();
            }

            // extract game configuration etc from bundle, and set up the game service
            // TODO: make this generic for multiple game strategies / player combinations
            Bundle extras = getIntent().getExtras();
            challengeDetail = extras.getParcelable("challenge");

            if(challengeDetail.getOpponentTrack() != null) {
                Track selectedTrack = Track.get(challengeDetail.getOpponentTrack().getDeviceId(), challengeDetail.getOpponentTrack().getTrackId());
                positionControllers.add(new RecordedTrackPositionController(selectedTrack));
            } else {
                positionControllers.add(new FixedVelocityPositionController());
            }
            positionControllers.add(new OutdoorPositionController(getApplicationContext()));

            // start the background service that runs the game
            // we initialise it once it's bound
            startService(new Intent(this, GameService.class));

            gameActivityVerticalLayout = (RelativeLayout)findViewById(R.id.gameActivityVerticalLayout);
            gameGoalView = findViewById(R.id.gameGoal);
            gameGoalText = (TextView)findViewById(R.id.gameGoalText);
            gameGoalProfilePic = (ImageView)findViewById(R.id.gameGoalProfilePic);
            gameMessageView = findViewById(R.id.gameMessage);
            gameMessageText = (TextView)findViewById(R.id.gameMessageText);
            gameMessageIcon = (ImageView)findViewById(R.id.gameMessageIcon);

            stickMenFragment = (GameStickMenFragment)getSupportFragmentManager().findFragmentById(R.id.gameStickMenFragment);
            musicButton = (ImageButton)findViewById(R.id.gameMusicButton);
            glassButton = (ImageButton)findViewById(R.id.gameGlassButton);
            lockButton = (ImageButton)findViewById(R.id.gameLockButton);
            pauseButton = (ImageButton)findViewById(R.id.gamePauseButton);
            quitButton = (ImageButton)findViewById(R.id.gameQuitButton);
            raceYourselfWords = (TextView)findViewById(R.id.gameRaceYourselfWords);

            // overlays
            gameOverlayGps = findViewById(R.id.gameOverlayGps);
            gameOverlayPause = findViewById(R.id.gameOverlayPause);
            gameOverlayQuit = findViewById(R.id.gameOverlayQuit);
            gameOverlayGpsTitle = (TextView)findViewById(R.id.gameOverlayGpsTitle);
            gameOverlayGpsDescription = (TextView)findViewById(R.id.gameOverlayGpsDescription);
            gameOverlayGpsImage = (ImageView)findViewById(R.id.gameOverlayGpsImage);
            gameOverlayGpsAction = (TextView)findViewById(R.id.gameOverlayGpsAction);
            gameOverlayGpsCancelButton = (Button)findViewById(R.id.gameOverlayGpsCancelButton);
            gameOverlayGpsActionButton = (Button)findViewById(R.id.gameOverlayGpsActionButton);
            gameOverlayPauseContinueButton = (ImageButton)findViewById(R.id.gameOverlayPauseContinueButton);
            gameOverlayPauseQuitButton = (ImageButton)findViewById(R.id.gameOverlayPauseQuitButton);
            gameOverlayQuitContinueButton = (ImageButton)findViewById(R.id.gameOverlayQuitContinueButton);
            gameOverlayQuitQuitButton = (ImageButton)findViewById(R.id.gameOverlayQuitQuitButton);

            // overlays should capture all touch input and prevent it from triggering underlying views
            View.OnTouchListener nullTouchListener = new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            };
            gameOverlayGps.setOnTouchListener(nullTouchListener);
            gameOverlayPause.setOnTouchListener(nullTouchListener);
            gameOverlayQuit.setOnTouchListener(nullTouchListener);

            // button listeners
            musicButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = null;
                    if (android.os.Build.VERSION.SDK_INT >= 15) {
                        intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC);  // API level 15+ only
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    } else {
                        intent = new Intent("android.intent.action.MUSIC_PLAYER");  // API level 8+ only
                    }
                    try {
                        startActivity(intent);
                    } catch (android.content.ActivityNotFoundException e) {
                        log.error("Failed to find a music player", e);
                        //TODO: display visual error to user
                    }
                }
            });

            glassButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // inflate the glass overlay
                    View glassOverlay = getLayoutInflater().inflate(R.layout.overlay_home_glass, null);
                    gameActivityVerticalLayout.addView(glassOverlay);
                    overlayHomeGlassButton = (Button)findViewById(R.id.overlay_home_glass_button);
                    overlayHomeGlassIcon = (ImageView)findViewById(R.id.overlay_home_glass_icon);
                    overlayHomeGlassLabelConnecting = (TextView)findViewById(R.id.overlay_home_glass_label_connecting);
                    overlayHomeGlassLabelConnected = (TextView)findViewById(R.id.overlay_home_glass_label_connected);
                    overlayHomeGlassButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // try to connect to glass
                            //GlassController gc = new GlassController();
                            overlayHomeGlassButton.setVisibility(View.GONE);
                            overlayHomeGlassLabelConnecting.setVisibility(View.VISIBLE);
                            overlayHomeGlassIcon.setBackgroundColor(Color.parseColor("#ffccaa"));
                        }
                    });
                }
            });

            gameOverlayGpsActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switch (positionAccuracy) {
                        case 1: {
                            Intent gpsOptionsIntent = new Intent(
                                    android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(gpsOptionsIntent);
                            break;
                        }
                        case 2: {
                            // nothing
                            break;
                        }
                        default: {
                            gameOverlayGps.setVisibility(View.GONE);
                            gameService.start();
                        }
                    }
                }
            });

            gameOverlayGpsCancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });

            lockButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (locked) {
                        locked = false;
                        lockButton.setImageResource(R.drawable.icon_unlocked);
                        raceYourselfWords.setVisibility(View.GONE);
                        pauseButton.setVisibility(View.VISIBLE);
                        musicButton.setVisibility(View.VISIBLE);
                        quitButton.setVisibility(View.VISIBLE);
                    } else {
                        locked = true;
                        lockButton.setImageResource(R.drawable.icon_locked);
                        pauseButton.setVisibility(View.GONE);
                        musicButton.setVisibility(View.GONE);
                        quitButton.setVisibility(View.GONE);
                        raceYourselfWords.setVisibility(View.VISIBLE);
                    }
                }
            });

            pauseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    log.info("Pause pressed, pausing game");
                    if (gameService != null) gameService.stop();
                    gameOverlayPause.setVisibility(View.VISIBLE);
                }
            });

            quitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    log.info("Quit pressed, pausing game");
                    if (gameService != null) gameService.stop();
                    gameOverlayQuit.setVisibility(View.VISIBLE);
                }
            });

            gameOverlayPauseContinueButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    log.info("Continue pressed, un-pausing game");
                    if (gameService != null) gameService.start();
                    gameOverlayPause.setVisibility(View.GONE);
                }
            });

            gameOverlayPauseQuitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    log.info("Quit pressed, exiting GameActivity");
                    gameOverlayPause.setVisibility(View.GONE);
                    finish();
                }
            });

            gameOverlayQuitContinueButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    log.info("Continue pressed, un-pausing game");
                    if (gameService != null) gameService.start();
                    gameOverlayQuit.setVisibility(View.GONE);
                }
            });

            gameOverlayQuitQuitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    log.info("Quit pressed, exiting GameActivity");
                    gameOverlayQuit.setVisibility(View.GONE);
                    finish();
                }
            });

            // Instantiate a ViewPager and a PagerAdapter.
            mPager = (ViewPager) findViewById(R.id.gameStatsPager);
            mPagerAdapter = new GameStatsPagerAdapter(getSupportFragmentManager());
            mPager.setAdapter(mPagerAdapter);

            // little circles to show which page of the pager is showing
            CirclePageIndicator circlePageIndicator = (CirclePageIndicator)findViewById(R.id.circlePageIndicator);
            circlePageIndicator.setFillColor(Color.parseColor("#696761"));
            circlePageIndicator.setRadius(10.0f);
            circlePageIndicator.setPageColor(Color.parseColor("#d1d2d4"));
            circlePageIndicator.setStrokeColor(Color.parseColor("#00ffffff"));
            circlePageIndicator.setViewPager(mPager);


            // set up a connection to the game service
            gameServiceConnection = new ServiceConnection() {

                // initialize the service as soon as we're connected
                public void onServiceConnected(ComponentName className, IBinder binder) {
                    gameService = ((GameService.GameServiceBinder)binder).getService();
                    // must do first bind before passing the gameService reference to anyone else
                    if (!isFirstBindDone) {
                        isFirstBindDone = true;
                        onFirstBind();
                    }
                    mPagerAdapter.setGameService(gameService); // pass the reference to all paged fragments
                    stickMenFragment.setGameService(gameService);
                    voiceFeedbackController.setGameService(gameService);
                    if (BROADCAST_TO_GLASS) glassController.setGameService(gameService);
                    displayGameMessage();
                    voiceFeedbackController.sayPaceDelta();
                    log.debug("Bound to GameService");
                }

                public void onServiceDisconnected(ComponentName className) {
                    // only called when service unexpectedly unbinds
                    // TODO: work out how to recover from this
                    gameService = null;
                    log.warn("Unexpectedly unbound from GameService");
                }
            };


            // add the UI fragments to the layout - in order of display
//            FragmentManager fm = this.getSupportFragmentManager();
//            fm.beginTransaction()
//                    .add(R.id.gameFragmentHolder, hudPage1Fragment)
//                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        if(gameService.getGameState() != GameService.GameState.PAUSED) {
            log.info("game - is not paused so stopping");
            if (gameService != null) gameService.stop();
            gameOverlayQuit.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        bindService(new Intent(this, GameService.class), gameServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();
        unbindService(gameServiceConnection);
    }

    @Override
    public void finish() {
        // shut-down the background service if we're exiting the game
        if (gameService != null) {
            gameService.shutdown();
        }
        // probably don't need next 4 lines
        mPagerAdapter.setGameService(null); // clear the reference from all fragments
        stickMenFragment.setGameService(null);
        voiceFeedbackController.setGameService(null);
        if (BROADCAST_TO_GLASS) glassController.setGameService(null);
        this.gameService = null;
        stopService(new Intent(GameActivity.this, GameService.class));
        super.finish();
    }

    /**
     * Initialisation that can only be carried out once we have bound to the game service
     */
    private void onFirstBind() {
        if (gameService == null) log.error("onFirstBind called when game service not bound");
        log.debug("onFirstBind called");

        GameConfiguration gameConfiguration = new GameConfiguration.GameStrategyBuilder(GameConfiguration.GameType.TIME_CHALLENGE).targetTime(challengeDetail.getChallenge().getChallengeGoal() * 1000).countdown(3000).build();
        gameService.initialize(positionControllers, gameConfiguration);

        // initialize view
        switch (gameConfiguration.getGameType()) {
            case DISTANCE_CHALLENGE: {
                gameGoalText.setText("Who can run " + Format.zeroDp(UnitConversion.miles(gameConfiguration.getTargetDistance())) + " miles the quickest?");
                break;
            }
            case TIME_CHALLENGE: {
                gameGoalText.setText("Who can run the furthest in " + Format.zeroDp(UnitConversion.minutes(gameConfiguration.getTargetTime())) + "min?");
                break;
            }
        }
        Picasso.with(this)
                .load(challengeDetail.getOpponent().getProfilePictureUrl())
                .placeholder(R.drawable.default_profile_pic)
                .transform(new PictureUtils.CropCircle())
                .into(gameGoalProfilePic);


        gameService.registerRegularUpdateListener(new RegularUpdateListener() {
            @Override
            public void onRegularUpdate() {
                if (overlayHomeGlassLabelConnecting.getVisibility() == View.VISIBLE && glassController.isConnected()) {
                    overlayHomeGlassLabelConnecting.setVisibility(View.GONE);
                    overlayHomeGlassLabelConnected.setVisibility(View.VISIBLE);
                    overlayHomeGlassIcon.setBackgroundColor(Color.parseColor("#aaffaa"));
                }
            }
        });

        // add a listener for changes to the local player's positioning accuracy
        gameService.registerRegularUpdateListener(new RegularUpdateListener() {
            @Override
            public void onRegularUpdate() {
                log.trace("PositionAccuracy callback triggered");
                PositionController player = gameService.getLocalPlayer();
                if (player instanceof OutdoorPositionController) {
                    OutdoorPositionController p = (OutdoorPositionController) player;
                    positionAccuracy = 1;
                    if (p.isLocationEnabled()) positionAccuracy++;
                    if (p.isLocationAvailable()) positionAccuracy++;
                    if (p.isLocationAccurateEnough()) positionAccuracy++;
                } else {
                    positionAccuracy = 4;
                }
                log.trace("PositionAccuracy is " + positionAccuracy);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (gameOverlayGps.getVisibility() == View.VISIBLE) {
                            log.trace("Updating GPS overlay");
                            switch (positionAccuracy) {
                                case 1:
                                    gameOverlayGpsTitle.setText(R.string.gps_title_1);
                                    gameOverlayGpsTitle.setTextColor(Color.RED);
                                    gameOverlayGpsDescription.setText(R.string.gps_description_1);
                                    gameOverlayGpsImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_gps_red1));
                                    gameOverlayGpsAction.setText(R.string.gps_action_1);
                                    gameOverlayGpsActionButton.setText(R.string.gps_button_1);
                                    break;
                                case 2:
                                    gameOverlayGpsTitle.setText(R.string.gps_title_2);
                                    gameOverlayGpsTitle.setTextColor(Color.YELLOW);
                                    gameOverlayGpsDescription.setText(R.string.gps_description_2);
                                    gameOverlayGpsImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_gps_yellow2));
                                    gameOverlayGpsAction.setText(R.string.gps_action_2);
                                    gameOverlayGpsActionButton.setText(R.string.gps_button_2);
                                    break;
                                case 3:
                                    gameOverlayGpsTitle.setText(R.string.gps_title_3);
                                    gameOverlayGpsTitle.setTextColor(Color.GREEN);
                                    gameOverlayGpsDescription.setText(R.string.gps_description_3);
                                    gameOverlayGpsImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_gps_green3));
                                    gameOverlayGpsAction.setText(R.string.gps_action_3);
                                    gameOverlayGpsActionButton.setText(R.string.gps_button_3);
                                    break;
                                case 4:
                                    gameOverlayGpsTitle.setText(R.string.gps_title_4);
                                    gameOverlayGpsTitle.setTextColor(Color.GREEN);
                                    gameOverlayGpsDescription.setText(R.string.gps_description_4);
                                    gameOverlayGpsImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_gps_green4));
                                    gameOverlayGpsAction.setText(R.string.gps_action_4);
                                    gameOverlayGpsActionButton.setText(R.string.gps_button_4);
                                    break;
                            }

                            // if we have high accuracy, dismiss the dialog and start the race
                            if (positionAccuracy == 4) {
                                Timer timer = new Timer();
                                timer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                gameOverlayGps.setVisibility(View.GONE);
                                            }
                                        });
                                        gameService.start();
                                    }
                                }, 500);
                            }
                        }
                    }
                });
            }
        }.setRecurrenceInterval(500));

        gameService.registerGameEventListener(new GameEventListener() {
            @Override
            public void onGameEvent(String eventTag) {
                if (eventTag.equals("Finished")) {
                    log.info("Game finished, launching challenge summary");
                    gameService.stop();  // stops position controllers and forces summary data to be written to track
                    gameService.unregisterGameEventListener(this);

                    // if we've recorded a track, register it as an attempt & add it to the challenge summary bean
                    PositionController p = gameService.getLocalPlayer();
                    if (p instanceof OutdoorPositionController) {
                        Track track = ((OutdoorPositionController)gameService.getLocalPlayer()).completeTrack();
                        Challenge challenge = Challenge.get(challengeDetail.getChallenge().getDeviceId(), challengeDetail.getChallenge().getChallengeId());
                        if (challenge != null) {
                            // real/shared/non-transient challenge (i.e. not match making)
                            challenge.addAttempt(track);
                        }
                        // Mark opponent track(s) as matched so that we do not get it/them again in the quickmatch
                        for (PositionController pc : gameService.getPositionControllers()) {
                            if (pc instanceof RecordedTrackPositionController) {
                                Track otherTrack = ((RecordedTrackPositionController)pc).getTrack();
                                MatchedTrack mt = new MatchedTrack(otherTrack);
                                mt.save();
                            }
                        }
                        TrackSummaryBean trackSummaryBean = new TrackSummaryBean(track);
                        challengeDetail.setPlayerTrack(trackSummaryBean);
                    }

                    // launch the challenge summary activity
                    Intent challengeSummary = new Intent(GameActivity.this, ChallengeSummaryActivity.class);
                    challengeSummary.putExtra("challenge", challengeDetail);
                    startActivity(challengeSummary);
                    finish();
                }
            }
        });
    }

    private void displayGameMessage() {
        if (gameService == null || !gameService.isInitialized()) return;
        PositionController player = gameService.getLocalPlayer();
        PositionController leadingOpponent = gameService.getLeadingOpponent();
        if (player.getRealDistance() > 0) {
            if (player.getCurrentSpeed() > leadingOpponent.getCurrentSpeed()) {
                gameMessageText.setText("WINNING PACE");
                gameMessageText.setTextColor(Color.parseColor("#85d2de"));
                gameMessageIcon.setImageResource(R.drawable.icon_runner_with_trail_blue);
            } else {
                gameMessageText.setText("LOSING PACE");
                gameMessageText.setTextColor(Color.parseColor("#ce5557"));
                gameMessageIcon.setImageResource(R.drawable.icon_runner_with_trail_red);
            }
            gameGoalView.setVisibility(View.GONE);
            gameMessageView.setVisibility(View.VISIBLE);

            // remove message after a delay
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    gameGoalView.setVisibility(View.VISIBLE);
                    gameMessageView.setVisibility(View.GONE);
                }
            }, 1700);

        }
    }


    /**
     * Fragment pager for the top part of the screen, showing the game stats
     */
    private class GameStatsPagerAdapter extends FragmentPagerAdapter {

        private GameStatsPage1Fragment fragment1 = new GameStatsPage1Fragment();
        private GameStatsPage2Fragment fragment2 = new GameStatsPage2Fragment();

        public GameStatsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0: return fragment1;
                case 1: return fragment2;
                default: return fragment1;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        public void setGameService(GameService s) {
            fragment1.setGameService(s);
            fragment2.setGameService(s);

        }

    }

}
