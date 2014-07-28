package com.raceyourself.raceyourself.home.sendchallenge;

import android.app.Activity;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.Challenge;
import com.raceyourself.platform.models.ChallengeNotification;
import com.raceyourself.platform.models.Event;
import com.raceyourself.platform.models.Notification;
import com.raceyourself.platform.models.Track;
import com.raceyourself.raceyourself.MobileApplication;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.PreviouslyRunDurationView;
import com.raceyourself.raceyourself.home.feed.HomeFeedFragment;
import com.raceyourself.raceyourself.home.UserBean;

import org.androidannotations.annotations.EViewGroup;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 08/07/2014.
 */
@Slf4j
@EViewGroup(R.layout.activity_select_duration)
public class SetChallengeView extends PreviouslyRunDurationView {

    private UserBean opponent;

    private Activity activity;

    protected SetChallengeView(Activity context) {
        super(context);
        this.activity = context;
    }

    public void bind(UserBean opponent) {
        this.opponent = opponent;
    }

    @Override
    public void onConfirm() {
        challengeFriend();

        ((MobileApplication) activity.getApplication()).sendMessage(
                HomeFeedFragment.class.getSimpleName(), HomeFeedFragment.MESSAGING_MESSAGE_REFRESH);
        ((MobileApplication) activity.getApplication()).sendMessage(
                FriendFragment.FRIEND_CHALLENGED, String.valueOf(opponent.getId()));

        String message = String.format(
                getResources().getString(R.string.challenge_enqueue_notification), opponent.getName());
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
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

        Track track = getAvailableOwnTracksMap().get((int) getDuration().getStandardMinutes()).first;
        challenge.save();
        // Challenge must be saved before attempt is added.
        challenge.addAttempt(track);
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
