package com.raceyourself.raceyourself.home;

import android.content.Context;

import com.raceyourself.platform.models.Friend;
import com.raceyourself.raceyourself.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Intentionally cut down from C# and platform equivalent, to de-couple from our DB implementation.
 * Just what we need to display on screen.
 *
 * Created by Duncan on 27/06/2014.
 */
@Slf4j
@Data
public class UserBean implements Comparable<UserBean> {
    private int id;
    private String photoUrl;
    private String name;
    private JoinStatus joinStatus;

    public UserBean() {}

    public UserBean(Friend friend) {
        this.id = friend.user_id;
        this.name = friend.getDisplayName();
        if (this.id > 0)
            this.joinStatus = JoinStatus.INVITE_SENT.MEMBER_NOT_YOUR_INVITE;
        else
            this.joinStatus = JoinStatus.NOT_MEMBER;
        photoUrl = friend.photo;
    }

    public static List<UserBean> from(List<Friend> friends) {
        List<UserBean> beans = new ArrayList<UserBean>(friends.size());
        for(Friend friend : friends) {
            friend.includeUser();
            beans.add(new UserBean(friend));
        }
        Collections.sort(beans);
        return beans;
    }

    @Override
    public int compareTo(UserBean another) {
        if (joinStatus.getOrder() != another.getJoinStatus().getOrder()) return Integer.compare(joinStatus.getOrder(), another.getJoinStatus().getOrder());
        return name.compareTo(another.name);
    }

    public enum JoinStatus {
        MEMBER_YOUR_INVITE(R.string.ry_invite_accepted, R.string.label_challenge_button, 0),
        MEMBER_NOT_YOUR_INVITE(R.string.ry_member, R.string.label_challenge_button, 0),
        INVITE_SENT(R.string.ry_invite_sent, R.string.label_invited_button, 1),
        NOT_MEMBER(R.string.not_ry_member, R.string.label_invite_button, 2);

        private final int statusStringId;
        @Getter
        private final int order;
        private final int actionStringId;

        JoinStatus(int statusStringId, int actionStringId, int order) {
            this.actionStringId = actionStringId;
            this.statusStringId = statusStringId;
            this.order = order;
        }

        public String getStatusText(Context context) {
            return context.getString(statusStringId);
        }
        public String getActionText(Context context) {
            return context.getString(actionStringId);
        }
    }
}
