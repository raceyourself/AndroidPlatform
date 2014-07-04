package com.raceyourself.raceyourself.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

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
public class UserBean implements Comparable<UserBean>, Parcelable {
    private int id;
    private Bitmap profilePicture;
    private String name;
    private JoinStatus joinStatus;

    public UserBean() {}

    public UserBean(Friend friend) {
        this.id = friend.user_id;
        this.name = friend.getDisplayName();
        if (this.id > 0) this.joinStatus = JoinStatus.INVITE_SENT.MEMBER_NOT_YOUR_INVITE;
        else this.joinStatus = JoinStatus.NOT_MEMBER;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }

    public enum JoinStatus {
        MEMBER_YOUR_INVITE(R.string.ry_invite_accepted, 0),
        MEMBER_NOT_YOUR_INVITE(R.string.ry_member, 0),
        INVITE_SENT(R.string.ry_invite_sent, 1),
        NOT_MEMBER(R.string.not_ry_member, 2);

        private final int stringId;
        @Getter
        private final int order;

        JoinStatus(int stringId, int order) {
            this.stringId = stringId;
            this.order = order;
        }

        public String getStatusText(Context context) {
            return context.getString(stringId);
        }
    }
}
