package com.raceyourself.raceyourself.home.feed;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

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
public class HomeFeedFragment extends Fragment implements AdapterView.OnItemClickListener, HorizontalMissionListAdapter.OnFragmentInteractionListener {
    /**
     * How long do we show expired challenges for before clearing them out? Currently disabled (i.e. no expired
     * challenges at all).
     */
    public static final int DAYS_RETENTION = 0;
    private static final int MAX_ACTIVITY_ITEMS = 15;

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
    @Getter
    private HomeFeedCompositeListAdapter compositeListAdapter;
    private AutomatchAdapter automatchAdapter;
    @Getter
    private List<ChallengeNotificationBean> notifications;

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
        List<ChallengeNotificationBean> activity =
                Lists.newArrayList(Iterables.filter(unfiltered, new Predicate<ChallengeNotificationBean>() {
                    @Override
                    public boolean apply(ChallengeNotificationBean input) {
                        return input.isActivity();
                    }
                }));

        // we want the user to feel like the app is being actively used - so we don't filter out old challenges here
        // after a couple of days like with the user's own challenges. Instead, we limit the list to at most N elements.
        if (activity.size() > MAX_ACTIVITY_ITEMS)
            activity = activity.subList(0, MAX_ACTIVITY_ITEMS);
        return activity;
    }

    private ListView listView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        int offset = 0; // sub-list offset

        View view = inflater.inflate(R.layout.fragment_challenge_list, container, false);
        StickyListHeadersListView stickyListView = (StickyListHeadersListView)
                view.findViewById(R.id.challengeList);
        listView = stickyListView.getWrappedList();

        notifications =
                ImmutableList.copyOf(ChallengeNotificationBean.from(Notification.getNotificationsByType("challenge")));

        // Inbox - unread and received challenges
        List<ChallengeNotificationBean> filteredNotifications = inboxFilter(notifications);
        inboxListAdapter = new ExpandableChallengeListAdapter(
                getActivity(), filteredNotifications, activity.getString(R.string.home_feed_title_inbox), 4732989818333L);

        inboxListAdapter.setAbsListView(stickyListView.getWrappedList(), offset);
        offset += filteredNotifications.size();

        // Missions
        VerticalMissionListWrapperAdapter verticalMissionListWrapperAdapter =
                VerticalMissionListWrapperAdapter.create(getActivity(), android.R.layout.simple_list_item_1);
        verticalMissionListWrapperAdapter.setOnFragmentInteractionListener(this);
        offset++;

        // Run - read or sent challenges
        filteredNotifications = runFilter(notifications);
        runListAdapter = new ExpandableChallengeListAdapter(
                getActivity(), filteredNotifications, activity.getString(R.string.home_feed_title_run),
                AutomatchAdapter.HEADER_ID);

        runListAdapter.setAbsListView(stickyListView.getWrappedList(), offset);
        offset += filteredNotifications.size();

        // Automatch. Similar presentation to 'run', but can't be in the same adapter as it mustn't be made
        // subject to ChallengeListAdapter.mergeItems().
        automatchAdapter = AutomatchAdapter.create(getActivity(), R.layout.fragment_challenge_list);
        offset++;

        // Activity feed - complete challenges (both people finished the race) involving one of your friends. Covers:
        // 1. You vs a friend races - to remind yourself of races you've completed;
        // 2. Friend vs other races - friend vs friend, OR friend vs unknown friend of friend.
        filteredNotifications = activityFilter(notifications);
        ActivityAdapter activityAdapter = ActivityAdapter.create(getActivity(), filteredNotifications);
        offset += filteredNotifications.size();

        ImmutableList<? extends StickyListHeadersAdapter> adapters =
                ImmutableList.of(
                        inboxListAdapter,
                        verticalMissionListWrapperAdapter,
                        runListAdapter,
                        automatchAdapter,
                        activityAdapter);
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

        refreshLists();
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

    public void scrollToRunSection() {
        int idx = compositeListAdapter.getFirstPosition(runListAdapter.getClass());
        if (idx == -1)
            idx = compositeListAdapter.getFirstPosition(automatchAdapter.getClass());
        listView.setSelection(idx);
    }

    private void refreshLists() {
        notifications =
                ImmutableList.copyOf(ChallengeNotificationBean.from(Notification.getNotificationsByType("challenge")));

        final List<ChallengeNotificationBean> refreshedInbox = inboxFilter(notifications);
        final List<ChallengeNotificationBean> refreshedRun = runFilter(notifications);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int offset = 0; // sub-list offset
                inboxListAdapter.mergeItems(refreshedInbox);
                inboxListAdapter.setListOffset(offset);
                offset += refreshedInbox.size();
                offset++; // horizontal mission list
                runListAdapter.mergeItems(refreshedRun);
                runListAdapter.setListOffset(offset);
                offset += refreshedRun.size();
                offset++; // automatch
                // offset += activityList.size();
            }
        });
    }

    @Override
    public void onFragmentInteraction(MissionBean mission, View view) {
        if (listener != null) listener.onFragmentInteraction(mission, view);
    }

    private class ChallengeListRefreshHandler implements MobileApplication.Callback<String> {
        @Override
        public boolean call(String message) {
            // Refresh list if sync succeeded or someone requested a refresh
            if (SyncHelper.MESSAGING_MESSAGE_SYNC_SUCCESS_FULL.equals(message)
                    || SyncHelper.MESSAGING_MESSAGE_SYNC_SUCCESS_PARTIAL.equals(message)
                    || MESSAGING_MESSAGE_REFRESH.equals(message)) {

                refreshLists();
            }
            return false; // recurring
        }
    }

    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(ChallengeNotificationBean challengeNotificationBean);
        public void onFragmentInteraction(MissionBean missionBean, View view);

        public void onQuickmatchSelect();
    }
}
