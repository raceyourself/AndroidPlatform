package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.Challenge;
import com.raceyourself.platform.models.Track;
import com.raceyourself.platform.models.User;
import com.raceyourself.platform.utils.Format;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.game.GameActivity;
import com.raceyourself.raceyourself.home.UserBean;
import com.squareup.picasso.Picasso;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.UiThread;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 15/07/2014.
 */
@Slf4j
@EViewGroup(R.layout.activity_challenge_summary)
public class ChallengeDetailView extends ScrollView {
    private Context context;

    public ChallengeDetailView(Context context) {
        super(context);
        this.context = context;
    }

    public ChallengeDetailView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        this.context = context;
    }

    public ChallengeDetailView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void bind(ChallengeNotificationBean currentChallenge) {

        final ChallengeDetailBean activeChallengeFragment = new ChallengeDetailBean();

        findViewById(R.id.header_image).setVisibility(View.GONE);
        findViewById(R.id.header_text_container).setVisibility(View.GONE);

        UserBean opponentUserBean = currentChallenge.getUser();
        activeChallengeFragment.setOpponent(currentChallenge.getUser());
        User player = SyncHelper.getUser(AccessToken.get().getUserId());

        final UserBean playerBean = new UserBean(player);

        activeChallengeFragment.setPlayer(playerBean);
        activeChallengeFragment.setChallenge(currentChallenge.getChallenge());

        final TextView challengeHeaderText = (TextView) findViewById(R.id.challengeHeader);
        String headerText = context.getString(R.string.challenge_notification_duration);

        String formattedHeader = String.format(headerText,
                activeChallengeFragment.getChallenge().getDuration().getStandardMinutes());
        challengeHeaderText.setText(formattedHeader);

        resetTextViewsAndImages();
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.challenge_progress);
        progressBar.startAnimation(AnimationUtils.loadAnimation(context, R.anim.continuous_rotation_anim));

        retrieveChallengeDetail(activeChallengeFragment, playerBean);

        final ImageView playerImage = (ImageView) findViewById(R.id.playerProfilePic);
        Picasso.with(context)
                .load(playerBean.getProfilePictureUrl())
                .placeholder(R.drawable.default_profile_pic)
                .transform(new PictureUtils.CropCircle())
                .into(playerImage);

        final Button raceNowBtn = (Button) findViewById(R.id.raceNowBtn);
        raceNowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gameIntent = new Intent(context, GameActivity.class);
                gameIntent.putExtra("challenge", activeChallengeFragment);
                context.startActivity(gameIntent);
            }
        });
    }

    @Background
    void retrieveChallengeDetail(@NonNull ChallengeDetailBean activeChallengeFragment,
                                 @NonNull UserBean playerBean) {
        log.debug("retrieveChallengeDetail");

        ChallengeDetailBean challengeDetailBean = new ChallengeDetailBean();
        Challenge challenge = SyncHelper.getChallenge(
                activeChallengeFragment.getChallenge().getDeviceId(),
                activeChallengeFragment.getChallenge().getChallengeId());
        challengeDetailBean.setChallenge(new ChallengeBean(challenge));
        Boolean playerFound = false;
        Boolean opponentFound = false;
        if (challenge != null) {
            for (Challenge.ChallengeAttempt attempt : challenge.getAttempts()) {
                if (attempt.user_id == playerBean.getId() && !playerFound) {
                    playerFound = true;
                    Track playerTrack = SyncHelper.getTrack(attempt.track_device_id, attempt.track_id);
                    activeChallengeFragment.setPlayerTrack(new TrackSummaryBean(playerTrack));
                } else if (attempt.user_id == activeChallengeFragment.getOpponent().getId() && !opponentFound) {
                    opponentFound = true;
                    Track opponentTrack = SyncHelper.getTrack(attempt.track_device_id, attempt.track_id);
                    activeChallengeFragment.setOpponentTrack(new TrackSummaryBean(opponentTrack));
                }
                if (playerFound && opponentFound) {
                    break;
                }
            }
        }
        drawChallengeDetail(activeChallengeFragment);
    }

    @UiThread
    void drawChallengeDetail(@NonNull ChallengeDetailBean activeChallengeFragment) {
        log.debug("drawChallengeDetail");

        activeChallengeFragment.setPoints(20000);
        String durationText = context.getString(R.string.challenge_notification_duration);

        int duration = activeChallengeFragment.getChallenge().getDuration().toStandardMinutes().getMinutes();
        activeChallengeFragment.setTitle(String.format(durationText, duration + " mins"));

        TextView opponentName = (TextView) findViewById(R.id.opponentName);
        opponentName.setText(activeChallengeFragment.getOpponent().getShortName());

        TextView playerName = (TextView) findViewById(R.id.playerName);
        playerName.setText(activeChallengeFragment.getPlayer().getShortName());

        ImageView opponentPic = (ImageView) findViewById(R.id.opponentProfilePic);
        //log.debug("opponent picture is " + activeChallengeFragment.getOpponent().getProfilePictureUrl());
        Picasso.with(context)
                .load(activeChallengeFragment.getOpponent().getProfilePictureUrl())
                .placeholder(R.drawable.default_profile_pic)
                .transform(new PictureUtils.CropCircle())
                .into(opponentPic);

        TrackSummaryBean playerTrack = activeChallengeFragment.getPlayerTrack();
        boolean playerComplete = false;
        if(playerTrack != null) {
            playerComplete = true;

            String formattedDistance = Format.twoDp(playerTrack.getDistanceRan());
            setTextViewAndColor(R.id.playerDistance, "#269b47", formattedDistance + "KM");
            setTextViewAndColor(R.id.playerAveragePace, "#269b47", playerTrack.getAveragePace() + "");
            setTextViewAndColor(R.id.playerTopSpeed, "#269b47", playerTrack.getTopSpeed() + "");
            setTextViewAndColor(R.id.playerTotalUp, "#269b47", playerTrack.getTotalUp() + "");
            setTextViewAndColor(R.id.playerTotalDown, "#269b47", playerTrack.getTotalDown() + "");

            Button raceNowBtn = (Button) findViewById(R.id.raceNowBtn);
            raceNowBtn.setVisibility(View.GONE);
            Button raceLaterBtn = (Button) findViewById(R.id.raceLaterBtn);
            raceLaterBtn.setVisibility(View.GONE);
        }
        TrackSummaryBean opponentTrack = activeChallengeFragment.getOpponentTrack();
        boolean opponentComplete = false;
        if(opponentTrack != null) {
            opponentComplete = true;

            String formattedDistance = Format.twoDp(opponentTrack.getDistanceRan());
            setTextViewAndColor(R.id.opponentDistance, "#269b47", formattedDistance + "KM");
            setTextViewAndColor(R.id.opponentAveragePace, "#269b47", opponentTrack.getAveragePace() + "");
            setTextViewAndColor(R.id.opponentTopSpeed, "#269b47", opponentTrack.getTopSpeed() + "");
            setTextViewAndColor(R.id.opponentTotalUp, "#269b47", opponentTrack.getTotalUp() + "");
            setTextViewAndColor(R.id.opponentTotalDown, "#269b47", opponentTrack.getTotalDown() + "");
        }

        if(playerComplete && opponentComplete) {

            final TextView challengeHeaderText = (TextView) findViewById(R.id.challengeHeader);

            if(playerTrack.getDistanceRan() > opponentTrack.getDistanceRan()) {
                TextView opponentDistance = (TextView) findViewById(R.id.opponentDistance);
                opponentDistance.setTextColor(Color.parseColor("#e31f26"));
                challengeHeaderText.setText("YOU WON");
            } else {
                TextView playerDistance = (TextView) findViewById(R.id.playerDistance);
                playerDistance.setTextColor(Color.parseColor("#e31f26"));
                challengeHeaderText.setText("YOU LOST");
                ImageView headerBox = (ImageView) findViewById(R.id.titleBox);
                headerBox.setImageDrawable(context.getResources().getDrawable(R.drawable.red_box));
                FrameLayout rewardIcon = (FrameLayout) findViewById(R.id.reward_icon);
                rewardIcon.setVisibility(View.GONE);
                TextView rewardText = (TextView) findViewById(R.id.rewardPoints);
                rewardText.setVisibility(View.GONE);
            }

            if(playerTrack.getAveragePace() < opponentTrack.getAveragePace()) {
                TextView opponentAveragePace = (TextView) findViewById(R.id.opponentAveragePace);
                opponentAveragePace.setTextColor(Color.parseColor("#e31f26"));
            } else {
                TextView playerAveragePace = (TextView) findViewById(R.id.playerAveragePace);
                playerAveragePace.setTextColor(Color.parseColor("#e31f26"));
            }

            if(playerTrack.getTopSpeed() < opponentTrack.getTopSpeed()) {
                TextView opponentTopSpeed = (TextView) findViewById(R.id.opponentTopSpeed);
                opponentTopSpeed.setTextColor(Color.parseColor("#e31f26"));
            } else {
                TextView playerTopSpeed = (TextView) findViewById(R.id.playerTopSpeed);
                playerTopSpeed.setTextColor(Color.parseColor("#e31f26"));
            }

            if(playerTrack.getTotalUp() > opponentTrack.getTotalUp()) {
                TextView opponentTotalUp = (TextView) findViewById(R.id.opponentTotalUp);
                opponentTotalUp.setTextColor(Color.parseColor("#e31f26"));
            } else {
                TextView playerTotalUp = (TextView) findViewById(R.id.playerTotalUp);
                playerTotalUp.setTextColor(Color.parseColor("#e31f26"));
            }

            if(playerTrack.getTotalDown() > opponentTrack.getTotalDown()) {
                TextView opponentTotalDown = (TextView) findViewById(R.id.opponentTotalDown);
                opponentTotalDown.setTextColor(Color.parseColor("#e31f26"));
            } else {
                TextView playerTotalDown = (TextView) findViewById(R.id.playerTotalDown);
                playerTotalDown.setTextColor(Color.parseColor("#e31f26"));
            }
        }
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.challenge_progress);
        progressBar.clearAnimation();
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void resetTextViewsAndImages() {
        setTextViewAndColor(R.id.playerDistance, "#1f1f1f", context.getString(R.string.challenge_default_value));
        setTextViewAndColor(R.id.opponentDistance, "#1f1f1f", context.getString(R.string.challenge_default_value));
        setTextViewAndColor(R.id.playerDistance, "#1f1f1f", context.getString(R.string.challenge_default_value));
        setTextViewAndColor(R.id.playerAveragePace, "#1f1f1f", context.getString(R.string.challenge_default_value));
        setTextViewAndColor(R.id.opponentAveragePace, "#1f1f1f", context.getString(R.string.challenge_default_value));
        setTextViewAndColor(R.id.playerTopSpeed, "#1f1f1f", context.getString(R.string.challenge_default_value));
        setTextViewAndColor(R.id.opponentTopSpeed, "#1f1f1f", context.getString(R.string.challenge_default_value));
        setTextViewAndColor(R.id.playerTotalUp, "#1f1f1f", context.getString(R.string.challenge_default_value));
        setTextViewAndColor(R.id.opponentTotalUp, "#1f1f1f", context.getString(R.string.challenge_default_value));
        setTextViewAndColor(R.id.playerTotalDown, "#1f1f1f", context.getString(R.string.challenge_default_value));
        setTextViewAndColor(R.id.opponentTotalDown, "#1f1f1f", context.getString(R.string.challenge_default_value));

        findViewById(R.id.raceNowBtn).setVisibility(View.VISIBLE);
        findViewById(R.id.raceLaterBtn).setVisibility(View.VISIBLE);
        findViewById(R.id.reward_icon).setVisibility(View.VISIBLE);
        findViewById(R.id.rewardPoints).setVisibility(View.VISIBLE);

        ImageView headerBox = (ImageView) findViewById(R.id.titleBox);
        headerBox.setImageDrawable(context.getResources().getDrawable(R.drawable.green_box));
    }

    private void setTextViewAndColor(int textViewId, String color, String textViewString) {
        TextView textView = (TextView) findViewById(textViewId);
        textView.setTextColor(Color.parseColor(color));
        textView.setText(textViewString);
    }
}
