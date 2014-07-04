package com.raceyourself.raceyourself.home;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;


import com.raceyourself.platform.models.Friend;
import com.raceyourself.raceyourself.R;
import com.squareup.picasso.Picasso;

import java.util.List;

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
    private ListAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: Change Adapter to display your content
        adapter = new FriendsListAdapter(getActivity(),
                android.R.layout.simple_list_item_1, UserBean.from(Friend.getFriends()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friend, container, false);

        // Set the adapter
        listView = (AbsListView) view.findViewById(android.R.id.list);
        ((AdapterView<ListAdapter>) listView).setAdapter(adapter);

        listView.setOnItemClickListener(this);

        return view;
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
            listener.onFragmentInteraction((UserBean)adapter.getItem(position));
        }
    }

    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(UserBean user);
    }

    public class FriendsListAdapter extends ArrayAdapter<UserBean> {

        private Context context;

        public FriendsListAdapter(Context context, int textViewResourceId, List<UserBean> items) {
            super(context, textViewResourceId, items);
            this.context = context;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.fragment_friend_item, null);
            }

            UserBean friend = (UserBean)adapter.getItem(position);
            TextView itemView = (TextView) view.findViewById(R.id.friend_item_friend_name);
            itemView.setText(friend.getName());
            itemView = (TextView) view.findViewById(R.id.friend_item_friend_status);
            itemView.setText(friend.getJoinStatus().getStatusText(context));

            ImageView opponentProfilePic = (ImageView) view.findViewById(R.id.friend_profile_pic);
            Picasso.with(context).load(friend.getPhotoUrl()).placeholder(R.drawable.icon_runner_green).into(opponentProfilePic);

            return view;
        }
    }
}
