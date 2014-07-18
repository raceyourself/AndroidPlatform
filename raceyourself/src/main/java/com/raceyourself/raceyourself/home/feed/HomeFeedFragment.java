package com.raceyourself.raceyourself.home.feed;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.Notification;
import com.raceyourself.raceyourself.MobileApplication;
import com.raceyourself.raceyourself.R;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * A fragment representing a list of Items.
 * <p />
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
@Slf4j
public class HomeFeedFragment extends Fragment implements AdapterView.OnItemClickListener {
    /**
     * How long do we show expired challenges for before clearing them out? Currently disabled (i.e. no expired
     * challenges at all).
     */
    public static final int DAYS_RETENTION = 0;

    private ChallengeListRefreshHandler challengeListRefreshHandler = new ChallengeListRefreshHandler();
    public static final String MESSAGING_MESSAGE_REFRESH = "refresh";

    private OnFragmentInteractionListener listener;
    private Activity activity;
    @Getter
    private ExpandableChallengeListAdapter inboxListAdapter;
    @Getter
    private ExpandableChallengeListAdapter runListAdapter;
    @Setter
    private Runnable onCreateViewListener = null;
    private HomeFeedCompositeListAdapter compositeListAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public HomeFeedFragment() {
    }

    private Predicate<ChallengeNotificationBean> activeOrRecentPredicate = new Predicate<ChallengeNotificationBean>() {
        @Override
        public boolean apply(ChallengeNotificationBean input) {
            // Filter out challenges that expired more than DAYS_RETENTION ago.
            return !input.getExpiry().plusDays(DAYS_RETENTION).isBeforeNow();
        }
    };

    private List<ChallengeNotificationBean> inboxFilter(List<ChallengeNotificationBean> unfiltered) {
        return Lists.newArrayList(Iterables.filter(
                Iterables.filter(unfiltered, new Predicate<ChallengeNotificationBean>() {
            @Override
            public boolean apply(ChallengeNotificationBean input) {
                return input.isInbox();
            }
        }), activeOrRecentPredicate));
    }

    private List<ChallengeNotificationBean> runFilter(List<ChallengeNotificationBean> unfiltered) {
        return Lists.newArrayList(Iterables.filter(
                Iterables.filter(unfiltered, new Predicate<ChallengeNotificationBean>() {
                    @Override
                    public boolean apply(ChallengeNotificationBean input) {
                        return input.isRunnableNow();
                    }
                }), activeOrRecentPredicate));
    }

    private List<ChallengeNotificationBean> activityFilter(List<ChallengeNotificationBean> unfiltered) {
        return Lists.newArrayList(Iterables.filter(
                Iterables.filter(unfiltered, new Predicate<ChallengeNotificationBean>() {
                    @Override
                    public boolean apply(ChallengeNotificationBean input) {
                        return input.isComplete();
                    }
                }), activeOrRecentPredicate));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_challenge_list, container, false);
        StickyListHeadersListView stickyListView = (StickyListHeadersListView)
                view.findViewById(R.id.challengeList);

        // Inbox - unread and received challenges
        List<ChallengeNotificationBean> notifications = inboxFilter(
                ChallengeNotificationBean.from(Notification.getNotificationsByType("challenge")));
        inboxListAdapter = new ExpandableChallengeListAdapter(
                getActivity(), notifications, activity.getString(R.string.home_feed_title_inbox), 4732989818333L);
        inboxListAdapter.setAbsListView(stickyListView.getWrappedList());

        // Missions
        VerticalMissionListWrapperAdapter verticalMissionListWrapperAdapter =
                VerticalMissionListWrapperAdapter.create(getActivity(), android.R.layout.simple_list_item_1);

        // Run - read or sent challenges
        notifications = runFilter(
                ChallengeNotificationBean.from(Notification.getNotificationsByType("challenge")));
        runListAdapter = new ExpandableChallengeListAdapter(
                getActivity(), notifications, activity.getString(R.string.home_feed_title_run), 732411007823432L);
        runListAdapter.setAbsListView(stickyListView.getWrappedList());

        // Automatch. Similar presentation to 'run', but can't be in the same adapter as it mustn't be made
        // subject to ChallengeListAdapter.mergeItems().
        AutomatchAdapter automatchAdapter = AutomatchAdapter.create(getActivity(), R.layout.fragment_challenge_list);

        // Activity feed - complete challenges (both people finished the race) involving one of your friends. Covers:
        // 1. You vs a friend races - to remind yourself of races you've completed;
        // 2. Friend vs other races - friend vs friend, OR friend vs unknown friend of friend.
        notifications = activityFilter(
                ChallengeNotificationBean.from(Notification.getNotificationsByType("challenge")));
        ActivityAdapter activityAdapter = ActivityAdapter.create(getActivity(), notifications);

        ImmutableList<? extends StickyListHeadersAdapter> adapters =
                ImmutableList.of(inboxListAdapter, verticalMissionListWrapperAdapter,
                        runListAdapter, automatchAdapter, activityAdapter);
        compositeListAdapter = new HomeFeedCompositeListAdapter(
                getActivity(), android.R.layout.simple_list_item_1, adapters);

        stickyListView.setAdapter(compositeListAdapter);

        stickyListView.getWrappedList().setOnItemClickListener(this);

        if (onCreateViewListener != null)
            onCreateViewListener.run();

        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        HomeFeedRowBean bean = compositeListAdapter.getItem(position);

        if (listener != null && bean instanceof AutomatchBean) {
            listener.onQuickmatchSelect();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        listener = (OnFragmentInteractionListener) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        ((MobileApplication)getActivity().getApplication()).addCallback(
                SyncHelper.MESSAGING_TARGET_PLATFORM,
                SyncHelper.MESSAGING_METHOD_ON_SYNCHRONIZATION, challengeListRefreshHandler);
        ((MobileApplication)getActivity().getApplication()).addCallback(
                getClass().getSimpleName(), challengeListRefreshHandler);

        refreshChallenges();
    }

    @Override
    public void onPause() {
        super.onPause();
        ((MobileApplication)getActivity().getApplication()).removeCallback(
                SyncHelper.MESSAGING_TARGET_PLATFORM,
                SyncHelper.MESSAGING_METHOD_ON_SYNCHRONIZATION, challengeListRefreshHandler);
        ((MobileApplication)getActivity().getApplication()).removeCallback(
                getClass().getSimpleName(), challengeListRefreshHandler);
    }

    private void refreshChallenges() {
        List<ChallengeNotificationBean> notifications =
                ChallengeNotificationBean.from(Notification.getNotificationsByType("challenge"));

        final List<ChallengeNotificationBean> refreshedInbox = inboxFilter(notifications);
        final List<ChallengeNotificationBean> refreshedRun = runFilter(notifications);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                inboxListAdapter.mergeItems(refreshedInbox);
                runListAdapter.mergeItems(refreshedRun);
            }
        });
    }

    private class ChallengeListRefreshHandler implements MobileApplication.Callback<String> {
        @Override
        public boolean call(String message) {
            // Refresh list if sync succeeded or someone requested a refresh
            if (SyncHelper.MESSAGING_MESSAGE_SYNC_SUCCESS_FULL.equals(message)
                    || SyncHelper.MESSAGING_MESSAGE_SYNC_SUCCESS_PARTIAL.equals(message)
                    || MESSAGING_MESSAGE_REFRESH.equals(message)) {

                refreshChallenges();
            }
            return false; // recurring
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

        public void onQuickmatchSelect();
    }
}
