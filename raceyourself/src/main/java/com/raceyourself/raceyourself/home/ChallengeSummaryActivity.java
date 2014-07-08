package com.raceyourself.raceyourself.home;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.base.util.StringFormattingUtils;
import com.raceyourself.raceyourself.game.GameActivity;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class ChallengeSummaryActivity extends Activity {

    ChallengeDetailBean challenge;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_challenge_summary);

        Bundle data = getIntent().getExtras();
        ChallengeDetailBean challengeDetail = data.getParcelable("challenge");
        log.info("ChallengeDetail: user 1 is " + challengeDetail.getPlayer().getName());
        log.info("ChallengeDetail: user 2 is " + challengeDetail.getOpponent().getName());
        log.info("ChallengeDetail: challenge duration is " + challengeDetail.getChallenge().getChallengeGoal());
        log.info("ChallengeDetail: title is " + challengeDetail.getTitle());
        log.info("ChallengeDetail: points is " + challengeDetail.getPoints());

        TextView challengeHeaderText = (TextView)findViewById(R.id.challengeHeader);
        String headerText = getString(R.string.challenge_notification_duration);

        String formattedHeader = String.format(headerText, challengeDetail.getChallenge().getChallengeGoal());
        challengeHeaderText.setText(formattedHeader);
        TextView opponentName = (TextView)findViewById(R.id.opponentName);
        opponentName.setText(challengeDetail.getOpponent().getShortName());

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
                opponentDistance.setTextColor(Color.parseColor("#e31f26"));
            } else {
                TextView playerDistance = (TextView)findViewById(R.id.playerDistance);
                playerDistance.setTextColor(Color.parseColor("#e31f26"));
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
        Picasso.with(this).load(challengeDetail.getPlayer().getProfilePictureUrl()).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                playerPic.measure(0, 0);
                playerPic.setImageBitmap(PictureUtils.getRoundedBmp(bitmap, playerPic.getMeasuredWidth()));
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                log.error("Bitmap failed - player pic");
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
            }
        });

        final ImageView opponentPic = (ImageView)findViewById(R.id.playerProfilePic);

        Picasso.with(this).load(challengeDetail.getOpponent().getProfilePictureUrl()).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                opponentPic.measure(0, 0);
                opponentPic.setImageBitmap(PictureUtils.getRoundedBmp(bitmap, opponentPic.getMeasuredWidth()));
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                log.error("Bitmap failed - opponent pic");
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
            }
        });
    }

    public void onRaceNow(View view) {
        Intent gameIntent = new Intent(this, GameActivity.class);
        gameIntent.putExtra("challenge", challenge);
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

}
