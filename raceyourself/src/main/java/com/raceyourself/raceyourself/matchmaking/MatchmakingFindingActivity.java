package com.raceyourself.raceyourself.matchmaking;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.AutoMatches;
import com.raceyourself.platform.models.Challenge;
import com.raceyourself.platform.models.Track;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.game.GameActivity;
import com.raceyourself.raceyourself.game.GameConfiguration;
import com.raceyourself.raceyourself.game.GameService;
import com.raceyourself.raceyourself.game.position_controllers.FixedVelocityPositionController;
import com.raceyourself.raceyourself.game.position_controllers.OutdoorPositionController;
import com.raceyourself.raceyourself.game.position_controllers.PositionController;
import com.raceyourself.raceyourself.game.position_controllers.TrackPositionController;
import com.raceyourself.raceyourself.home.DurationChallengeBean;
import com.raceyourself.raceyourself.utils.PictureUtils;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MatchmakingFindingActivity extends Activity {

    TextView matchingText;
    TextView searchingText;
    TextView matrixText;
    TextView foundText;

    ImageView heartIcon;
    ImageView globeIcon;
    ImageView wandIcon;
    ImageView tickIcon;

    Animation translateRightAnim;
    Animation rotationAnim;

    Drawable heartIconDrawable;
    Drawable globeIconDrawable;
    Drawable wandIconDrawable;
    Drawable tickIconDrawable;

    Drawable spinnerIconDrawable;

    Button raceButton;

    User opponent;

    TextView opponentNameText;
    ImageView opponentProfilePic;

    int animationCount = 0;

    private GameConfiguration gameConfiguration;
    private GameService gameService;

    private ServiceConnection gameServiceConnection;

    private List<PositionController> positionControllers = new ArrayList<PositionController>();

    private Challenge quickmatchChallenge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matchmaking_finding);

        matchingText = (TextView)findViewById(R.id.matchingText);
        searchingText = (TextView)findViewById(R.id.searchingText);
        matrixText = (TextView)findViewById(R.id.matrixText);
        foundText = (TextView)findViewById(R.id.matchedText);

        heartIcon = (ImageView)findViewById(R.id.heartIcon);
        globeIcon = (ImageView)findViewById(R.id.globeIcon);
        wandIcon = (ImageView)findViewById(R.id.wandIcon);
        tickIcon = (ImageView)findViewById(R.id.tickIcon);

        translateRightAnim = AnimationUtils.loadAnimation(this, R.anim.matched_text_anim);
        rotationAnim = AnimationUtils.loadAnimation(this, R.anim.rotating_icon_anim);

        heartIconDrawable = getResources().getDrawable(R.drawable.ic_heart_grey);
        globeIconDrawable = getResources().getDrawable(R.drawable.ic_globe_grey);
        wandIconDrawable = getResources().getDrawable(R.drawable.ic_wand_grey);
        tickIconDrawable = getResources().getDrawable(R.drawable.ic_tick_grey);
        spinnerIconDrawable = getResources().getDrawable(R.drawable.ic_spinner);

        raceButton = (Button)findViewById(R.id.startRaceBtn);

        opponentNameText = (TextView)findViewById(R.id.opponentName);
        opponentProfilePic = (ImageView)findViewById(R.id.opponentProfilePic);

        User user = User.get(AccessToken.get().getUserId());
        String url = user.getImage();

        final ImageView playerImage = (ImageView)findViewById(R.id.playerProfilePic);
        setProfilePic(url, playerImage);

        Bundle bundle = getIntent().getExtras();
        int duration = bundle.getInt("duration");

        List<Track> trackList = AutoMatches.getBucket(user.getProfile().running_fitness.toLowerCase(), duration);

        Random random = new Random();
        int trackNumber = random.nextInt(trackList.size());

        final Track selectedTrack = trackList.get(trackNumber);

        ExecutorService pool = Executors.newFixedThreadPool(1);
        final Future<User> futureUser = pool.submit(new Callable<User>() {
            @Override
            public User call() throws Exception {
                return SyncHelper.get("users/" + selectedTrack.user_id, User.class);
            }
        });

        translateRightAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                switch(animationCount) {
                    case 0:
                        startImageAnimation(heartIcon);
                        break;

                    case 1:
                        startImageAnimation(globeIcon);
                        break;

                    case 2:
                        startImageAnimation(wandIcon);
                        break;

                    case 3:
                        startImageAnimation(tickIcon);
                        break;
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        rotationAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                switch(animationCount) {
                    case 0:
                        endImageAnimation(heartIcon, heartIconDrawable, searchingText);
                        break;
                    case 1:
                        endImageAnimation(globeIcon, globeIconDrawable, matrixText);
                        break;
                    case 2:
                        endImageAnimation(wandIcon, wandIconDrawable, foundText);
                        break;
                    case 3:
                        tickIcon.setImageDrawable(tickIconDrawable);
                        raceButton.setVisibility(View.VISIBLE);
                        try {
                            opponent = futureUser.get();
                            opponentNameText.setText(opponent.name);
                            setProfilePic(opponent.getImage(), opponentProfilePic);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                        break;
                }
                if(animationCount < 3) {
                    animationCount++;
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        matchingText.startAnimation(translateRightAnim);

        positionControllers.add(new OutdoorPositionController(this));
        positionControllers.add(new TrackPositionController(selectedTrack));
        gameConfiguration = new GameConfiguration.GameStrategyBuilder(GameConfiguration.GameType.TIME_CHALLENGE).targetTime(duration * 60 * 1000).countdown(3000).build();

        startService(new Intent(this, GameService.class));

        gameServiceConnection = new ServiceConnection() {

            // initialize the service as soon as we're connected
            public void onServiceConnected(ComponentName className, IBinder binder) {
                gameService = ((GameService.GameServiceBinder)binder).getService();
            }

            public void onServiceDisconnected(ComponentName className) {
                gameService = null;
            }
        };

        quickmatchChallenge = new Challenge();
        quickmatchChallenge.type = "duration";
        quickmatchChallenge.addAttempt(selectedTrack);
    }

    public void onRaceClick(View view) {
        gameService.initialize(positionControllers, gameConfiguration);
        Intent gameIntent = new Intent(this, GameActivity.class);
        gameService.start();
        startActivity(gameIntent);
    }

    public void setProfilePic(String imageUrl, final ImageView imageView) {
        Picasso.with(MatchmakingFindingActivity.this).load(imageUrl).into(new Target() {

            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                Log.i("Matchmaking", "bitmap loaded correctly - " + imageView.getId());
                Bitmap roundedBitmap = PictureUtils.getRoundedBmp(bitmap, bitmap.getWidth());
                imageView.setImageBitmap(roundedBitmap);
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                Log.i("Matchmaking", "bitmap failed - " + imageView.getId());
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {}
        });
    }

    public void startImageAnimation(ImageView imageView) {
        imageView.setImageDrawable(spinnerIconDrawable);
        imageView.setVisibility(View.VISIBLE);
        imageView.startAnimation(rotationAnim);

    }

    public void endImageAnimation(ImageView imageView, Drawable drawable, TextView textView) {
        imageView.setImageDrawable(drawable);
        textView.setVisibility(View.VISIBLE);
        textView.startAnimation(translateRightAnim);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.matchmaking_finding, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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
}
