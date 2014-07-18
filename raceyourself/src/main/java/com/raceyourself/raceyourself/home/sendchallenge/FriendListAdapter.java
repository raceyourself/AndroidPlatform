package com.raceyourself.raceyourself.home.sendchallenge;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.home.UserBean;
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
public class FriendListAdapter extends ArrayAdapter<UserBean> {

    @Getter @Setter
    private List<UserBean> items;
    private Context context;

    public FriendListAdapter(
            @NonNull Context context, int textViewResourceId, @NonNull List<UserBean> items) {
        super(context, textViewResourceId, items);
        this.context = context;
        this.items = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.fragment_friend_item, null);
        }

        final UserBean friend = items.get(position);
        TextView itemView = (TextView) view.findViewById(R.id.playerName);
        itemView.setText(friend.getName());

        ImageView opponentProfilePic = (ImageView) view.findViewById(R.id.playerProfilePic);
        Picasso.with(context).load(friend.getProfilePictureUrl()).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(opponentProfilePic);

        Button button = (Button)view.findViewById(R.id.challengeBtn);
        TextView subtitle = (TextView) view.findViewById(R.id.playerSubtitle);
        ImageView rankIcon = (ImageView)view.findViewById(R.id.rankIcon);
        if(friend.getJoinStatus() == UserBean.JoinStatus.MEMBER_NOT_YOUR_INVITE || friend.getJoinStatus() == UserBean.JoinStatus.MEMBER_YOUR_INVITE) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, SetChallengeActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("opponent", friend);
                    intent.putExtras(bundle);
                    context.startActivity(intent);
                }
            });
            button.setVisibility(View.VISIBLE);
            subtitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.icon_coin_small, 0, 0, 0);
            subtitle.setText("500");
            subtitle.setTextColor(Color.parseColor("#ffecbb1e"));
            if (friend.getRank() != null) {
                rankIcon.setImageDrawable(view.getResources().getDrawable(friend.getRankDrawable()));
                rankIcon.setVisibility(View.VISIBLE);
            } else {
                rankIcon.setVisibility(View.INVISIBLE);
            }
        } else {
            button.setVisibility(View.INVISIBLE);
            subtitle.setCompoundDrawables(null, null, null, null);
            subtitle.setText("Not a member");
            subtitle.setTextColor(Color.parseColor("#a29f94"));
            rankIcon.setVisibility(View.INVISIBLE);
        }
        return view;
    }
}
