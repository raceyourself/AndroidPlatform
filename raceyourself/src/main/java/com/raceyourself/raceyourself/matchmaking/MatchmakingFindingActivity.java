package com.raceyourself.raceyourself.matchmaking;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.AutoMatches;
import com.raceyourself.platform.models.Track;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.BaseActivity;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.base.util.StringFormattingUtils;
import com.raceyourself.raceyourself.game.GameActivity;
import com.raceyourself.raceyourself.home.ChallengeBean;
import com.raceyourself.raceyourself.home.ChallengeDetailBean;
import com.raceyourself.raceyourself.home.TrackSummaryBean;
import com.raceyourself.raceyourself.home.UserBean;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MatchmakingFindingActivity extends BaseActivity {

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

    ChallengeDetailBean challengeDetail;

    int animationCount = 0;

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
        Picasso.with(MatchmakingFindingActivity.this).load(url).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(playerImage);

        // requirements for opponent track
        Bundle bundle = getIntent().getExtras();
        int duration = bundle.getInt("duration");
        String fitnessLevel = user.getProfile().running_fitness.toLowerCase();

        // find an appropriate opponent track
        log.info("Querying tracklist for fitness: " + fitnessLevel + " and duration: " + duration);
        List<Track> trackList = AutoMatches.getBucket(fitnessLevel, duration);
        log.info(trackList.size() + " appropriate tracks found");
        if (trackList.size() == 0) {
            log.error("No tracks found for this distance / fitness level. Please try another.");
            Toast toast = new Toast(this);
            toast.setText("No tracks found for this distance / fitness level. Please try another.");
        }

        // choose random track from list
        Random random = new Random();
        int trackNumber = random.nextInt(trackList.size());
        final Track selectedTrack = trackList.get(trackNumber);

        // background thread to pull chosen opponent's details from server
        ExecutorService pool = Executors.newFixedThreadPool(1);
        final Future<User> futureUser = pool.submit(new Callable<User>() {
            @Override
            public User call() throws Exception {
                return SyncHelper.get("users/" + selectedTrack.user_id, User.class);
            }
        });

        // start the funky matching animations
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
                            Picasso.with(MatchmakingFindingActivity.this).load(opponent.getImage()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(opponentProfilePic);

                            UserBean opponentBean = new UserBean(opponent);
                            challengeDetail.setOpponent(opponentBean);

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

        challengeDetail = new ChallengeDetailBean();

        UserBean player = new UserBean(user);
        challengeDetail.setPlayer(player);

        TrackSummaryBean opponentTrack = new TrackSummaryBean(selectedTrack);
        challengeDetail.setOpponentTrack(opponentTrack);

        ChallengeBean challengeBean = new ChallengeBean(null);
        challengeBean.setType("duration");
        challengeBean.setChallengeGoal(duration * 60);
        challengeDetail.setChallenge(challengeBean);

        challengeDetail.setPoints(20000);
    }

    public void onRaceClick(View view) {
        Intent gameIntent = new Intent(this, GameActivity.class);
        gameIntent.putExtra("challenge", challengeDetail);
        startActivity(gameIntent);
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
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }
}
