package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.base.util.StringFormattingUtils;
import com.raceyourself.raceyourself.home.UserBean;
import com.squareup.picasso.Picasso;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.UiThread;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 15/07/2014.
 */
@Slf4j
@EViewGroup(R.layout.fragment_challenge_notification)
public class ChallengeHeaderView extends LinearLayout {
    /**
     * For expiry duration.
     *
     * TODO 118n. Does JodaTime put these suffixes in the right place for languages other than
     * English? */
    private static final PeriodFormatter TERSE_PERIOD_FORMAT = new PeriodFormatterBuilder()
            .appendYears()
            .appendSuffix("yr")
            .appendMonths()
            .appendSuffix("mo")
            .appendDays()
            .appendSuffix("d")
            .appendHours()
            .appendSuffix("h")
            .appendMinutes()
            .appendSuffix("m")
            .toFormatter();

    /** For activity headline - e.g. "How far can you run in 5 min?". TODO i18n */
    private static final PeriodFormatter ACTIVITY_PERIOD_FORMAT = new PeriodFormatterBuilder()
            .appendHours()
            .appendSuffix(" hr")
            .appendMinutes()
            .appendSuffix(" min")
            .toFormatter();

    private Context context;

    public ChallengeHeaderView(Context context) {
        super(context);
        this.context = context;
    }

    public ChallengeHeaderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        this.context = context;
    }

    public ChallengeHeaderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void bind(ChallengeNotificationBean notif) {
        ChallengeBean chal = notif.getChallenge(); // TODO avoid cast - more generic methods in ChallengeBean? 'limit' and 'goal'?

        retrieveUser(notif);

        TextView durationView = (TextView) findViewById(R.id.challenge_notification_duration);
        String durationText = context.getString(R.string.challenge_notification_duration);
        String duration = ACTIVITY_PERIOD_FORMAT.print(chal.getDuration().toPeriod());

        log.debug("Duration text and value: {} / {}", durationText, duration);
        durationView.setText(String.format(durationText, duration));

        TextView expiryView = (TextView) findViewById(R.id.challenge_notification_expiry);

        DateTime expiry = notif.getExpiry();

        String expiryStr;
        if (expiry.isBeforeNow())
            expiryStr = context.getString(R.string.challenge_expired);
        else {
            String period = TERSE_PERIOD_FORMAT.print(new Period(new DateTime(), expiry));
            String expiryRes = context.getString(R.string.challenge_expiry);
            expiryStr = String.format(expiryRes, period);
        }
        expiryView.setText(expiryStr);

        TextView subtitleView = (TextView) findViewById(R.id.challenge_notification_challenge_subtitle);

        String challengeName = chal.getName(context);
        String subtitle = context.getString(notif.isFromMe()
                ? R.string.challenge_sent : R.string.challenge_received);
        subtitleView.setText(String.format(subtitle, challengeName));

        View card = findViewById(R.id.challenge_card_detail);
        log.info("getView - user={};isRead={}", notif.getUser().getName(), notif.isRead());
    }

    @Background
    void retrieveUser(ChallengeNotificationBean challengeNotificationBean) {
        User actualUser = SyncHelper.getUser(challengeNotificationBean.getUser().getId());

        drawTitle(actualUser, challengeNotificationBean);
    }

    @UiThread
    void drawTitle(User actualUser, ChallengeNotificationBean notif) {
        UserBean user = notif.getUser();
        user.setName(actualUser.getName());
        user.setShortName(StringFormattingUtils.getForenameAndInitial(user.getName()));
        user.setProfilePictureUrl(actualUser.getImage());

        TextView itemView = (TextView) findViewById(R.id.challenge_notification_challenger_name);
        itemView.setText(user.getName());

        final ImageView opponentProfilePic = (ImageView) findViewById(R.id.challenge_notification_profile_pic);

        Picasso.with(context)
                .load(user.getProfilePictureUrl())
                .placeholder(R.drawable.default_profile_pic)
                .transform(new PictureUtils.CropCircle())
                .into(opponentProfilePic);

        notif.setUser(user);
    }
}