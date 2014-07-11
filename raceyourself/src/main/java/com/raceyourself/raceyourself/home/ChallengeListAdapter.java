package com.raceyourself.raceyourself.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.collect.Maps;
import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.base.util.StringFormattingUtils;
import com.squareup.picasso.Picasso;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 10/07/2014.
 */
@Slf4j
class ChallengeListAdapter extends ArrayAdapter<ChallengeNotificationBean> {
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
        super(context, textViewResourceId, items);
        this.context = context;
        for (ChallengeNotificationBean notif : items) {
            notificationsById.put(notif.getId(), notif);
        }
    }

    public ChallengeNotificationBean getChallengeNotificationBeanById(int id) {
        return notificationsById.get(id);
    }

    public synchronized void mergeItems(List<ChallengeNotificationBean> notifications) {
        final boolean DEBUG = true;
        if (notifications.isEmpty()) {
            this.clear();
            log.info("Challenge notifications list cleared");
            return;
        }
        try {
            // Iterate over new list and old lsit (in sorted order), matching, adding and removing items
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
                    log.info("Challenge notifications: match at " + index);
                    index++;
                    continue;
                }
                int cmp = a.compareTo(b);
                // New notification to be placed before or in same position as current b,
                // insert above b and continue (next iteration will compare next a to same b)
                if (cmp <= 0) {
                    // TODO: Animate insertion
                    this.insert(a, index);
                    log.info("Challenge notifications: inserted at " + index);
                    index++;
                    continue;
                }
                // Items did not match and old item was earlier in the ordering, ie. removed.
                // TODO: Animate removal
                this.remove(b);
                log.info("Challenge notifications: removal at " + index);
            }
            // Remove tail items not in new list
            for (; index < getCount(); index++) {
                ChallengeNotificationBean b = this.getItem(index);
                // TODO: Animate removal
                this.remove(b);
                log.info("Challenge notifications: tail removal at " + index);
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
        log.info("Updated challenge notification list. There are now {} challenges.", getCount());
    }

    public View getView(@NonNull int position, View convertView, ViewGroup parent) {
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

        log.info("getView - user={};isRead={}", notif.getUser().getName(), notif.isRead());
        view.setBackgroundColor(context.getResources().getColor(
                notif.isRead() ?
                        android.R.color.white :
                        android.R.color.holo_blue_light
        ));

        return view;
    }
}
