package com.raceyourself.raceyourself.home.sendchallenge;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.Friend;
import com.raceyourself.platform.models.Notification;
import com.raceyourself.platform.utils.MessageHandler;
import com.raceyourself.platform.utils.MessagingInterface;
import com.raceyourself.raceyourself.MobileApplication;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.home.HomeActivity;
import com.raceyourself.raceyourself.home.UserBean;
import com.raceyourself.raceyourself.home.feed.ChallengeNotificationBean;

import java.util.List;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

@Slf4j
public class FriendFragment extends Fragment {

    public static final String FRIEND_CHALLENGED = "FriendChallenged";
    /**
     * The fragment's ListView/GridView.
     */
    private StickyListHeadersListView listView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with Views.
     */
    private FriendListAdapter friendListAdapter;
    private FriendsListRefreshHandler friendsListRefreshHandler;
    private FriendChallengedHandler friendChallengedHandler;
    private Activity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        List<UserBean> users = UserBean.from(Friend.getFriends());
        friendListAdapter = new FriendListAdapter(getActivity(),
                android.R.layout.simple_list_item_1, users);
        final List<ChallengeNotificationBean> notifs =
                ChallengeNotificationBean.from(Notification.getNotificationsByType("challenge"));
        friendListAdapter.setChallengeNotifications(notifs);
        // TODO dream up way of avoiding cast
        friendListAdapter.setOnFriendAction((HomeActivity) getActivity());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friend, container, false);

        // Set the adapter
        listView = (StickyListHeadersListView) view.findViewById(R.id.friendsListView);
        listView.setAreHeadersSticky(false);
        listView.setAdapter(friendListAdapter);

        return view;
    }

    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        MessagingInterface.addHandler(
                friendsListRefreshHandler = new FriendsListRefreshHandler());
        ((MobileApplication) activity.getApplication()).addCallback(FRIEND_CHALLENGED,
                friendChallengedHandler = new FriendChallengedHandler());
    }

    @Override
    public void onPause() {
        super.onPause();
        MessagingInterface.removeHandler(friendsListRefreshHandler);
        ((MobileApplication) activity.getApplication()).removeCallback(FRIEND_CHALLENGED, friendChallengedHandler);
    }

    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = listView.getEmptyView();

        if (emptyText instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    private class FriendsListRefreshHandler implements MessageHandler {
        @Override
        public void sendMessage(String target, String method, String message) {
            if (SyncHelper.MESSAGING_METHOD_ON_SYNCHRONIZATION.equals(method)
                    && (SyncHelper.MESSAGING_MESSAGE_SYNC_SUCCESS_FULL.equals(message)
                    || SyncHelper.MESSAGING_MESSAGE_SYNC_SUCCESS_PARTIAL.equals(message))) {
                refreshFriends();
            }
        }
    }

    private class FriendChallengedHandler implements MobileApplication.Callback<String> {
        @Override
        public boolean call(String message) {
            // message = friend's ID
            friendListAdapter.friendChallenged(Integer.parseInt(message));
            friendListAdapter.notifyDataSetChanged();
            return true;
        }
    }

    public void refreshFriends() {
        final List<UserBean> refreshedUsers = UserBean.from(Friend.getFriends());
        final List<ChallengeNotificationBean> notifs =
                ChallengeNotificationBean.from(Notification.getNotificationsByType("challenge"));

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                friendListAdapter.setChallengeNotifications(notifs);
                friendListAdapter.mergeItems(refreshedUsers);
                friendListAdapter.notifyDataSetChanged();
                log.info("Updated friends list. There are now {} friends.", refreshedUsers.size());
            }
        });
    }
}
