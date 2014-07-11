package com.raceyourself.raceyourself.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.squareup.picasso.Picasso;

import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 10/07/2014.
 */
@Slf4j
class FriendsListAdapter extends ArrayAdapter<UserBean> {

    @Getter @Setter
    private List<UserBean> items;
    private Context context;

    public FriendsListAdapter(
            @NonNull Context context, int textViewResourceId, @NonNull List<UserBean> items) {
        super(context, textViewResourceId, items);
        this.context = context;
        this.items = items;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.fragment_friend_item, null);
        }

        UserBean friend = (UserBean) items.get(position);
        TextView itemView = (TextView) view.findViewById(R.id.friend_item_friend_name);
        itemView.setText(friend.getName());
        itemView = (TextView) view.findViewById(R.id.friend_item_friend_status);

        UserBean.JoinStatus joinStatus = friend.getJoinStatus();
        itemView.setText(joinStatus.getStatusText(context));
        TextView button = (TextView) view.findViewById(R.id.label_action_button);
        button.setText(joinStatus.getActionText(context));

        final ImageView opponentProfilePic = (ImageView) view.findViewById(R.id.friend_profile_pic);
        Picasso.with(context).load(friend.getProfilePictureUrl()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(opponentProfilePic);

        return view;
    }
}
