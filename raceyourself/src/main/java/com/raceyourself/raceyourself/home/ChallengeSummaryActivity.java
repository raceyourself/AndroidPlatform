package com.raceyourself.raceyourself.home;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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

import org.apache.commons.lang3.StringUtils;

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
                    ImageView opponentPic = (ImageView)findViewById(R.id.opponentProfilePic);
                    Picasso.with(ChallengeSummaryActivity.this).load(user.getProfilePictureUrl()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(opponentPic);

                    challengeDetail.setOpponent(user);
                    return null;
                }
            }, Task.UI_THREAD_EXECUTOR);
        } else {
            opponentName.setText(challengeDetail.getOpponent().getShortName());
            ImageView opponentPic = (ImageView)findViewById(R.id.opponentProfilePic);
            Picasso.with(ChallengeSummaryActivity.this).load(challengeDetail.getOpponent().getProfilePictureUrl()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(opponentPic);
        }


        TextView playerName = (TextView)findViewById(R.id.playerName);
        playerName.setText(challengeDetail.getPlayer().getShortName());

        TrackSummaryBean playerTrack = challengeDetail.getPlayerTrack();
        Boolean playerComplete = false;
        if(playerTrack != null) {
            playerComplete = true;

            String formattedDistance = Format.twoDp(UnitConversion.miles(playerTrack.getDistanceRan()));
            setTextViewAndColor(R.id.playerDistance, "#269b47", formattedDistance + "KM");
            setTextViewAndColor(R.id.playerAveragePace, "#269b47", Format.oneDp(UnitConversion.minutesPerMile(playerTrack.getAveragePace())));
            setTextViewAndColor(R.id.playerTopSpeed, "#269b47", Format.oneDp(UnitConversion.minutesPerMile(playerTrack.getTopSpeed())));
            setTextViewAndColor(R.id.playerTotalUp, "#269b47", playerTrack.getTotalUp() + "");
            setTextViewAndColor(R.id.playerTotalDown, "#269b47", playerTrack.getTotalDown() + "");
        }
        TrackSummaryBean opponentTrack = challengeDetail.getOpponentTrack();
        Boolean opponentComplete = false;
        if(opponentTrack != null) {
            opponentComplete = true;

            String formattedDistance =  Format.twoDp(UnitConversion.miles(opponentTrack.getDistanceRan()));
            setTextViewAndColor(R.id.opponentDistance, "#269b47", formattedDistance + "KM");
            setTextViewAndColor(R.id.opponentAveragePace, "#269b47", Format.oneDp(UnitConversion.minutesPerMile(opponentTrack.getAveragePace())));
            setTextViewAndColor(R.id.opponentTopSpeed, "#269b47", Format.oneDp(UnitConversion.minutesPerMile(opponentTrack.getTopSpeed())));
            setTextViewAndColor(R.id.opponentTotalUp, "#269b47", opponentTrack.getTotalUp() + "");
            setTextViewAndColor(R.id.opponentTotalDown, "#269b47", opponentTrack.getTotalDown() + "");
        }

        if(playerComplete && opponentComplete) {

            TextView resultName = (TextView)findViewById(R.id.resultName);
            ImageView resultPic = (ImageView)findViewById(R.id.resultPic);

            if(playerTrack.getDistanceRan() > opponentTrack.getDistanceRan()) {
                TextView opponentDistance = (TextView)findViewById(R.id.opponentDistance);
                opponentDistance.setTextColor(Color.parseColor("#e31f26"));
                resultName.setText(StringUtils.abbreviate(challengeDetail.getPlayer().getShortName(),12));
                Picasso.with(this).load(challengeDetail.getPlayer().getProfilePictureUrl()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(resultPic);
            } else {
                TextView playerDistance = (TextView)findViewById(R.id.playerDistance);
                playerDistance.setTextColor(Color.parseColor("#e31f26"));
                resultName.setText(StringUtils.abbreviate(challengeDetail.getOpponent().getShortName(),12));
                Picasso.with(this).load(challengeDetail.getOpponent().getProfilePictureUrl()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(resultPic);
            }

            if(playerTrack.getAveragePace() < opponentTrack.getAveragePace()) {
                TextView opponentAveragePace = (TextView)findViewById(R.id.opponentAveragePace);
                opponentAveragePace.setTextColor(Color.parseColor("#e31f26"));
            } else {
                TextView playerAveragePace = (TextView)findViewById(R.id.playerAveragePace);
                playerAveragePace.setTextColor(Color.parseColor("#e31f26"));
            }

            if(playerTrack.getTopSpeed() < opponentTrack.getTopSpeed()) {
                TextView opponentTopSpeed = (TextView)findViewById(R.id.opponentTopSpeed);
                opponentTopSpeed.setTextColor(Color.parseColor("#e31f26"));
            } else {
                TextView playerTopSpeed = (TextView)findViewById(R.id.playerTopSpeed);
                playerTopSpeed.setTextColor(Color.parseColor("#e31f26"));
            }

            if(playerTrack.getTotalUp() > opponentTrack.getTotalUp()) {
                TextView opponentTotalUp = (TextView)findViewById(R.id.opponentTotalUp);
                opponentTotalUp.setTextColor(Color.parseColor("#e31f26"));
            } else {
                TextView playerTotalUp = (TextView)findViewById(R.id.playerTotalUp);
                playerTotalUp.setTextColor(Color.parseColor("#e31f26"));
            }

            if(playerTrack.getTotalDown() > opponentTrack.getTotalDown()) {
                TextView opponentTotalDown = (TextView)findViewById(R.id.opponentTotalDown);
                opponentTotalDown.setTextColor(Color.parseColor("#e31f26"));
            } else {
                TextView playerTotalDown = (TextView)findViewById(R.id.playerTotalDown);
                playerTotalDown.setTextColor(Color.parseColor("#e31f26"));
            }
        }

        final ImageView playerPic = (ImageView)findViewById(R.id.playerProfilePic);
        Picasso.with(this).load(challengeDetail.getPlayer().getProfilePictureUrl()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(playerPic);


    }

    public void onRaceNow(View view) {
        Intent homeIntent = new Intent(this, HomeActivity_.class);
        startActivity(homeIntent);
    }

    public void setTextViewAndColor(int textViewId, String color, String textViewString) {
        TextView textView = (TextView)findViewById(textViewId);
        textView.setTextColor(Color.parseColor(color));
        textView.setText(textViewString);
    }

    @Override
    public void onBackPressed() {
        if(previous.equalsIgnoreCase("home")) {
            super.onBackPressed();
        } else {
            Intent homeActivity = new Intent(this, HomeActivity_.class);
            homeActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(homeActivity);
        }
    }
}
