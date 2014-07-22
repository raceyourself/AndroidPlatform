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
import android.widget.Toast;

import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.Track;
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

    @Setter
    private FriendView.OnFriendAction onFriendAction;

    public FriendListAdapter(
            @NonNull Context context, int textViewResourceId, @NonNull List<UserBean> items) {
        super(context, textViewResourceId, items);
        this.context = context;
        this.items = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FriendView friendView;
        if (convertView == null) {
            friendView = FriendView_.build(context);
        }
        else {
            friendView = (FriendView) convertView;
        }

        UserBean friend = getItem(position);

        friendView.bind(friend);
        friendView.setOnFriendAction(onFriendAction);

        return friendView;
    }
}
