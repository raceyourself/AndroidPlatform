package com.raceyourself.raceyourself.game;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.raceyourself.platform.models.Track;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.BaseFragmentActivity;
import com.raceyourself.raceyourself.game.position_controllers.FixedVelocityPositionController;
import com.raceyourself.raceyourself.game.position_controllers.OutdoorPositionController;
import com.raceyourself.raceyourself.game.position_controllers.PositionController;
import com.raceyourself.raceyourself.game.position_controllers.RecordedTrackPositionController;
import com.raceyourself.raceyourself.home.ChallengeDetailBean;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GameActivity extends BaseFragmentActivity {

    private GameService gameService;
    private ServiceConnection gameServiceConnection;

    private List<PositionController> positionControllers = new ArrayList<PositionController>();
//    private GameConfiguration gameConfiguration;

    // UI components
    private ViewPager mPager;
    private GameStatsPagerAdapter mPagerAdapter;
    private GameStickMenFragment stickMenFragment;

    // bottom bar
    private boolean locked = true; // is the UI locked?
    private ImageButton musicButton;
    private ImageButton lockButton;
    private ImageButton pauseButton;
    private ImageButton quitButton;
    private ImageView raceYourselfWords;

    // Overlays
    private View gameOverlayPause;
    private View gameOverlayQuit;
    private ImageButton gameOverlayPauseContinueButton;
    private ImageButton gameOverlayPauseQuitButton;
    private ImageButton gameOverlayQuitContinueButton;
    private ImageButton gameOverlayQuitQuitButton;

    private ChallengeDetailBean challengeDetail;

    // Sound
    private VoiceFeedbackController voiceFeedbackController = new VoiceFeedbackController(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        getActionBar().hide();  // no action-bar on the in-game screens
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  // keep the screen on during this activity
        log.warn("onCreate");

        // savedInstanceState will be null on the 1st invocation of onCreate only
        // important to only do this stuff once, otherwise we end up with multiple copies of each fragment
        if (savedInstanceState == null) {

            // TODO: make this generic for multiple game strategies / player combinations
            // position controllers for player and opponent(s)
//            positionControllers.add(new OutdoorPositionController(this));
//            positionControllers.add(new FixedVelocityPositionController());
//            gameConfiguration = new GameConfiguration.GameStrategyBuilder(GameConfiguration.GameType.TIME_CHALLENGE).targetTime(120000).countdown(3000).build();
            //gameStrategy = new GameStrategy.GameStrategyBuilder(GameStrategy.GameType.DISTANCE_CHALLENGE).targetDistance(500).countdown(3000).build();

            Bundle extras = getIntent().getExtras();
            challengeDetail = extras.getParcelable("challenge");

            if(challengeDetail.getOpponentTrack() != null) {
                Track selectedTrack = Track.get(challengeDetail.getOpponentTrack().getDeviceId(), challengeDetail.getOpponentTrack().getTrackId());
                positionControllers.add(new RecordedTrackPositionController(selectedTrack));
            } else {
                positionControllers.add(new FixedVelocityPositionController());
            }
            positionControllers.add(new OutdoorPositionController(this));
//            gameConfiguration = new GameConfiguration.GameStrategyBuilder(GameConfiguration.GameType.TIME_CHALLENGE).targetTime(challengeDetail.getChallenge().getChallengeGoal() * 60 * 1000).countdown(3000).build();
//            startService(new Intent(this, GameService.class));
//
//            gameService.initialize(positionControllers, gameConfiguration);
//            gameService.start();

            stickMenFragment = (GameStickMenFragment)getSupportFragmentManager().findFragmentById(R.id.gameStickMenFragment);
            musicButton = (ImageButton)findViewById(R.id.gameMusicButton);
            lockButton = (ImageButton)findViewById(R.id.gameLockButton);
            pauseButton = (ImageButton)findViewById(R.id.gamePauseButton);
            quitButton = (ImageButton)findViewById(R.id.gameQuitButton);
            raceYourselfWords = (ImageView)findViewById(R.id.gameRaceYourselfWords);

            // overlays
            gameOverlayPause = findViewById(R.id.gameOverlayPause);
            gameOverlayQuit = findViewById(R.id.gameOverlayQuit);
            gameOverlayPauseContinueButton = (ImageButton)findViewById(R.id.gameOverlayPauseContinueButton);
            gameOverlayPauseQuitButton = (ImageButton)findViewById(R.id.gameOverlayPauseQuitButton);
            gameOverlayQuitContinueButton = (ImageButton)findViewById(R.id.gameOverlayQuitContinueButton);
            gameOverlayQuitQuitButton = (ImageButton)findViewById(R.id.gameOverlayQuitQuitButton);

            // button listeners
            musicButton.setVisibility(View.GONE);  // TODO: make it work, and re-enable
            musicButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent("android.intent.category.APP_MUSIC");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        startActivity(intent);
                    } catch (android.content.ActivityNotFoundException e) {
                        log.error("Failed to find a music player");
                        //TODO: display visual error to user
                    }
                }
            });

            lockButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (locked) {
                        locked = false;
                        lockButton.setImageResource(R.drawable.icon_unlocked_black);
                        raceYourselfWords.setVisibility(View.GONE);
                        pauseButton.setVisibility(View.VISIBLE);
                        quitButton.setVisibility(View.VISIBLE);
                    } else {
                        locked = true;
                        lockButton.setImageResource(R.drawable.icon_locked_black);
                        pauseButton.setVisibility(View.GONE);
                        quitButton.setVisibility(View.GONE);
                        raceYourselfWords.setVisibility(View.VISIBLE);
                    }
                }
            });

            pauseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    gameService.stop();
                    gameOverlayPause.setVisibility(View.VISIBLE);
                }
            });

            quitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    gameService.stop();
                    gameOverlayQuit.setVisibility(View.VISIBLE);
                }
            });

            gameOverlayPauseContinueButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    gameService.start();
                    gameOverlayPause.setVisibility(View.GONE);
                }
            });

            gameOverlayPauseQuitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    gameOverlayPause.setVisibility(View.GONE);
                    finish();
                }
            });

            gameOverlayQuitContinueButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    gameService.start();
                    gameOverlayQuit.setVisibility(View.GONE);
                }
            });

            gameOverlayQuitQuitButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    gameOverlayQuit.setVisibility(View.GONE);
                    finish();
                }
            });

            // Instantiate a ViewPager and a PagerAdapter.
            mPager = (ViewPager) findViewById(R.id.gameStatsPager);
            mPagerAdapter = new GameStatsPagerAdapter(getSupportFragmentManager());
            mPager.setAdapter(mPagerAdapter);

            // start the game service (no harm done if already started)
            log.info("Starting GameService");
//            startService(new Intent(this, GameService.class));

            // set up a connection to the game service
            gameServiceConnection = new ServiceConnection() {

                // initialize the service as soon as we're connected
                public void onServiceConnected(ComponentName className, IBinder binder) {
                    gameService = ((GameService.GameServiceBinder)binder).getService();
                    mPagerAdapter.setGameService(gameService); // pass the reference to all paged fragments
                    stickMenFragment.setGameService(gameService);
                    voiceFeedbackController.setGameService(gameService);
                    log.debug("Bound to GameService");
                }

                public void onServiceDisconnected(ComponentName className) {
                    gameService = null;
                    mPagerAdapter.setGameService(null); // clear the reference from all fragments
                    stickMenFragment.setGameService(null);
                    voiceFeedbackController.setGameService(null);
                    log.debug("Unbound from GameService");
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
    public void onDestroy() {
        // stop the game service. May want to move this to another activity, as accessing the service
        // from e.g. a post-race screen could be useful.
        log.info("Stopping GameService");
//        stopService(new Intent(this, GameService.class));
        super.onDestroy();
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
