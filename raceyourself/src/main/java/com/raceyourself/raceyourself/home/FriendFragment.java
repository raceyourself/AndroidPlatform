package com.raceyourself.raceyourself.home;

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

import java.util.List;

import lombok.NonNull;

/**
 * A fragment representing a list of Items.
 * <p />
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p />
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class FriendFragment extends Fragment implements AbsListView.OnItemClickListener {

    private OnFragmentInteractionListener listener;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView listView;

    /**
     * The Adapter which will be used to populate the ListView/GridView with Views.
     */
    private FriendsListAdapter friendsListAdapter;
    private FriendsListRefreshHandler friendsListRefreshHandler;
    private List<UserBean> users;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        users = UserBean.from(Friend.getFriends());
        friendsListAdapter = new FriendsListAdapter(getActivity(),
                android.R.layout.simple_list_item_1, users);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friend, container, false);

        // Set the adapter
        listView = (AbsListView) view.findViewById(android.R.id.list);
        ((AdapterView<ListAdapter>) listView).setAdapter(friendsListAdapter);

        listView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (listener != null) {
            listener.onFragmentInteraction((UserBean) friendsListAdapter.getItem(position));
        }
    }

    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(UserBean user);
    }

    private class FriendsListRefreshHandler implements MessageHandler {
        @Override
        public void sendMessage(String target, String method, String message) {
            if (SyncHelper.MESSAGING_METHOD_ON_SYNCHRONIZATION.equals(method)
                    && (SyncHelper.MESSAGING_MESSAGE_SYNC_SUCCESS_FULL.equals(message)
                    || SyncHelper.MESSAGING_MESSAGE_SYNC_SUCCESS_PARTIAL.equals(message))) {
                List<UserBean> refreshedUsers = UserBean.from(Friend.getFriends());

                if (!refreshedUsers.equals(users)) {
                    friendsListAdapter.setItems(refreshedUsers);
                    friendsListAdapter.notifyDataSetChanged();
                }
            }
        }
    }
}
