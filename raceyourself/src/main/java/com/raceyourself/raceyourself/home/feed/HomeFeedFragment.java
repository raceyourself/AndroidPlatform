package com.raceyourself.raceyourself.home.feed;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
public class HomeFeedFragment extends Fragment {
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
    private ChallengeListAdapter inboxListAdapter;
    @Getter
    private ChallengeListAdapter runListAdapter;
    private HomeFeedCompositeListAdapter compositeAdapter;
    @Setter
    private Runnable onCreateViewListener = null;

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
                return !input.isRead() && !input.isFromMe();
            }
        }), activeOrRecentPredicate));
    }

    private List<ChallengeNotificationBean> runFilter(List<ChallengeNotificationBean> unfiltered) {
        return Lists.newArrayList(Iterables.filter(
                Iterables.filter(unfiltered, new Predicate<ChallengeNotificationBean>() {
            @Override
            public boolean apply(ChallengeNotificationBean input) {
                return input.isRead() || input.isFromMe();
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
        inboxListAdapter =
                new ChallengeListAdapter(getActivity(), R.layout.fragment_challenge_list,
                        notifications, activity.getString(R.string.home_feed_title_inbox));
        inboxListAdapter.setAbsListView(stickyListView.getWrappedList());

        // Missions
        VerticalMissionListWrapperAdapter verticalMissionListWrapperAdapter =
                VerticalMissionListWrapperAdapter.create(getActivity(), android.R.layout.simple_list_item_1);
        
        // Run - read or sent challenges
        notifications = runFilter(
                ChallengeNotificationBean.from(Notification.getNotificationsByType("challenge")));
        notifications.add(new AutomatchBean());

        runListAdapter =
                new ChallengeListAdapter(getActivity(), R.layout.fragment_challenge_list,
                        notifications, activity.getString(R.string.home_feed_title_run));
        runListAdapter.setAbsListView(stickyListView.getWrappedList());

        ImmutableList<? extends StickyListHeadersAdapter> adapters =
                ImmutableList.of(inboxListAdapter, verticalMissionListWrapperAdapter, runListAdapter);
        HomeFeedCompositeListAdapter compositeListAdapter = new HomeFeedCompositeListAdapter(
                getActivity(), android.R.layout.simple_list_item_1, adapters);

        stickyListView.setAdapter(compositeListAdapter);

        if (onCreateViewListener != null) onCreateViewListener.run();

        return view;
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

    //    @Override
//    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//        if (listener != null) {
//            listener.onFragmentInteraction((ChallengeNotificationBean)getListAdapter().getItem(position));
//        }
//    }
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
    }
}
