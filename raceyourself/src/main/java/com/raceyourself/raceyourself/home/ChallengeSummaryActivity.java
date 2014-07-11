package com.raceyourself.raceyourself.home;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.base.util.StringFormattingUtils;
import com.raceyourself.raceyourself.game.GameActivity;
import com.squareup.picasso.Picasso;

import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class ChallengeSummaryActivity extends Activity {

    ChallengeDetailBean challengeDetail;

    String previous = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_challenge_summary);

        if(getIntent().hasExtra("previous")) {
            previous = getIntent().getStringExtra("previous");
        } else {
            previous = "none";
        }

        Bundle data = getIntent().getExtras();
        challengeDetail = data.getParcelable("challenge");
        log.info("ChallengeDetail: user 1 is " + challengeDetail.getPlayer().getName());
        log.info("ChallengeDetail: user 2 is " + challengeDetail.getOpponent().getName());
        log.info("ChallengeDetail: title is " + challengeDetail.getTitle());
        log.info("ChallengeDetail: points is " + challengeDetail.getPoints());

        TextView challengeHeaderText = (TextView)findViewById(R.id.challengeHeader);
        String headerText = getString(R.string.challenge_notification_duration);

        String formattedHeader = String.format(headerText, challengeDetail.getChallenge().getChallengeGoal() / 60);
        challengeHeaderText.setText(formattedHeader);
        final TextView opponentName = (TextView)findViewById(R.id.opponentName);
        if(challengeDetail.getOpponent().getName().equals(UserBean.DEFAULT_NAME)) {
            Task.callInBackground(new Callable<User>() {
                @Override
                public User call() throws Exception {
                    User actualUser = SyncHelper.getUser(challengeDetail.getOpponent().getId());
                    return actualUser;
                }
            }).continueWith(new Continuation<User, Void>() {
                @Override
                public Void then(Task<User> userTask) throws Exception {
                    User foundUser = userTask.getResult();
                    UserBean user = new UserBean(foundUser);

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

            String formattedDistance = StringFormattingUtils.getDistanceInKmString(playerTrack.getDistanceRan());
            setTextViewAndColor(R.id.playerDistance, "#269b47", formattedDistance + "KM");
            setTextViewAndColor(R.id.playerAveragePace, "#269b47", playerTrack.getAveragePace() + "");
            setTextViewAndColor(R.id.playerTopSpeed, "#269b47", playerTrack.getTopSpeed() + "");
            setTextViewAndColor(R.id.playerTotalUp, "#269b47", playerTrack.getTotalUp() + "");
            setTextViewAndColor(R.id.playerTotalDown, "#269b47", playerTrack.getTotalDown() + "");

            Button raceNowBtn = (Button)findViewById(R.id.raceNowBtn);
            raceNowBtn.setVisibility(View.INVISIBLE);
            Button raceLaterBtn = (Button)findViewById(R.id.raceLaterBtn);
            raceLaterBtn.setVisibility(View.INVISIBLE);
        }
        TrackSummaryBean opponentTrack = challengeDetail.getOpponentTrack();
        Boolean opponentComplete = false;
        if(opponentTrack != null) {
            opponentComplete = true;

            String formattedDistance = StringFormattingUtils.getDistanceInKmString(opponentTrack.getDistanceRan());
            setTextViewAndColor(R.id.opponentDistance, "#269b47", formattedDistance + "KM");
            setTextViewAndColor(R.id.opponentAveragePace, "#269b47", opponentTrack.getAveragePace() + "");
            setTextViewAndColor(R.id.opponentTopSpeed, "#269b47", opponentTrack.getTopSpeed() + "");
            setTextViewAndColor(R.id.opponentTotalUp, "#269b47", opponentTrack.getTotalUp() + "");
            setTextViewAndColor(R.id.opponentTotalDown, "#269b47", opponentTrack.getTotalDown() + "");
        }

        if(playerComplete && opponentComplete) {

            if(playerTrack.getDistanceRan() > opponentTrack.getDistanceRan()) {
                TextView opponentDistance = (TextView)findViewById(R.id.opponentDistance);
                challengeHeaderText.setText("YOU WON");
                opponentDistance.setTextColor(Color.parseColor("#e31f26"));
            } else {
                TextView playerDistance = (TextView)findViewById(R.id.playerDistance);
                playerDistance.setTextColor(Color.parseColor("#e31f26"));
                challengeHeaderText.setText("YOU LOST");
                ImageView headerBox = (ImageView)findViewById(R.id.titleBox);
                headerBox.setImageDrawable(getResources().getDrawable(R.drawable.red_box));
                FrameLayout rewardIcon = (FrameLayout)findViewById(R.id.reward_icon);
                rewardIcon.setVisibility(View.INVISIBLE);
                TextView rewardText = (TextView)findViewById(R.id.rewardPoints);
                rewardText.setVisibility(View.INVISIBLE);
            }

            if(playerTrack.getAveragePace() > opponentTrack.getAveragePace()) {
                TextView opponentAveragePace = (TextView)findViewById(R.id.opponentAveragePace);
                opponentAveragePace.setTextColor(Color.parseColor("#e31f26"));
            } else {
                TextView playerAveragePace = (TextView)findViewById(R.id.playerAveragePace);
                playerAveragePace.setTextColor(Color.parseColor("#e31f26"));
            }

            if(playerTrack.getTopSpeed() > opponentTrack.getTopSpeed()) {
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
        Intent gameIntent = new Intent(this, GameActivity.class);
        gameIntent.putExtra("challenge", challengeDetail);
        startActivity(gameIntent);
    }

    public void onRaceLaterClick(View view) {
        onBackPressed();
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
            Intent homeActivity = new Intent(this, HomeActivity.class);
            homeActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(homeActivity);
        }
    }

}
