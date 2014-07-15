package com.raceyourself.raceyourself.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.collect.Maps;
import com.nhaarman.listviewanimations.itemmanipulation.ExpandableListItemAdapter;
import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.Challenge;
import com.raceyourself.platform.models.Position;
import com.raceyourself.platform.models.Track;
import com.raceyourself.platform.models.User;
import com.raceyourself.platform.utils.Format;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.base.util.StringFormattingUtils;
import com.raceyourself.raceyourself.game.GameActivity;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.crypto.interfaces.PBEKey;

import bolts.Continuation;
import bolts.Task;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 10/07/2014.
 */
@Slf4j
class ChallengeListAdapter extends ExpandableListItemAdapter<ChallengeNotificationBean> {
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

    //private final String DISTANCE_LABEL = NonSI.MILE.toString();
    //private final UnitConverter metresToMiles = SI.METER.getConverterTo(NonSI.MILE);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
    private Map<Integer, ChallengeNotificationBean> notificationsById = Maps.newHashMap();
    private final Context context;

    public ChallengeListAdapter(@NonNull Context context, int textViewResourceId, @NonNull List<ChallengeNotificationBean> items) {
        super(context, items);
        this.context = context;
        for (ChallengeNotificationBean notif : items) {
            notificationsById.put(notif.getId(), notif);
        }
    }

    public ChallengeNotificationBean getChallengeNotificationBeanById(int id) {
        return notificationsById.get(id);
    }

    public synchronized void mergeItems(@NonNull List<ChallengeNotificationBean> notifications) {
        final boolean DEBUG = true;
        if (notifications.isEmpty()) {
            this.clear();
            log.info("Challenge notifications list cleared");
            return;
        }
        try {
            // Iterate over new list and old list (in sorted order), matching, adding and removing items
            int index = 0;
            while (index < notifications.size()) {
                // At end of old/current list - nothing to match against: add remaining from new list and
                // break out of loop
                if (index >= getCount()) {
                    // TODO: Animate insertion
                    this.addAll(notifications.subList(index, notifications.size()));
                    log.info("Challenge notifications: tail insertion of " + (notifications.size() - index) + " at " + index);
                    index = notifications.size();
                    break;
                }
                ChallengeNotificationBean a = notifications.get(index);
                ChallengeNotificationBean b = this.getItem(index);
                // Same notification: skip over item
                if (a.getId() == b.getId()) {
                    // TODO: Do we need to copy any data over?
                    log.info("Challenge notifications: " + a.getId() + " match at " + index);
                    index++;
                    continue;
                }
                int cmp = a.compareTo(b);
                // New notification to be placed before or in same position as current b,
                // insert above b and continue (next iteration will compare next a to same b)
                if (cmp <= 0) {
                    // TODO: Animate insertion
                    this.remove(a);
                    this.add(index, a);
                    log.info("Challenge notifications: " + a.getId() + " inserted at " + index);
                    index++;
                    continue;
                }
                // Items did not match and old item was earlier in the ordering, ie. removed.
                // TODO: Animate removal
                this.remove(b);
                log.info("Challenge notifications: removal " + b.getId() + " at " + index);
            }
            // Remove tail items not in new list
            for (; index < getCount();) {
                ChallengeNotificationBean b = this.getItem(index);
                // TODO: Animate removal
                this.remove(b);
                log.info("Challenge notifications: " + b.getId() + " tail removal at " + index);
            }
            if (DEBUG) {
                /// DEBUG
                if (notifications.size() != getCount())
                    throw new Error("ASSERT: size mismatch: " + notifications.size() + " != " + getCount());
                boolean error = false;
                for (int i = 0; i < notifications.size(); i++) {
                    if (notifications.get(i).getId() != getItem(i).getId()) {
                        log.error("Challenge notification list has not been updated correctly at index: " + i + ": " + notifications.get(i).getId() + " != " + getItem(i).getId());
                        error = true;
                    }
                    if (error) {
                        throw new Error("ASSERT: merge errors!");
                    }
                }
                /// DEBUG
            }
        } catch (Exception e) {
            log.error("Error in ChallengeListAdapter::mergeItems algorithm, attempting to recover with a clear+addAll", e);
            this.clear();
            this.addAll(notifications);
        }
        for (ChallengeNotificationBean notif : notifications) {
            notificationsById.put(notif.getId(), notif);
        }
        log.info("Updated challenge notification list. There are now {} challenges.", getCount());
    }

    @Override
    public View getTitleView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.fragment_challenge_notification, null);
        }

        final ChallengeNotificationBean notif = getItem(position);
        ChallengeBean chal = notif.getChallenge(); // TODO avoid cast - more generic methods in ChallengeBean? 'limit' and 'goal'?

        final View finalView = view;
        Task.callInBackground(new Callable<User>() {
            @Override
            public User call() throws Exception {
                User actualUser = SyncHelper.getUser(notif.getUser().getId());
                return actualUser;
            }
        }).continueWith(new Continuation<User, Void>() {
            @Override
            public Void then(Task<User> userTask) throws Exception {
                User foundUser = userTask.getResult();
                UserBean user = notif.getUser();
                user.setName(foundUser.getName());
                user.setShortName(StringFormattingUtils.getForenameAndInitial(user.getName()));
                user.setProfilePictureUrl(foundUser.getImage());

                TextView itemView = (TextView) finalView.findViewById(R.id.challenge_notification_challenger_name);
                itemView.setText(user.getName());

                final ImageView opponentProfilePic = (ImageView) finalView.findViewById(R.id.challenge_notification_profile_pic);

                Picasso.with(context).load(user.getProfilePictureUrl()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(opponentProfilePic);

                notif.setUser(user);
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);

        TextView durationView = (TextView) view.findViewById(R.id.challenge_notification_duration);
        String durationText = context.getString(R.string.challenge_notification_duration);
        String duration = ACTIVITY_PERIOD_FORMAT.print(chal.getDuration().toPeriod());

        log.debug("Duration text and value: {} / {}", durationText, duration);
        durationView.setText(String.format(durationText, duration));

        TextView expiryView = (TextView) view.findViewById(R.id.challenge_notification_expiry);

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

        TextView subtitleView = (TextView) view.findViewById(R.id.challenge_notification_challenge_subtitle);

        String challengeName = chal.getName(context);
        String subtitle = context.getString(notif.isFromMe()
                ? R.string.challenge_sent : R.string.challenge_received);
        subtitleView.setText(String.format(subtitle, challengeName));

        View card = view.findViewById(R.id.challenge_card_detail);
        log.info("getView - user={};isRead={}", notif.getUser().getName(), notif.isRead());
//        card.setBackgroundColor(context.getResources().getColor(
//                notif.isRead() ?
//                        android.R.color.white :
//                        android.R.color.holo_blue_light
//        ));

        return view;
    }

    @Override
    public View getContentView(int groupPosition, View convertView, ViewGroup parent) {
        LayoutInflater inflater = ((Activity)context).getLayoutInflater();

        if(convertView == null) {
            convertView = inflater.inflate(R.layout.activity_challenge_summary, null);
        }

        ChallengeNotificationBean currentChallenge = this.get(groupPosition);

        final ChallengeDetailBean activeChallengeFragment = new ChallengeDetailBean();

        convertView.findViewById(R.id.header_image).setVisibility(View.GONE);
        convertView.findViewById(R.id.header_text_container).setVisibility(View.GONE);

        activeChallengeFragment.setOpponent(currentChallenge.getUser());
        User player = SyncHelper.getUser(AccessToken.get().getUserId());
        final UserBean playerBean = new UserBean(player);
        activeChallengeFragment.setPlayer(playerBean);
        activeChallengeFragment.setChallenge(currentChallenge.getChallenge());

        final TextView challengeHeaderText = (TextView)convertView.findViewById(R.id.challengeHeader);
        String headerText = context.getString(R.string.challenge_notification_duration);

        String formattedHeader = String.format(headerText, activeChallengeFragment.getChallenge().getDuration().getStandardMinutes());
        challengeHeaderText.setText(formattedHeader);
        final View finalConvertView = convertView;

        resetTextViewsAndImages(convertView);

        final Button raceNowBtn = (Button)finalConvertView.findViewById(R.id.raceNowBtn);

        final ProgressBar progressBar = (ProgressBar)finalConvertView.findViewById(R.id.challenge_progress);
        progressBar.startAnimation(AnimationUtils.loadAnimation(context, R.anim.continuous_rotation_anim));

        Task.callInBackground(new Callable<ChallengeDetailBean>() {

            @Override
            public ChallengeDetailBean call() throws Exception {
                ChallengeDetailBean challengeDetailBean = new ChallengeDetailBean();
                Challenge challenge = SyncHelper.getChallenge(activeChallengeFragment.getChallenge().getDeviceId(), activeChallengeFragment.getChallenge().getChallengeId());
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
                return challengeDetailBean;
            }
        }).continueWith(new Continuation<ChallengeDetailBean, Void>() {
            @Override
            public Void then(Task<ChallengeDetailBean> challengeTask) throws Exception {
                activeChallengeFragment.setPoints(20000);
                String durationText = context.getString(R.string.challenge_notification_duration);
//                DurationChallengeBean durationChallenge = (DurationChallengeBean)activeChallengeFragment.getChallenge();

                int duration = activeChallengeFragment.getChallenge().getDuration().toStandardMinutes().getMinutes();
                activeChallengeFragment.setTitle(String.format(durationText, duration + " mins"));

                TextView opponentName = (TextView) finalConvertView.findViewById(R.id.opponentName);
                opponentName.setText(activeChallengeFragment.getOpponent().getShortName());

                TextView playerName = (TextView) finalConvertView.findViewById(R.id.playerName);
                playerName.setText(activeChallengeFragment.getPlayer().getShortName());

                ImageView opponentPic = (ImageView)finalConvertView.findViewById(R.id.opponentProfilePic);
//                log.info("opponent picture is " + activeChallengeFragment.getOpponent().getProfilePictureUrl());
                Picasso.with(context).load(activeChallengeFragment.getOpponent().getProfilePictureUrl()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(opponentPic);

                TrackSummaryBean playerTrack = activeChallengeFragment.getPlayerTrack();
                boolean playerComplete = false;
                if(playerTrack != null) {
                    playerComplete = true;

                    String formattedDistance = Format.twoDp(playerTrack.getDistanceRan());
                    setTextViewAndColor(R.id.playerDistance, "#269b47", formattedDistance + "KM", finalConvertView);
                    setTextViewAndColor(R.id.playerAveragePace, "#269b47", playerTrack.getAveragePace() + "", finalConvertView);
                    setTextViewAndColor(R.id.playerTopSpeed, "#269b47", playerTrack.getTopSpeed() + "", finalConvertView);
                    setTextViewAndColor(R.id.playerTotalUp, "#269b47", playerTrack.getTotalUp() + "", finalConvertView);
                    setTextViewAndColor(R.id.playerTotalDown, "#269b47", playerTrack.getTotalDown() + "", finalConvertView);

                    raceNowBtn.setVisibility(View.GONE);
                    Button raceLaterBtn = (Button)finalConvertView.findViewById(R.id.raceLaterBtn);
                    raceLaterBtn.setVisibility(View.GONE);
                }
                TrackSummaryBean opponentTrack = activeChallengeFragment.getOpponentTrack();
                boolean opponentComplete = false;
                if(opponentTrack != null) {
                    opponentComplete = true;

                    String formattedDistance = Format.twoDp(opponentTrack.getDistanceRan());
                    setTextViewAndColor(R.id.opponentDistance, "#269b47", formattedDistance + "KM", finalConvertView);
                    setTextViewAndColor(R.id.opponentAveragePace, "#269b47", opponentTrack.getAveragePace() + "", finalConvertView);
                    setTextViewAndColor(R.id.opponentTopSpeed, "#269b47", opponentTrack.getTopSpeed() + "", finalConvertView);
                    setTextViewAndColor(R.id.opponentTotalUp, "#269b47", opponentTrack.getTotalUp() + "", finalConvertView);
                    setTextViewAndColor(R.id.opponentTotalDown, "#269b47", opponentTrack.getTotalDown() + "", finalConvertView);
                }

                if(playerComplete && opponentComplete) {

                    if(playerTrack.getDistanceRan() > opponentTrack.getDistanceRan()) {
                        TextView opponentDistance = (TextView)finalConvertView.findViewById(R.id.opponentDistance);
                        opponentDistance.setTextColor(Color.parseColor("#e31f26"));
                        challengeHeaderText.setText("YOU WON");
                    } else {
                        TextView playerDistance = (TextView)finalConvertView.findViewById(R.id.playerDistance);
                        playerDistance.setTextColor(Color.parseColor("#e31f26"));
                        challengeHeaderText.setText("YOU LOST");
                        ImageView headerBox = (ImageView)finalConvertView.findViewById(R.id.titleBox);
                        headerBox.setImageDrawable(context.getResources().getDrawable(R.drawable.red_box));
                        FrameLayout rewardIcon = (FrameLayout)finalConvertView.findViewById(R.id.reward_icon);
                        rewardIcon.setVisibility(View.GONE);
                        TextView rewardText = (TextView)finalConvertView.findViewById(R.id.rewardPoints);
                        rewardText.setVisibility(View.GONE);
                    }

                    if(playerTrack.getAveragePace() < opponentTrack.getAveragePace()) {
                        TextView opponentAveragePace = (TextView)finalConvertView.findViewById(R.id.opponentAveragePace);
                        opponentAveragePace.setTextColor(Color.parseColor("#e31f26"));
                    } else {
                        TextView playerAveragePace = (TextView)finalConvertView.findViewById(R.id.playerAveragePace);
                        playerAveragePace.setTextColor(Color.parseColor("#e31f26"));
                    }

                    if(playerTrack.getTopSpeed() < opponentTrack.getTopSpeed()) {
                        TextView opponentTopSpeed = (TextView)finalConvertView.findViewById(R.id.opponentTopSpeed);
                        opponentTopSpeed.setTextColor(Color.parseColor("#e31f26"));
                    } else {
                        TextView playerTopSpeed = (TextView)finalConvertView.findViewById(R.id.playerTopSpeed);
                        playerTopSpeed.setTextColor(Color.parseColor("#e31f26"));
                    }

                    if(playerTrack.getTotalUp() > opponentTrack.getTotalUp()) {
                        TextView opponentTotalUp = (TextView)finalConvertView.findViewById(R.id.opponentTotalUp);
                        opponentTotalUp.setTextColor(Color.parseColor("#e31f26"));
                    } else {
                        TextView playerTotalUp = (TextView)finalConvertView.findViewById(R.id.playerTotalUp);
                        playerTotalUp.setTextColor(Color.parseColor("#e31f26"));
                    }

                    if(playerTrack.getTotalDown() > opponentTrack.getTotalDown()) {
                        TextView opponentTotalDown = (TextView)finalConvertView.findViewById(R.id.opponentTotalDown);
                        opponentTotalDown.setTextColor(Color.parseColor("#e31f26"));
                    } else {
                        TextView playerTotalDown = (TextView)finalConvertView.findViewById(R.id.playerTotalDown);
                        playerTotalDown.setTextColor(Color.parseColor("#e31f26"));
                    }
                }
                progressBar.clearAnimation();
                progressBar.setVisibility(View.INVISIBLE);
                return null;
            }
        }, Task.UI_THREAD_EXECUTOR);

        final ImageView playerImage = (ImageView)finalConvertView.findViewById(R.id.playerProfilePic);
        Picasso.with(context).load(playerBean.getProfilePictureUrl()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(playerImage);

        raceNowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gameIntent = new Intent(context, GameActivity.class);
                gameIntent.putExtra("challenge", activeChallengeFragment);
                context.startActivity(gameIntent);
            }
        });

        return finalConvertView;
    }

    private void resetTextViewsAndImages(View convertView) {
        setTextViewAndColor(R.id.playerDistance, "#1f1f1f", context.getString(R.string.challenge_default_value), convertView);
        setTextViewAndColor(R.id.opponentDistance, "#1f1f1f", context.getString(R.string.challenge_default_value), convertView);
        setTextViewAndColor(R.id.playerDistance, "#1f1f1f", context.getString(R.string.challenge_default_value), convertView);
        setTextViewAndColor(R.id.playerAveragePace, "#1f1f1f", context.getString(R.string.challenge_default_value), convertView);
        setTextViewAndColor(R.id.opponentAveragePace, "#1f1f1f", context.getString(R.string.challenge_default_value), convertView);
        setTextViewAndColor(R.id.playerTopSpeed, "#1f1f1f", context.getString(R.string.challenge_default_value), convertView);
        setTextViewAndColor(R.id.opponentTopSpeed, "#1f1f1f", context.getString(R.string.challenge_default_value), convertView);
        setTextViewAndColor(R.id.playerTotalUp, "#1f1f1f", context.getString(R.string.challenge_default_value), convertView);
        setTextViewAndColor(R.id.opponentTotalUp, "#1f1f1f", context.getString(R.string.challenge_default_value), convertView);
        setTextViewAndColor(R.id.playerTotalDown, "#1f1f1f", context.getString(R.string.challenge_default_value), convertView);
        setTextViewAndColor(R.id.opponentTotalDown, "#1f1f1f", context.getString(R.string.challenge_default_value), convertView);

        convertView.findViewById(R.id.raceNowBtn).setVisibility(View.VISIBLE);
        convertView.findViewById(R.id.raceLaterBtn).setVisibility(View.VISIBLE);
        convertView.findViewById(R.id.reward_icon).setVisibility(View.VISIBLE);
        convertView.findViewById(R.id.rewardPoints).setVisibility(View.VISIBLE);

        ImageView headerBox = (ImageView)convertView.findViewById(R.id.titleBox);
        headerBox.setImageDrawable(context.getResources().getDrawable(R.drawable.green_box));
    }

    public void setTextViewAndColor(int textViewId, String color, String textViewString, View view) {
        TextView textView = (TextView)view.findViewById(textViewId);
        textView.setTextColor(Color.parseColor(color));
        textView.setText(textViewString);
    }
}
