package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.Challenge;
import com.raceyourself.platform.models.Track;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.base.util.StringFormattingUtils;
import com.raceyourself.raceyourself.game.GameConfiguration;
import com.raceyourself.raceyourself.home.UserBean;
import com.squareup.picasso.Picasso;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.joda.time.DateTime;
import org.joda.time.Period;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 15/07/2014.
 */
@Slf4j
@EViewGroup(R.layout.fragment_friend_activity)
public class ActivityTitleView extends LinearLayout {

    private Context context;

    @ViewById
    ImageView fromProfilePic;

    @ViewById
    ImageView fromRankIcon;

    @ViewById
    TextView fromName;

    @ViewById
    ImageView toProfilePic;

    @ViewById
    ImageView toRankIcon;

    @ViewById
    TextView toName;

    @ViewById
    TextView raceOutcome;

    public ActivityTitleView(Context context) {
        super(context);
        this.context = context;
    }

    public ActivityTitleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        this.context = context;
    }

    public ActivityTitleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void bind(ChallengeNotificationBean notif) {
        if (notif.getOutcome() == null) retrieveUsers(notif);
        else drawTitle(notif);
    }

    @Background
    void retrieveUsers(ChallengeNotificationBean challengeNotificationBean) {
        User fromUser = SyncHelper.getUser(challengeNotificationBean.getFrom().getId());
        if (fromUser != null) {
            UserBean fromBean = challengeNotificationBean.getFrom();
            fromBean.setName(fromUser.getName());
            fromBean.setShortName(StringFormattingUtils.getForenameAndInitial(fromUser.getName()));
            fromBean.setProfilePictureUrl(fromUser.getImage());
            fromBean.setRank(fromUser.getRank());
        }
        User toUser = SyncHelper.getUser(challengeNotificationBean.getTo().getId());
        if (toUser != null) {
            UserBean toBean = challengeNotificationBean.getTo();
            toBean.setName(toUser.getName());
            toBean.setShortName(StringFormattingUtils.getForenameAndInitial(toUser.getName()));
            toBean.setProfilePictureUrl(toUser.getImage());
            toBean.setRank(toUser.getRank());
        }

        // Draw user details before we have the final outcome as track download may take a while
        drawTitle(challengeNotificationBean);

        Challenge challenge = SyncHelper.getChallenge(challengeNotificationBean.getChallenge().getDeviceId(), challengeNotificationBean.getChallenge().getChallengeId());
        GameConfiguration game = new GameConfiguration.GameStrategyBuilder(GameConfiguration.GameType.TIME_CHALLENGE).targetTime(challengeNotificationBean.getChallenge().getChallengeGoal() * 1000).countdown(2999).build();

        TrackSummaryBean fromTrack = null;
        TrackSummaryBean toTrack = null;
        for(Challenge.ChallengeAttempt attempt : challenge.getAttempts()) {
            if(attempt.user_id == fromUser.getId()) {
                Track track = SyncHelper.getTrack(attempt.track_device_id, attempt.track_id);
                fromTrack = new TrackSummaryBean(track, game);
            }
            if(attempt.user_id == toUser.getId()) {
                Track track = SyncHelper.getTrack(attempt.track_device_id, attempt.track_id);
                toTrack = new TrackSummaryBean(track, game);
            }
            if (fromTrack != null && toTrack != null) break;
        }

        if (fromTrack == null || toTrack == null) {
            // Do nothing, will retry
        } else if (fromTrack.getDistanceRan() > toTrack.getDistanceRan()) {
            challengeNotificationBean.setOutcome("Won against ");
        } else if (fromTrack.getDistanceRan() < toTrack.getDistanceRan()) {
            challengeNotificationBean.setOutcome("Lost to ");
        } else {
            challengeNotificationBean.setOutcome("Tied with ");
        }
        drawTitle(challengeNotificationBean);
    }

    // TODO use inheritance to avoid having both these methods below here together...

    @UiThread
    void drawTitle(ChallengeNotificationBean notif) {
        drawUserDetails(notif.getFrom(), notif, fromName, fromProfilePic, fromRankIcon);
        drawUserDetails(notif.getTo(), notif, toName, toProfilePic, toRankIcon);

        if (notif.getOutcome() != null) raceOutcome.setText(notif.getOutcome());
        else raceOutcome.setText("Challenged ");
    }

    private void drawUserDetails(UserBean user, ChallengeNotificationBean notif,
                                 TextView name, ImageView pic, ImageView rank) {
        if (user != null) {
            name.setText(user.getName());
            if (user.getRank() != null) {
                rank.setImageDrawable(getResources().getDrawable(user.getRankDrawable()));
                rank.setVisibility(VISIBLE);
            } else {
                rank.setVisibility(INVISIBLE);
            }

            Picasso.with(context)
                    .load(user.getProfilePictureUrl())
                    .placeholder(R.drawable.default_profile_pic)
                    .transform(new PictureUtils.CropCircle())
                    .into(pic);
        } else {
            // Handle deleted user or no network connectivity
            name.setText("<No network>");
            rank.setVisibility(INVISIBLE);

            Picasso.with(context)
                    .load((String)null)
                    .placeholder(R.drawable.default_profile_pic)
                    .transform(new PictureUtils.CropCircle())
                    .into(pic);
        }

    }
}
