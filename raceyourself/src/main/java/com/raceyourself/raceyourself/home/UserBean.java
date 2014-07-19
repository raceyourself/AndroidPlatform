package com.raceyourself.raceyourself.home;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.raceyourself.platform.models.Friend;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.StringFormattingUtils;
import com.raceyourself.raceyourself.home.feed.HomeFeedRowBean;

import java.io.Serializable;
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
public class UserBean implements Comparable<UserBean>, Parcelable, Serializable, HomeFeedRowBean {
    private int id;
    private String uid;
    private String provider;
    private String profilePictureUrl;
    private String name;
    private String shortName;
    private Integer rank = null;
    private JoinStatus joinStatus;

    public static final String DEFAULT_NAME = "?";

    public static final int[] RANK_DRAWABLES = {R.drawable.icon_badge_rank_bronze_1, R.drawable.icon_badge_rank_bronze_2, R.drawable.icon_badge_rank_bronze_3,
                                                R.drawable.icon_badge_rank_silver_1, R.drawable.icon_badge_rank_silver_2, R.drawable.icon_badge_rank_silver_3,
                                                R.drawable.icon_badge_rank_gold_1, R.drawable.icon_badge_rank_gold_2, R.drawable.icon_badge_rank_gold_3};

    public UserBean() {}

    public UserBean(Friend friend) {
        this.id = friend.user_id;
        this.uid = friend.uid;
        this.provider = friend.provider;
        this.name = friend.getDisplayName();
        if (this.id > 0) {
            this.joinStatus = JoinStatus.INVITE_SENT.MEMBER_NOT_YOUR_INVITE;
            if (friend.getUser() != null) this.rank = friend.getUser().getRank();
        } else {
            this.joinStatus = JoinStatus.NOT_MEMBER;
        }
        profilePictureUrl = friend.photo;
    }

    public UserBean(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.shortName = StringFormattingUtils.getForenameAndInitial(user.getName());
        this.profilePictureUrl = user.getImage();
        this.rank = user.getRank();
    }

    public static int getRankDrawable(int rank) {
        return RANK_DRAWABLES[Math.min(Math.max(0, rank-1), RANK_DRAWABLES.length - 1)];
    }

    public int getRankDrawable() {
        return getRankDrawable(this.rank);
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

    public static List<UserBean> fromOnlyRYFriends(List<Friend> friends) {
        List<UserBean> beans = new ArrayList<UserBean>(friends.size());
        for(Friend friend : friends) {
            friend.includeUser();
            if(friend.user_id > 0) {
                beans.add(new UserBean(friend));
            }
        }
        Collections.sort(beans);
        return beans;
    }

    @Override
    @TargetApi(19)
    public int compareTo(UserBean another) {
        if (joinStatus.getOrder() != another.getJoinStatus().getOrder()) return Integer.compare(joinStatus.getOrder(), another.getJoinStatus().getOrder());
        return name.compareTo(another.name);
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public UserBean createFromParcel(Parcel in) {
            return new UserBean(in);
        }

        public UserBean[] newArray(int size) {
            return new UserBean[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(uid);
        dest.writeString(provider);
        dest.writeString(profilePictureUrl);
        dest.writeString(name);
        dest.writeString(shortName);
    }

    private UserBean(Parcel in) {
        this.id = in.readInt();
        this.uid = in.readString();
        this.provider = in.readString();
        this.profilePictureUrl = in.readString();
        this.name = in.readString();
        this.shortName = in.readString();
    }

    public enum JoinStatus {
        MEMBER_YOUR_INVITE(R.string.ry_invite_accepted, R.string.label_challenge_button, true, 0),
        MEMBER_NOT_YOUR_INVITE(R.string.ry_member, R.string.label_challenge_button, true, 0),
        INVITE_SENT(R.string.ry_invite_sent, R.string.label_invited_button, false, 1),
        NOT_MEMBER(R.string.not_ry_member, R.string.label_invite_button, false, 2);

        private final int statusStringId;
        @Getter
        private final int order;
        private final int actionStringId;
        @Getter
        private final boolean member;

        JoinStatus(int statusStringId, int actionStringId, boolean member, int order) {
            this.actionStringId = actionStringId;
            this.statusStringId = statusStringId;
            this.order = order;
            this.member = member;
        }

        public String getStatusText(Context context) {
            return context.getString(statusStringId);
        }
        public String getActionText(Context context) {
            return context.getString(actionStringId);
        }
    }
}
