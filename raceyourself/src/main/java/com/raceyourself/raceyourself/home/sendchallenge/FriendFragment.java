package com.raceyourself.raceyourself.home.sendchallenge;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.Friend;
import com.raceyourself.platform.utils.MessageHandler;
import com.raceyourself.platform.utils.MessagingInterface;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.home.HomeActivity;
import com.raceyourself.raceyourself.home.UserBean;

import java.util.List;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FriendFragment extends Fragment {

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView listView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with Views.
     */
    private FriendListAdapter friendListAdapter;
    private FriendsListRefreshHandler friendsListRefreshHandler;
    private List<UserBean> users;
    private Activity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        users = UserBean.from(Friend.getFriends());
        friendListAdapter = new FriendListAdapter(getActivity(),
                android.R.layout.simple_list_item_1, users);

        // TODO dream up way of avoiding cast
        friendListAdapter.setOnFriendAction((HomeActivity) getActivity());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friend, container, false);

        // Set the adapter
        listView = (AbsListView) view.findViewById(android.R.id.list);
        ((AdapterView<ListAdapter>) listView).setAdapter(friendListAdapter);

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
    }

    @Override
    public void onPause() {
        super.onPause();
        MessagingInterface.removeHandler(friendsListRefreshHandler);
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

                final List<UserBean> refreshedUsers = UserBean.from(Friend.getFriends());

                if (!refreshedUsers.equals(users)) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            friendListAdapter.setItems(refreshedUsers);
                            friendListAdapter.notifyDataSetChanged();
                            log.info("Updated friends list. There are now {} friends.",
                                    refreshedUsers.size());
                        }
                    });
                }
            }
        }
    }
}
