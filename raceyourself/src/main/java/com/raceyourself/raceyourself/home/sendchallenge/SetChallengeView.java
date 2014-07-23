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
import com.raceyourself.platform.models.Notification;
import com.raceyourself.platform.models.Track;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.MobileApplication;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.ChooseDurationView;
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
public class SetChallengeView extends ChooseDurationView {
    // TODO refactor as popup.
    private UserBean opponent;
    private SortedMap<Integer,Pair<Track,MatchQuality>> durationToTrackId = Maps.newTreeMap();

    @ViewById(R.id.findBtn)
    Button findBtn;
    @ViewById(R.id.playerProfilePic)
    ImageView opponentProfileImageView;

    private Activity activity;

    // TODO refactor. This field doesn't belong in a View subclass.
    private PopupWindow popup;

    protected SetChallengeView(Activity context) {
        super(context);
        this.activity = context;
    }

    public void bind(UserBean opponent) {
        this.opponent = opponent;
    }

    public void show() {
        popup = new PopupWindow(this);
        popup.setWindowLayoutMode(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        popup.showAtLocation(
                activity.getWindow().getDecorView().findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
    }

    public boolean isShowing() {
        return popup.isShowing();
    }

    public void dismiss() {
        popup.dismiss();
    }

    @AfterViews
    protected void afterViews() {
        super.afterViews();

        findBtn.setText("Send Challenge");

        User player = User.get(AccessToken.get().getUserId());
        Picasso
            .with(activity)
            .load(player.getImage())
            .placeholder(R.drawable.default_profile_pic)
            .transform(new PictureUtils.CropCircle())
            .into(opponentProfileImageView);

        populateAvailableTracksMap();

        // override listener defined in layout
        findBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onMatchClick(null);
            }
        });
    }

    /**
     * We need to match the selected duration with an available track. Considerations:
     *
     * 1. Duration. Ideally about the same as the selected duration; failing that longer, failing that, shorter.
     * 2. Age. Favour recent tracks.
     *
     * We prioritise duration. Among similarly 'qualified' tracks, we go with the most recent.
     */
    private void populateAvailableTracksMap() {
        int playerUserId = AccessToken.get().getUserId();
        List<Track> playerTracks = Track.getTracks(playerUserId);
        for (int durationMins = MIN_DURATION_MINS; durationMins <= MAX_DURATION_MINS; durationMins += STEP_SIZE_MINS) {
            long desiredDurationMillis = durationMins * 60 * 1000;
            List<Track> matches = Lists.newArrayList();
            MatchQuality quality = null;
            for (Track candidate : playerTracks) {
                // Go 2.5 mins above desired duration.
                if (candidate.time >= (desiredDurationMillis - 60L * 1000L) &&
                        candidate.time < (desiredDurationMillis + 150L * 1000L)) {
                    matches.add(candidate);
                    quality = MatchQuality.GOOD;
                }
            }
            if (matches.isEmpty()) {
                // If they've not run the desired distance, pick a longer run (so we can truncate).
                for (Track candidate : playerTracks) {
                    if (candidate.time >= desiredDurationMillis) {
                        matches.add(candidate);
                        quality = MatchQuality.TRACK_TOO_LONG;
                    }
                }
            }
            if (matches.isEmpty()) {
                // Final fallback: shorter tracks.
                matches = playerTracks;
                quality = MatchQuality.TRACK_TOO_SHORT;
            }
            Collections.sort(matches, new Comparator<Track>() {
                @Override
                public int compare(Track lhs, Track rhs) {
                    return (int) (lhs.ts - rhs.ts); // TODO in practice, is this cast safe...?
                }
            });
            Track newestOfFiltered = matches.get(0);
            durationToTrackId.put(durationMins, new Pair(newestOfFiltered, quality));
        }
    }

    @Override
    public void onMatchClick(View view) {
        challengeFriend();
        ((MobileApplication) activity.getApplication()).sendMessage(
                HomeFeedFragment.class.getSimpleName(), HomeFeedFragment.MESSAGING_MESSAGE_REFRESH);

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
        challenge.duration = getDuration()*60;
        challenge.isPublic = true;
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
        log.info(String.format("Challenged user %d with challenge <%d,%d>",
                opponent.getId(), challenge.device_id, challenge.challenge_id));
        Notification synthetic = new Notification(new ChallengeNotification(
                AccessToken.get().getUserId(), opponent.getId(), challenge));
        synthetic.save();
        log.info(String.format("Created synthetic notification %d for challenge <%d,%d> to user %d",
                synthetic.id, challenge.device_id, challenge.challenge_id, opponent.getId()));
        return challenge;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        super.onProgressChanged(seekBar, progress, fromUser);

        if (durationToTrackId.isEmpty()) {
            log.debug("durationToTrackId empty; quitting onProgressChanged(). Legit if this occurs from onCreate();" +
                    " dubious if observed repeatedly.");
            return;
        }

        // TODO code stolen from superclass below; consolidate!
        int nSteps = 6;
        TextView warning = (TextView) findViewById(R.id.lengthWarning);
        int duration = ((progress / nSteps) + 1) * MIN_DURATION_MINS;
        if(duration == 0) {
            duration = MIN_DURATION_MINS;
        }
        MatchQuality quality = durationToTrackId.get(duration).second;

        // TODO jodatime...
        String qualityWarning = quality.getMessageId() == null ? "" :
                String.format(activity.getString(quality.getMessageId()), duration + " mins");
        warning.setText(qualityWarning);
    }

    private enum MatchQuality {
        GOOD(null),
        TRACK_TOO_LONG(R.string.send_challenge_track_too_long),
        TRACK_TOO_SHORT(R.string.send_challenge_track_too_short);

        @Getter
        private final Integer messageId;

        MatchQuality(Integer messageId) {
            this.messageId = messageId;
        }
    }
}
