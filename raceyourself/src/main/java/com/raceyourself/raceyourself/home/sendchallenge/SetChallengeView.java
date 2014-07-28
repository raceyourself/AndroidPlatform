package com.raceyourself.raceyourself.home.sendchallenge;

import android.app.Activity;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.Challenge;
import com.raceyourself.platform.models.ChallengeNotification;
import com.raceyourself.platform.models.Event;
import com.raceyourself.platform.models.Notification;
import com.raceyourself.platform.models.Track;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.MobileApplication;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.PreviouslyRunDurationView;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.home.feed.HomeFeedFragment;
import com.raceyourself.raceyourself.home.UserBean;
import com.squareup.picasso.Picasso;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.SortedMap;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 08/07/2014.
 */
@Slf4j
@EViewGroup(R.layout.activity_select_duration)
public class SetChallengeView extends PreviouslyRunDurationView {

    private UserBean opponent;

    @ViewById(R.id.findBtn)
    Button findBtn;

    private Activity activity;

    protected SetChallengeView(Activity context) {
        super(context);
        this.activity = context;
    }

    public void bind(UserBean opponent) {
        this.opponent = opponent;
    }

    @AfterViews
    protected void afterViews() {
        super.afterViews();

        // override listener defined in layout
        findBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onMatchClick(null);
            }
        });
    }

    @Override
    public void onMatchClick(View view) {
        challengeFriend();

        ((MobileApplication) activity.getApplication()).sendMessage(
                HomeFeedFragment.class.getSimpleName(), HomeFeedFragment.MESSAGING_MESSAGE_REFRESH);
        ((MobileApplication) activity.getApplication()).sendMessage(
                FriendFragment.FRIEND_CHALLENGED, String.valueOf(opponent.getId()));

        Toast.makeText(
                activity,
                String.format(getResources().getString(R.string.challenge_enqueue_notification), opponent.getName()),
                Toast.LENGTH_LONG
        ).show();
        popup.dismiss();
    }

    @SneakyThrows(JsonProcessingException.class)
    private Challenge challengeFriend() {
        Challenge challenge = Challenge.createChallenge();
        challenge.type = "duration";
        challenge.duration = (int) getDuration().getStandardMinutes();
        challenge.isPublic = true;
        challenge.points_awarded = 500;
        challenge.start_time = new Date();
        Calendar expiry = new GregorianCalendar();
        expiry.add(Calendar.HOUR, 48);
        challenge.stop_time = expiry.getTime();

        Pair<Track,MatchQuality> p = durationToTrackId.get(getDuration());
        challenge.save();
        // Challenge must be saved before attempt is added.
        challenge.addAttempt(p.first);
        log.info(String.format("Created a challenge with id <%d,%d>", challenge.device_id, challenge.challenge_id));
        challenge.challengeUser(opponent.getId());
        Event.log(new Event.EventEvent("send_challenge").setChallengeId(challenge.id));
        log.info(String.format("Challenged user %d with challenge <%d,%d>",
                opponent.getId(), challenge.device_id, challenge.challenge_id));
        Notification synthetic = new Notification(new ChallengeNotification(
                AccessToken.get().getUserId(), opponent.getId(), challenge));
        synthetic.save();
        log.info(String.format("Created synthetic notification %d for challenge <%d,%d> to user %d",
                synthetic.id, challenge.device_id, challenge.challenge_id, opponent.getId()));
        return challenge;
    }

    protected int getButtonTextResId() {
        return R.string.send_challenge_button;
    }
}
