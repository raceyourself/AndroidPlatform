package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.graphics.Color;
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
        retrieveUsers(notif);
    }

    @Background
    void retrieveUsers(ChallengeNotificationBean challengeNotificationBean) {
        User fromUser = SyncHelper.getUser(challengeNotificationBean.getFrom().getId());
        User toUser = SyncHelper.getUser(challengeNotificationBean.getTo().getId());
        drawTitle(fromUser, toUser, challengeNotificationBean);
    }

    // TODO use inheritance to avoid having both these methods below here together...

    @UiThread
    void drawTitle(User from, User to, ChallengeNotificationBean notif) {
        drawUserDetails(from, notif, fromName, fromProfilePic, fromRankIcon);
        drawUserDetails(to, notif, toName, toProfilePic, toRankIcon);

        // TODO make the below actually state the result!
        raceOutcome.setText("Won against");
    }

    private void drawUserDetails(User user, ChallengeNotificationBean notif,
                                 TextView name, ImageView pic, ImageView rank) {
        UserBean fromUser = notif.getFrom();
        fromUser.setName(user.getName());
        fromUser.setShortName(StringFormattingUtils.getForenameAndInitial(user.getName()));
        fromUser.setProfilePictureUrl(user.getImage());

        name.setText(fromUser.getName());
        //rank.setImageDrawable(fromUser.getr);

        Picasso.with(context)
            .load(fromUser.getProfilePictureUrl())
            .placeholder(R.drawable.default_profile_pic)
            .transform(new PictureUtils.CropCircle())
            .into(pic);
    }
}
