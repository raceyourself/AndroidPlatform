package com.raceyourself.raceyourself.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

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
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
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
    @Getter @Setter
    private List<ChallengeNotificationBean> items;
    private final Context context;

    public ChallengeListAdapter(@NonNull Context context, int textViewResourceId, @NonNull List<ChallengeNotificationBean> items) {
        super(context, textViewResourceId, items);
        this.items = items;
        this.context = context;
    }

    public View getView(@NonNull int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.fragment_challenge_notification, null);
        }

        final ChallengeNotificationBean notif = items.get(position);
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

        return view;
    }
}
