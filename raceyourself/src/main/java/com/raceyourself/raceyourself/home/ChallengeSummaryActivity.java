package com.raceyourself.raceyourself.home;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.AtomicDouble;
import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.Challenge;
import com.raceyourself.platform.models.Transaction;
import com.raceyourself.platform.models.User;
import com.raceyourself.platform.points.PointsHelper;
import com.raceyourself.platform.utils.Format;
import com.raceyourself.platform.utils.UnitConversion;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.ParticleAnimator;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.home.feed.ChallengeDetailBean;
import com.raceyourself.raceyourself.home.feed.TrackSummaryBean;
import com.raceyourself.raceyourself.shop.ShopActivity_;
import com.squareup.picasso.Picasso;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class ChallengeSummaryActivity extends Activity {

    // Details of challenge to populate values of summary
    ChallengeDetailBean challengeDetail;

    // String for previous activity
    String previous = "";

    // Coin animator
    private ParticleAnimator coinAnimator = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_challenge_summary);

        // Get action bar and disable unnecessary elements
        ActionBar mActionBar = getActionBar();
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(false);

        // Inflate the custom layout for the action bar
        LayoutInflater li = LayoutInflater.from(this);
        final View actionBarView = li.inflate(R.layout.action_bar_home, null);

        // Set the action bar layout and background colour
        mActionBar.setCustomView(actionBarView);
        mActionBar.setDisplayShowCustomEnabled(true);
        mActionBar.setBackgroundDrawable(new ColorDrawable(Color.WHITE));

        // Set the player's points in the action bar
        final TextView pointsView = (TextView) actionBarView.findViewById(R.id.points_value);
        User player = User.get(AccessToken.get().getUserId());
        pointsView.setText(String.valueOf(player.getPoints()));

        // Show the store when the button is pressed
        ImageView store = (ImageView) actionBarView.findViewById(R.id.store);
        store.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shopIntent = new Intent(ChallengeSummaryActivity.this, ShopActivity_.class);
                startActivity(shopIntent);
            }
        });

        // Popup a toast for the settings
        ImageView settings = (ImageView) actionBarView.findViewById(R.id.action_settings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ChallengeSummaryActivity.this, "Settings menu. Coming soon!", Toast.LENGTH_SHORT).show();
            }
        });

        // Popup a toast for the watch
        ImageView watch = (ImageView) actionBarView.findViewById(R.id.watchIcon);
        watch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ChallengeSummaryActivity.this, "Smartwatch integration. Coming soon!", Toast.LENGTH_SHORT).show();
            }
        });

        // Popup a toast for glass
        ImageView glass = (ImageView) actionBarView.findViewById(R.id.glassIcon);
        glass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ChallengeSummaryActivity.this, "Google Glass integration. Coming soon!", Toast.LENGTH_SHORT).show();
            }
        });

        // Check if there is an extra for previous screen and set it if so
        if(getIntent().hasExtra("previous")) {
            previous = getIntent().getStringExtra("previous");
        } else {
            previous = "none";
        }

        // Get the parcelable ChallengeDetailBean
        Bundle data = getIntent().getExtras();
        challengeDetail = data.getParcelable("challenge");

        // Get the header TextView for the main challenge header
        TextView challengeHeaderText = (TextView)findViewById(R.id.challengeHeader);
        String headerText = getString(R.string.challenge_notification_duration);

        // Format the text for the header and set the title
        String formattedHeader = String.format(headerText, challengeDetail.getChallenge().getChallengeGoal() / 60 + " min");
        challengeHeaderText.setText(formattedHeader);

        // Get the TextView for the opponent name
        final TextView opponentName = (TextView)findViewById(R.id.opponentName);

        // Set the number of points for the challenge
        final TextView resultRewardNumber = (TextView)findViewById(R.id.resultRewardNumber);
        resultRewardNumber.setText(String.valueOf(challengeDetail.getChallenge().getPoints()));

        // Make sure the opponent name is valid, if not get the opponent again
        if(challengeDetail.getOpponent().getName().equals(UserBean.DEFAULT_NAME)) {
            Task.callInBackground(new Callable<User>() {
                @Override
                public User call() throws Exception {
                    // Get the user from the server/database
                    User actualUser = SyncHelper.getUser(challengeDetail.getOpponent().getId());
                    return actualUser;
                }
            }).continueWith(new Continuation<User, Void>() {
                @Override
                public Void then(Task<User> userTask) throws Exception {
                    // Get the user from the task and set the user bean
                    User foundUser = userTask.getResult();
                    UserBean user = new UserBean(foundUser);

                    // Set the opponent's name and profile picture
                    opponentName.setText(user.getShortName());
                    setOpponentPicture(user.getProfilePictureUrl());
                    challengeDetail.setOpponent(user);
                    return null;
                }
            }, Task.UI_THREAD_EXECUTOR);
        } else {
            // Set the opponent name
            opponentName.setText(challengeDetail.getOpponent().getShortName());
            setOpponentPicture(challengeDetail.getOpponent().getProfilePictureUrl());
        }

        // Set the player name
        TextView playerName = (TextView)findViewById(R.id.playerName);
        playerName.setText(challengeDetail.getPlayer().getShortName());

        // Get the player's track and set the text views
        TrackSummaryBean playerTrack = challengeDetail.getPlayerTrack();
        Boolean playerComplete = false;
        if(playerTrack != null) {
            playerComplete = true;

            String formattedDistance = Format.twoDp(UnitConversion.miles(playerTrack.getDistanceRan()));
            setTextViewAndColor(R.id.playerDistanceText, "#ffffff", formattedDistance);
            setTextViewAndColor(R.id.playerPaceText, "#ffffff", Format.twoDp(UnitConversion.minutesPerMile(playerTrack.getTopSpeed())) + "");
            setTextViewAndColor(R.id.playerClimbText, "#ffffff", Format.zeroDp(UnitConversion.feet(playerTrack.getTotalUp())) + "");
        }

        // Get the opponent's track and set the text view
        TrackSummaryBean opponentTrack = challengeDetail.getOpponentTrack();
        Boolean opponentComplete = false;
        if(opponentTrack != null) {
            opponentComplete = true;

            String formattedDistance =  Format.twoDp(UnitConversion.miles(opponentTrack.getDistanceRan()));
            setTextViewAndColor(R.id.opponentDistanceText, "#ffffff", formattedDistance);
            setTextViewAndColor(R.id.opponentPaceText, "#ffffff", Format.twoDp(UnitConversion.minutesPerMile(opponentTrack.getTopSpeed())) + "");
            setTextViewAndColor(R.id.opponentClimbText, "#ffffff", Format.zeroDp(UnitConversion.feet(opponentTrack.getTotalUp())) + "");
        }

        if(playerComplete && opponentComplete) {
            // Get textviews for the results
            TextView resultName = (TextView)findViewById(R.id.resultName);
            ImageView resultPic = (ImageView)findViewById(R.id.resultPic);

            // If the player won
            if(playerTrack.getDistanceRan() > opponentTrack.getDistanceRan()) {
                // Get the text that tells the user to claim points by tapping and make it visible
                final TextView pointsText = (TextView)findViewById(R.id.claimText);
                pointsText.setVisibility(View.VISIBLE);

                // Get the points box and add a click listener
                final ImageView pointsClaimer = (ImageView)findViewById(R.id.resultBox);
                pointsClaimer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Remove the listener to stop the button being pressed repeatedly
                        pointsClaimer.setOnClickListener(null);
                        pointsText.setVisibility(View.INVISIBLE);

                        // Get the parent layout's location
                        final ViewGroup layout = (ViewGroup)findViewById(R.id.relativeLayout);
                        int[] parent_location = new int[2];
                        layout.getLocationOnScreen(parent_location);

                        // Get the points box's position on screen
                        int[] location = new int[2];
                        v.getLocationOnScreen(location);
                        location[0] = location[0] - parent_location[0] + v.getMeasuredWidth()/2;
                        location[1] = location[1] - parent_location[1] + v.getMeasuredHeight()/2;

                        // Number of coins/particles
                        int coins = 25;

                        // Number of points per coin
                        final double pointsPerCoin = (double)challengeDetail.getChallenge().getPoints() / coins;
                        // List of particles based on the number of coins
                        List<ParticleAnimator.Particle> particles = new ArrayList<ParticleAnimator.Particle>(coins);
                        // For each coin, set the drawable, location, add it to layout and to the list of particles
                        for (int i=0; i<coins; i++) {
                            ImageView coin = new ImageView(ChallengeSummaryActivity.this);
                            coin.setImageDrawable(getResources().getDrawable(R.drawable.icon_coin_small));
                            coin.setX(location[0]);
                            coin.setY(location[1]);
                            layout.addView(coin);
                            // Adds a particle, needs an image view, x-velocity and y-velocity - for initial direction
                            particles.add(new ParticleAnimator.Particle(coin, new Vector2D(-500+Math.random()*1000, -500+Math.random()*1000)));
                        }
                        // Add a points counter which increments the number of points - atomic = thread-safe double
                        final AtomicDouble pointsCounter = new AtomicDouble(0.0);
                        // Get the points textview location relative to the layout location
                        int[] target_location = new int[2];
                        pointsView.getLocationOnScreen(target_location);
                        target_location[0] = target_location[0] - parent_location[0];
                        target_location[1] = target_location[1] - parent_location[1];

                        // Start a new animator with the particles, vector for the position, attraction(scalar acceleration) and delay before attraction
                        coinAnimator = new ParticleAnimator(particles, new Vector2D(target_location[0], target_location[1]), 99999, 500);
                        // Set the particle listener for when the target is reached
                        coinAnimator.setParticleListener(new ParticleAnimator.ParticleListener() {
                            @Override
                            public void onTargetReached(ParticleAnimator.Particle particle, int particlesAlive) {
                                // Set the number of points in the textview
                                final User player = User.get(AccessToken.get().getUserId());
                                pointsView.setText(String.valueOf(player.getPoints() + (int) pointsCounter.addAndGet(pointsPerCoin)));
                                // Remove the coin
                                layout.removeView(particle.getView());
                                // When all particles are dead update points on the server
                                if (particlesAlive == 0) {
                                    try {
                                        PointsHelper.getInstance(layout.getContext()).awardPoints("RACE WIN", ("[" + challengeDetail.getChallenge().getChallengeId() + "," + challengeDetail.getChallenge().getDeviceId() + "]"), "ChallengeSummaryActivity.java", challengeDetail.getChallenge().getPoints());
                                    } catch (Transaction.InsufficientFundsException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                        // Start the animation
                        coinAnimator.start();
                    }
                });
                // Change the opponent's distance graph
                setGraph(R.id.opponentDistanceGraph, (float)opponentTrack.getDistanceRan(), (float)playerTrack.getDistanceRan(), 0.34f);
                // Set the result name and text to the player
				resultName.setText(challengeDetail.getPlayer().getShortName());
                Picasso.with(this).load(challengeDetail.getPlayer().getProfilePictureUrl()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(resultPic);

            } else {
                // Change the player's distance graph
                setGraph(R.id.playerDistanceGraph, (float)playerTrack.getDistanceRan(), (float)opponentTrack.getDistanceRan(), 0.34f);
                // Set the result name and text to the opponent
				resultName.setText(challengeDetail.getOpponent().getShortName());
                Picasso.with(this).load(challengeDetail.getOpponent().getProfilePictureUrl()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(resultPic);

            }

            if(playerTrack.getTopSpeed() > opponentTrack.getTopSpeed()) {
                // Change the opponent's top speed graph
                setGraph(R.id.opponentPaceGraph, opponentTrack.getTopSpeed(), playerTrack.getTopSpeed(), 0.25f);
            } else {
                // Change the player's top speed graph
                setGraph(R.id.playerPaceGraph, playerTrack.getTopSpeed(), opponentTrack.getTopSpeed(), 0.25f);
            }

            log.info("Player up is " + playerTrack.getTotalUp() + ", opponent up is " + opponentTrack.getTotalUp());
            if(playerTrack.getTotalUp() > opponentTrack.getTotalUp()) {
                // Change the opponent's climb graph
                setGraph(R.id.opponentClimbGraph, opponentTrack.getTotalUp(), playerTrack.getTotalUp(), 0.25f);
            } else {
                // Change the player's climb graph
                setGraph(R.id.playerClimbGraph, playerTrack.getTotalUp(), opponentTrack.getTotalUp(), 0.25f);
            }
        }

        // Set the player's names on all the necessary locations
        final ImageView playerPic = (ImageView)findViewById(R.id.playerProfilePic);
        Picasso.with(this).load(challengeDetail.getPlayer().getProfilePictureUrl()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(playerPic);

        final ImageView playerDistancePic = (ImageView)findViewById(R.id.playerDistancePic);
        Picasso.with(this).load(challengeDetail.getPlayer().getProfilePictureUrl()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(playerDistancePic);

        final ImageView playerClimbPic = (ImageView)findViewById(R.id.playerClimbPic);
        Picasso.with(this).load(challengeDetail.getPlayer().getProfilePictureUrl()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(playerClimbPic);

        final ImageView playerPacePic = (ImageView)findViewById(R.id.playerPacePic);
        Picasso.with(this).load(challengeDetail.getPlayer().getProfilePictureUrl()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(playerPacePic);
    }

    /**
     * Sets all the opponent's pictures in all the locations
     * @param url for the opponent's picture
     */
    public void setOpponentPicture(String url) {
        ImageView opponentPic = (ImageView)findViewById(R.id.opponentProfilePic);
        Picasso.with(ChallengeSummaryActivity.this).load(url).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(opponentPic);

        ImageView opponentDistancePic = (ImageView)findViewById(R.id.opponentDistancePic);
        Picasso.with(ChallengeSummaryActivity.this).load(url).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(opponentDistancePic);

        ImageView opponentClimbPic = (ImageView)findViewById(R.id.opponentClimbPic);
        Picasso.with(ChallengeSummaryActivity.this).load(url).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(opponentClimbPic);

        ImageView opponentPacePic = (ImageView)findViewById(R.id.opponentPacePic);
        Picasso.with(ChallengeSummaryActivity.this).load(url).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(opponentPacePic);
    }

    /**
     * When the Return button is pressed
     * @param view
     */
    public void onRaceNow(View view) {
        // Initialise the intent for the home activity
        Intent homeIntent = new Intent(this, HomeActivity_.class);
        // Clear the history so the player can't go back
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        // Make sure the tutorial isn't displayed when going to the home screen
        homeIntent.putExtra("displayTutorial", false);
        // Start the activity
        startActivity(homeIntent);
    }

    /**
     * Sets the text view's color and text
     * @param textViewId
     * @param color of text using a string
     * @param textViewString
     */
    public void setTextViewAndColor(int textViewId, String color, String textViewString) {
        // Find the text view
        TextView textView = (TextView)findViewById(textViewId);
        // Set the color and text
        textView.setTextColor(Color.parseColor(color));
        textView.setText(textViewString);
    }

    /**
     * Calculates the scale for the graph
     * @param arg1 the "losing" value
     * @param arg2 the "winning" value
     * @param minScale the minimum scale so that text can still be seen
     * @return
     */
    public float getScaleFactor(float arg1, float arg2, float minScale) {
        // Set the scale as the minimum
        float scale = minScale;
        // Calculate the scale
        if(arg2 > 0) {
            scale = (arg1 / arg2);
        }
        // Checks to make sure the scale isn't out of bounds
        if(scale < minScale) scale = minScale;
        if(scale > 1) scale = 1;
        if(arg1 == arg2) scale = 1;
        return scale;
    }

    /**
     * Sets the size of the graph
     * @param imageViewId to find the image view for the graph
     * @param losingVal losing value for the scale
     * @param winningVal winning value for the scale
     * @param minScale the minimum scale so that text can still be seen when scaling the graph
     */
    public void setGraph(int imageViewId, float losingVal, float winningVal, float minScale) {
        // Find the graph
        ImageView graphView = (ImageView)findViewById(imageViewId);
        // Get the current height
        float currentHeightPx = graphView.getLayoutParams().height;
        // Get the scale factor
        float scaleFactor = getScaleFactor(losingVal, winningVal, minScale);
        // Scale the height and re-set it
        float scaledHeightInPx = currentHeightPx * scaleFactor;
        graphView.getLayoutParams().height = (int)scaledHeightInPx;
    }

    /**
     * Overriding the back button being pressed to go back to the home screen
     */
    @Override
    public void onBackPressed() {
        // Initialise the intent for the home activity
        Intent homeIntent = new Intent(this, HomeActivity_.class);
        // Clear the history so the player can't go back
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        // Make sure the tutorial isn't displayed when going to the home screen
        homeIntent.putExtra("displayTutorial", false);
        // Start the activity
        startActivity(homeIntent);
    }
}
