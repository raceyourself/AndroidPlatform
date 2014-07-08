package com.raceyourself.raceyourself.home;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.raceyourself.platform.models.Notification;
import com.raceyourself.raceyourself.R;
import com.squareup.picasso.Picasso;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.text.SimpleDateFormat;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * A fragment representing a list of Items.
 * <p />
 * <p />
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
@Slf4j
public class ChallengeFragment extends ListFragment implements AbsListView.OnItemClickListener {

    private OnFragmentInteractionListener listener;

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

    private static final PeriodFormatter ACTIVITY_PERIOD_FORMAT = new PeriodFormatterBuilder()
            .appendHours()
            .appendSuffix(" hr")
            .appendMinutes()
            .appendSuffix(" min")
            .toFormatter();

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ChallengeFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setListAdapter(new ChallengeListAdapter(getActivity(),
                android.R.layout.simple_list_item_1, ChallengeNotificationBean.from(Notification.getNotificationsbyType("challenge"))));
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getListView().setOnItemClickListener(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        listener = (OnFragmentInteractionListener) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (listener != null) {
            listener.onFragmentInteraction((ChallengeNotificationBean)getListAdapter().getItem(position));
        }
    }

    /**
    * This interface must be implemented by activities that contain this
    * fragment to allow an interaction in this fragment to be communicated
    * to the activity and potentially other fragments contained in that
    * activity.
    * <p>
    * See the Android Training lesson <a href=
    * "http://developer.android.com/training/basics/fragments/communicating.html"
    * >Communicating with Other Fragments</a> for more information.
    */
    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(ChallengeNotificationBean challengeNotification);
    }

    public class ChallengeListAdapter extends ArrayAdapter<ChallengeNotificationBean> {

        //private final String DISTANCE_LABEL = NonSI.MILE.toString();
        //private final UnitConverter metresToMiles = SI.METER.getConverterTo(NonSI.MILE);
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");

        private Context context;

        public ChallengeListAdapter(Context context, int textViewResourceId, List<ChallengeNotificationBean> items) {
            super(context, textViewResourceId, items);
            this.context = context;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.fragment_challenge_notification, null);
            }

            ChallengeNotificationBean notif = (ChallengeNotificationBean)getListAdapter().getItem(position);
            ChallengeBean chal = notif.getChallenge(); // TODO avoid cast - more generic methods in ChallengeBean? 'limit' and 'goal'?

            TextView itemView = (TextView) view.findViewById(R.id.challenge_notification_challenger_name);
            itemView.setText(notif.getUser().getName());

            ImageView opponentProfilePic = (ImageView) view.findViewById(R.id.challenge_notification_profile_pic);
            Picasso.with(context)
                    .load(notif.getUser().getProfilePictureUrl())
                    .placeholder(R.drawable.icon_runner_green)
                    .into(opponentProfilePic);

//            TextView distanceView = (TextView) view.findViewById(R.id.challenge_notification_distance);
//            String distanceText = getString(R.string.challenge_notification_distance);
//            double miles = metresToMiles.convert(chal.getDistanceMetres());
//            distanceView.setText(String.format(distanceText, chal.getDistanceMetres(), DISTANCE_LABEL));

            TextView durationView = (TextView) view.findViewById(R.id.challenge_notification_duration);
            String durationText = getString(R.string.challenge_notification_duration);
            String duration = ACTIVITY_PERIOD_FORMAT.print(chal.getDuration().toPeriod());

            log.debug("Duration text and value: {} / {}", durationText, duration);
            durationView.setText(String.format(durationText, duration));

            TextView expiryView = (TextView) view.findViewById(R.id.challenge_notification_expiry);

            String period = TERSE_PERIOD_FORMAT.print(
                    new Period(new DateTime(), new DateTime(notif.getExpiry())));
            String expiryText = getString(R.string.challenge_expiry);
            expiryView.setText(String.format(expiryText, period));

            TextView subtitleView = (TextView) view.findViewById(R.id.challenge_notification_challenge_subtitle);

            String challengeName = chal.getName(context);
            String subtitle = getString(notif.isFromMe()
                    ? R.string.challenge_sent : R.string.challenge_received);
            subtitleView.setText(String.format(subtitle, challengeName));

            return view;
        }
    }
}
