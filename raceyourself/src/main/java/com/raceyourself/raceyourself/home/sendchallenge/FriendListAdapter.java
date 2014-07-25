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

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.raceyourself.platform.gpstracker.SyncHelper;
import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.Challenge;
import com.raceyourself.platform.models.Track;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.home.UserBean;
import com.raceyourself.raceyourself.home.feed.ChallengeNotificationBean;
import com.raceyourself.raceyourself.home.feed.TrackSummaryBean;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * Created by Duncan on 10/07/2014.
 */
@Slf4j
public class FriendListAdapter extends ArrayAdapter<UserBean> implements StickyListHeadersAdapter {

    @Getter @Setter
    private List<UserBean> items;
    private Context context;

    @Setter
    private FriendView.OnFriendAction onFriendAction;

    private Set<Integer> usersAlreadySentChallenges;

    public FriendListAdapter(
            @NonNull Context context, int textViewResourceId, @NonNull List<UserBean> items) {
        super(context, textViewResourceId, items);
        this.context = context;
        this.items = items;
    }

    public void friendChallenged(int userId) {
        usersAlreadySentChallenges.add(userId);
    }

    public void setChallengeNotifications(@NonNull List<ChallengeNotificationBean> challengeNotifications) {
        int playerId = AccessToken.get().getUserId();
        usersAlreadySentChallenges = Sets.newHashSet();

        for (ChallengeNotificationBean notif : challengeNotifications) {
            if (!notif.isFromMe()) // filter out inbound notifs and non-player stuff (activity feed)
                continue;

            if (notif.getExpiry().isBeforeNow())
                continue;

            Challenge challenge = null;
            try {
                challenge = SyncHelper.getChallenge(notif.getChallenge().getDeviceId(),
                        notif.getChallenge().getChallengeId());
            } catch (SyncHelper.CouldNotFetchException e) {
                log.error("Could not fetch challenge from challenge notification", e);
            }

            if (challenge == null) {
                log.error("Retrieved challenge must not be null. Challenge ID={}",
                        notif.getChallenge().getChallengeId());
                continue;
            }

            int friendId = notif.getTo().getId();
            boolean friendAttemptedChallenge = false;
            for(Challenge.ChallengeAttempt attempt : challenge.getAttempts()) {
                if (friendId == attempt.user_id) {
                    friendAttemptedChallenge = true;
                }
            }
            if (!friendAttemptedChallenge)
                usersAlreadySentChallenges.add(friendId);
        }
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
        friendView.setChallengeSent(usersAlreadySentChallenges.contains(friend.getId()));
        friendView.bind(friend);
        friendView.setOnFriendAction(onFriendAction);

        return friendView;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.fragment_header, parent, false);
        }

        UserBean friend = getItem(position);
        String titleText = context.getString(friend.getJoinStatus().isMember() ?
                R.string.header_challenge_friend : R.string.header_invite_friend);

        TextView title = (TextView) convertView.findViewById(R.id.textView);
        title.setText(titleText);

        View missions = convertView.findViewById(R.id.missionsProgress);
        missions.setVisibility(View.GONE);

        return convertView;
    }

    @Override
    public long getHeaderId(int position) {
        UserBean friend = getItem(position);
        return friend.getJoinStatus().isMember() ? 1 : 0;
    }
}
