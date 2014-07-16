package com.raceyourself.raceyourself.home;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ListAdapter;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.raceyourself.platform.gpstracker.SyncHelper;
import com.nhaarman.listviewanimations.swinginadapters.prepared.AlphaInAnimationAdapter;
import com.raceyourself.platform.models.Friend;
import com.raceyourself.platform.models.Notification;
import com.raceyourself.raceyourself.MobileApplication;
import com.raceyourself.raceyourself.R;

import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * A fragment representing a list of Items.
 * <p />
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
@Slf4j
public class ChallengeFragment extends Fragment {
    /**
     * How long do we show expired challenges for before clearing them out?
     */
    public static final int DAYS_RETENTION = 2;

    private ChallengeListRefreshHandler challengeListRefreshHandler = new ChallengeListRefreshHandler();
    public static final String MESSAGING_MESSAGE_REFRESH = "refresh";

    private OnFragmentInteractionListener listener;
    private Activity activity;
    @Getter
    private ChallengeListAdapter challengeListAdapter;
    private HomePageCompositeListAdapter compositeAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ChallengeFragment() {
    }

    private List<ChallengeNotificationBean> filterOutOldExpiredChallenges(List<ChallengeNotificationBean> unfiltered) {
        return Lists.newArrayList(Iterables.filter(unfiltered, new Predicate<ChallengeNotificationBean>() {
            @Override
            public boolean apply(ChallengeNotificationBean input) {
                // Filter out challenges that expired more than DAYS_RETENTION ago.
                return !input.getExpiry().plusDays(DAYS_RETENTION).isBeforeNow();
            }
        }));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_challenge_list, container, false);

        List<HomePageRowBean> rowBeans = Lists.newArrayList();

        ListView listView = (ListView)view.findViewById(R.id.challengeList);
        List<ChallengeNotificationBean> notifications = filterOutOldExpiredChallenges(ChallengeNotificationBean.from(Notification.getNotificationsByType("challenge")));
        rowBeans.addAll(notifications);

        challengeListAdapter = new ChallengeListAdapter(getActivity(), R.layout.fragment_challenge_list, notifications);
        challengeListAdapter.setAbsListView(listView);

        List<UserBean> users = UserBean.from(Friend.getFriends());
        FriendListAdapter friendListAdapter = new FriendListAdapter(getActivity(),
                android.R.layout.simple_list_item_1, users);
        rowBeans.addAll(users);

        HomePageCompositeListAdapter compositeListAdapter = new HomePageCompositeListAdapter(
                getActivity(),
                android.R.layout.simple_list_item_1,
                rowBeans,
                ImmutableList.of(challengeListAdapter, friendListAdapter),
                ImmutableList.of(challengeListAdapter.getCount(), friendListAdapter.getCount()));

        listView.setAdapter(compositeListAdapter);

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

        ((MobileApplication)getActivity().getApplication()).addCallback(SyncHelper.MESSAGING_TARGET_PLATFORM, SyncHelper.MESSAGING_METHOD_ON_SYNCHRONIZATION, challengeListRefreshHandler);
        ((MobileApplication)getActivity().getApplication()).addCallback(getClass().getSimpleName(), challengeListRefreshHandler);

        refreshChallenges();
    }

    @Override
    public void onPause() {
        super.onPause();
        ((MobileApplication)getActivity().getApplication()).removeCallback(SyncHelper.MESSAGING_TARGET_PLATFORM, SyncHelper.MESSAGING_METHOD_ON_SYNCHRONIZATION, challengeListRefreshHandler);
        ((MobileApplication)getActivity().getApplication()).removeCallback(getClass().getSimpleName(), challengeListRefreshHandler);
    }

    private void refreshChallenges() {
        final List<ChallengeNotificationBean> refreshedNotifs = filterOutOldExpiredChallenges(
                ChallengeNotificationBean.from(Notification.getNotificationsByType("challenge")));

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                challengeListAdapter.mergeItems(refreshedNotifs);
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

                final List<ChallengeNotificationBean> refreshedNotifs = filterOutOldExpiredChallenges(
                        ChallengeNotificationBean.from(Notification.getNotificationsByType("challenge")));

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        challengeListAdapter.mergeItems(refreshedNotifs);
                    }
                });

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
