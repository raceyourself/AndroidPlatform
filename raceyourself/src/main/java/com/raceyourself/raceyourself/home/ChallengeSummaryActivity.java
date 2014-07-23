package com.raceyourself.raceyourself.home;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ScaleDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.User;
import com.raceyourself.platform.utils.Format;
import com.raceyourself.platform.utils.UnitConversion;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.home.feed.ChallengeDetailBean;
import com.raceyourself.raceyourself.home.feed.TrackSummaryBean;
import com.squareup.picasso.Picasso;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_challenge_summary);

        getActionBar().hide();

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
            opponentName.setText(challengeDetail.getOpponent().getShortName());
            setOpponentPicture(challengeDetail.getOpponent().getProfilePictureUrl());
        }

        TextView playerName = (TextView)findViewById(R.id.playerName);
        playerName.setText(challengeDetail.getPlayer().getShortName());

        TrackSummaryBean playerTrack = challengeDetail.getPlayerTrack();
        Boolean playerComplete = false;
        if(playerTrack != null) {
            playerComplete = true;

            String formattedDistance = Format.twoDp(UnitConversion.miles(playerTrack.getDistanceRan()));
            setTextViewAndColor(R.id.playerDistanceText, "#ffffff", formattedDistance);
            setTextViewAndColor(R.id.playerPaceText, "#ffffff", Format.twoDp(UnitConversion.minutesPerMile(playerTrack.getTopSpeed())) + "");
            setTextViewAndColor(R.id.playerClimbText, "#ffffff", Format.twoDp(UnitConversion.miles(playerTrack.getTotalUp())) + "");
        }
        TrackSummaryBean opponentTrack = challengeDetail.getOpponentTrack();
        Boolean opponentComplete = false;
        if(opponentTrack != null) {
            opponentComplete = true;

            String formattedDistance =  Format.twoDp(UnitConversion.miles(opponentTrack.getDistanceRan()));
            log.info("Regular distance is " + opponentTrack.getDistanceRan() + ", and formatted distance is " + formattedDistance);
            setTextViewAndColor(R.id.opponentDistanceText, "#ffffff", formattedDistance);
            setTextViewAndColor(R.id.opponentPaceText, "#ffffff", Format.twoDp(UnitConversion.minutesPerMile(opponentTrack.getTopSpeed())) + "");
            setTextViewAndColor(R.id.opponentClimbText, "#ffffff", Format.twoDp(UnitConversion.miles(opponentTrack.getTotalUp())) + "");
        }

        if(playerComplete && opponentComplete) {

            TextView resultName = (TextView)findViewById(R.id.resultName);
            ImageView resultPic = (ImageView)findViewById(R.id.resultPic);

            if(playerTrack.getDistanceRan() > opponentTrack.getDistanceRan()) {
                ImageView opponentDistanceGraph = (ImageView)findViewById(R.id.opponentDistanceGraph);
                float currentHeightPx = opponentDistanceGraph.getLayoutParams().height;
                float scaleFactor = getScaleFactor((float)opponentTrack.getDistanceRan(), (float)playerTrack.getDistanceRan(), 0.34f);
                float scaledHeightInPx = currentHeightPx * scaleFactor;
                log.info("current height is " + currentHeightPx + ", new height as float is " + scaledHeightInPx + ", new height as int is " + (int)scaledHeightInPx);
                opponentDistanceGraph.getLayoutParams().height = (int)scaledHeightInPx;
				resultName.setText(challengeDetail.getPlayer().getShortName());
                Picasso.with(this).load(challengeDetail.getPlayer().getProfilePictureUrl()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(resultPic);

            } else {
                ImageView playerDistanceGraph = (ImageView)findViewById(R.id.playerDistanceGraph);
                float currentHeightPx = playerDistanceGraph.getLayoutParams().height;
                float scaleFactor = getScaleFactor((float)playerTrack.getDistanceRan(), (float)opponentTrack.getDistanceRan(), 0.34f);
                float scaledHeightInPx = currentHeightPx * scaleFactor;
                log.info("current height is " + currentHeightPx + ", new height as float is " + scaledHeightInPx + ", new height as int is " + (int)scaledHeightInPx);
                playerDistanceGraph.getLayoutParams().height = (int)scaledHeightInPx;
				resultName.setText(challengeDetail.getOpponent().getShortName());
                Picasso.with(this).load(challengeDetail.getOpponent().getProfilePictureUrl()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(resultPic);

            }

            if(playerTrack.getTopSpeed() > opponentTrack.getTopSpeed()) {
                ImageView opponentPaceGraph = (ImageView)findViewById(R.id.opponentPaceGraph);
                float currentHeightPx = opponentPaceGraph.getLayoutParams().height;
                float scaleFactor = getScaleFactor(playerTrack.getTopSpeed(), opponentTrack.getTopSpeed(), 0.25f);
                float scaledHeightInPx = currentHeightPx * scaleFactor;
                log.info("current height is " + currentHeightPx + ", new height as float is " + scaledHeightInPx + ", new height as int is " + (int)scaledHeightInPx);
                opponentPaceGraph.getLayoutParams().height = (int)scaledHeightInPx;
            } else {
                ImageView playerPaceGraph = (ImageView)findViewById(R.id.playerPaceGraph);
                float currentHeightPx = playerPaceGraph.getLayoutParams().height;
                float scaleFactor = getScaleFactor(opponentTrack.getTopSpeed(), playerTrack.getTopSpeed(), 0.25f);
                float scaledHeightInPx = currentHeightPx * scaleFactor;
                log.info("current height is " + currentHeightPx + ", new height as float is " + scaledHeightInPx + ", new height as int is " + (int)scaledHeightInPx);
                playerPaceGraph.getLayoutParams().height = (int)scaledHeightInPx;
            }

            log.info("Player up is " + playerTrack.getTotalUp() + ", opponent up is " + opponentTrack.getTotalUp());
            if(playerTrack.getTotalUp() > opponentTrack.getTotalUp()) {
                ImageView opponentClimbGraph = (ImageView)findViewById(R.id.opponentClimbGraph);
                float currentHeightPx = opponentClimbGraph.getLayoutParams().height;
                float scaleFactor = getScaleFactor(playerTrack.getTotalUp(), opponentTrack.getTotalUp(), 0.25f);
                float scaledHeightInPx = currentHeightPx * scaleFactor;
                log.info("current height is " + currentHeightPx + ", new height as float is " + scaledHeightInPx + ", new height as int is " + (int)scaledHeightInPx);
                opponentClimbGraph.getLayoutParams().height = (int)scaledHeightInPx;
            } else {
                ImageView playerClimbGraph = (ImageView)findViewById(R.id.playerClimbGraph);
                float currentHeightPx = playerClimbGraph.getLayoutParams().height;
                float scaleFactor = getScaleFactor(opponentTrack.getTotalUp(), playerTrack.getTotalUp(), 0.25f);
                float scaledHeightInPx = currentHeightPx * scaleFactor;
                log.info("current height is " + currentHeightPx + ", new height as float is " + scaledHeightInPx + ", new height as int is " + (int)scaledHeightInPx);
                playerClimbGraph.getLayoutParams().height = (int)scaledHeightInPx;
            }
        }

        final ImageView playerPic = (ImageView)findViewById(R.id.playerProfilePic);
        Picasso.with(this).load(challengeDetail.getPlayer().getProfilePictureUrl()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(playerPic);

        final ImageView playerDistancePic = (ImageView)findViewById(R.id.playerDistancePic);
        Picasso.with(this).load(challengeDetail.getPlayer().getProfilePictureUrl()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(playerDistancePic);

        final ImageView playerClimbPic = (ImageView)findViewById(R.id.playerClimbPic);
        Picasso.with(this).load(challengeDetail.getPlayer().getProfilePictureUrl()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(playerClimbPic);

        final ImageView playerPacePic = (ImageView)findViewById(R.id.playerPacePic);
        Picasso.with(this).load(challengeDetail.getPlayer().getProfilePictureUrl()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(playerPacePic);
    }

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

    public void onRaceNow(View view) {
        Intent homeIntent = new Intent(this, HomeActivity_.class);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
    }

    public void setTextViewAndColor(int textViewId, String color, String textViewString) {
        TextView textView = (TextView)findViewById(textViewId);
        textView.setTextColor(Color.parseColor(color));
        textView.setText(textViewString);
    }

    public float getScaleFactor(float arg1, float arg2, float minScale) {
        float scale = minScale;
        if(arg2 > 0) {
            scale = (arg1 / arg2);
        }
        if(scale < minScale) scale = minScale;
        if(scale > 1) scale = 1;
        if(arg1 == arg2) scale = 1;
        return scale;
    }

    @Override
    public void onBackPressed() {
        Intent homeActivity = new Intent(this, HomeActivity_.class);
        homeActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeActivity);

    }
}
