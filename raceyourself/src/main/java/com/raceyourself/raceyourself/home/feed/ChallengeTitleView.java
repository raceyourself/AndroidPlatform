package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.util.AttributeSet;
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
import org.androidannotations.annotations.ViewById;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 15/07/2014.
 */
@Slf4j
@EViewGroup(R.layout.fragment_inbox_collapsed)
public class ChallengeTitleView extends LinearLayout {

    private Context context;

    // TODO nothing in design for this yet!
//    @ViewById(R.id.challenge_notification_duration)
//    TextView durationView;

    @ViewById(R.id.challenge_notification_profile_pic)
    ImageView opponentProfilePic;

    @ViewById(R.id.challenge_notification_challenger_name)
    TextView opponentName;

    public ChallengeTitleView(Context context) {
        super(context);
        this.context = context;
    }

    public ChallengeTitleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        this.context = context;
    }

    public ChallengeTitleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void bind(ChallengeNotificationBean notif) {
        ChallengeBean chal = notif.getChallenge(); // TODO avoid cast - more generic methods in ChallengeBean? 'limit' and 'goal'?

        retrieveUser(notif);

//        String durationText = context.getString(R.string.challenge_notification_duration);
//        String duration = StringFormattingUtils.ACTIVITY_PERIOD_FORMAT.print(chal.getDuration().toPeriod());
//        durationView.setText(String.format(durationText, duration));
//        log.debug("Duration text and value: {} / {}", durationText, duration);

        // TODO needed for 'RUN!' section, but not inbox.
//        TextView expiryView = (TextView) findViewById(R.id.challenge_notification_expiry);
//        DateTime expiry = notif.getExpiry();
//
//        String expiryStr;
//        if (expiry.isBeforeNow())
//            expiryStr = context.getString(R.string.challenge_expired);
//        else {
//            String period = StringFormattingUtils.TERSE_PERIOD_FORMAT.print(new Period(new DateTime(), expiry));
//            String expiryRes = context.getString(R.string.challenge_expiry);
//            expiryStr = String.format(expiryRes, period);
//        }
//        expiryView.setText(expiryStr);


        // TODO needed for 'RUN!' section, but not inbox.
//        TextView subtitleView = (TextView) findViewById(R.id.challenge_notification_challenge_subtitle);
//        String challengeName = chal.getName(context);
//        String subtitle = context.getString(notif.isFromMe()
//                ? R.string.challenge_sent : R.string.challenge_received);
//        subtitleView.setText(String.format(subtitle, challengeName));
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

        opponentName.setText(user.getName());

        Picasso.with(context)
                .load(user.getProfilePictureUrl())
                .placeholder(R.drawable.default_profile_pic)
                .transform(new PictureUtils.CropCircle())
                .into(opponentProfilePic);

        notif.setUser(user);
    }
}