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
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.raceyourself.raceyourself.game.popup.GpsOverlay;
import com.raceyourself.raceyourself.game.popup.GpsPopup;
import com.raceyourself.raceyourself.game.popup.PauseOverlay;
import com.raceyourself.raceyourself.game.popup.QuitOverlay;
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
    private ChallengeDetailBean challengeDetail;

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

    // Popups
    private GpsOverlay gpsOverlay;
    private PauseOverlay pauseOverlay;
    private QuitOverlay quitOverlay;

    // temporary stuff for glass button (hidden)
    private Button overlayHomeGlassButton;
    private ImageView overlayHomeGlassIcon;
    private TextView overlayHomeGlassLabelConnecting;
    private TextView overlayHomeGlassLabelConnected;

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
            gpsOverlay = new GpsOverlay(this, gameActivityVerticalLayout);
            pauseOverlay = new PauseOverlay(this, gameActivityVerticalLayout);
            quitOverlay = new QuitOverlay(this, gameActivityVerticalLayout);

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
                    pauseOverlay.popup();
                }
            });

            quitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    log.info("Quit pressed, pausing game");
                    if (gameService != null) gameService.stop();
                    quitOverlay.popup();
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
                    gpsOverlay.setGameService(gameService);
                    displayGameMessage();
                    voiceFeedbackController.sayOutlook();
                    log.debug("Bound to GameService");
                }

                public void onServiceDisconnected(ComponentName className) {
                    // only called when service unexpectedly unbinds
                    // TODO: work out how to recover from this
                    gameService = null;
                    log.warn("Unexpectedly unbound from GameService");
                }
            };

            // popup the searching for GPS overlay
            gpsOverlay.popup();


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
            quitOverlay.popup();
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
        // wait a moment (for e.g. sounds to finish playing), then..
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // shut-down the background service if we're exiting the game
                        if (gameService != null) {
                            gameService.shutdown();
                        }
                        // probably don't need next 4 lines
                        mPagerAdapter.setGameService(null); // clear the reference from all fragments
                        stickMenFragment.setGameService(null);
                        voiceFeedbackController.setGameService(null);
                        if (BROADCAST_TO_GLASS) glassController.setGameService(null);
                        GameActivity.this.gameService = null;
                        stopService(new Intent(GameActivity.this, GameService.class));
                        GameActivity.super.finish();
                    }
                }, 1200);
            }
        });

    }

    /**
     * Initialisation that can only be carried out once we have bound to the game service
     */
    private void onFirstBind() {
        if (gameService == null) log.error("onFirstBind called when game service not bound");
        log.debug("onFirstBind called");

        GameConfiguration gameConfiguration = new GameConfiguration.GameStrategyBuilder(GameConfiguration.GameType.TIME_CHALLENGE).targetTime(challengeDetail.getChallenge().getChallengeGoal() * 1000).countdown(2999).build();
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

        if (gameService == null
                || !gameService.isInitialized()
                || gameService.getGameState() != GameService.GameState.IN_PROGRESS) return;

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
