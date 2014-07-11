package com.raceyourself.raceyourself.home;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.Notification;
import com.raceyourself.platform.utils.MessageHandler;
import com.raceyourself.platform.utils.MessagingInterface;

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
public class ChallengeFragment extends ListFragment implements AbsListView.OnItemClickListener {
    /**
     * How long do we show expired challenges for before clearing them out?
     */
    public static final int DAYS_RETENTION = 2;

    private ChallengeListRefreshHandler challengeListRefreshHandler;

    private OnFragmentInteractionListener listener;
    private Activity activity;
    @Getter
    private ChallengeListAdapter challengeListAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ChallengeFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<ChallengeNotificationBean> notifications = filterOutOldExpiredChallenges(
                ChallengeNotificationBean.from(Notification.getNotificationsByType("challenge")));
        challengeListAdapter = new ChallengeListAdapter(getActivity(), android.R.layout.simple_list_item_1, notifications);
        setListAdapter(challengeListAdapter);
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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getListView().setOnItemClickListener(this);
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
        MessagingInterface.addHandler(
                challengeListRefreshHandler = new ChallengeListRefreshHandler());

        final List<ChallengeNotificationBean> refreshedNotifs = filterOutOldExpiredChallenges(
                ChallengeNotificationBean.from(Notification.getNotificationsByType("challenge")));

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                challengeListAdapter.mergeItems(refreshedNotifs);
                challengeListAdapter.mergeItems(refreshedNotifs);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        MessagingInterface.removeHandler(challengeListRefreshHandler);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (listener != null) {
            listener.onFragmentInteraction((ChallengeNotificationBean) getListAdapter().getItem(position));
        }
    }

    private class ChallengeListRefreshHandler implements MessageHandler {
        @Override
        public void sendMessage(String target, String method, String message) {
            if (SyncHelper.MESSAGING_METHOD_ON_SYNCHRONIZATION.equals(method)
                    && (SyncHelper.MESSAGING_MESSAGE_SYNC_SUCCESS_FULL.equals(message)
                    || SyncHelper.MESSAGING_MESSAGE_SYNC_SUCCESS_PARTIAL.equals(message))) {

                final List<ChallengeNotificationBean> refreshedNotifs = filterOutOldExpiredChallenges(
                        ChallengeNotificationBean.from(Notification.getNotificationsByType("challenge")));

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        challengeListAdapter.mergeItems(refreshedNotifs);
                    }
                });
            }
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
