package com.raceyourself.raceyourself.home;

import android.os.Parcel;
import android.os.Parcelable;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Amerigo on 04/07/2014.
 */
@Slf4j
@Data
public class ChallengeDetailBean implements Parcelable {

    private UserBean player;
    private UserBean opponent;
    private TrackSummaryBean playerTrack;
    private TrackSummaryBean opponentTrack;
    private ChallengeBean challenge;
    private String title;
    private int points;

    public void getChallengeNotificationDetail(ChallengeNotificationBean challengeNote) {

    }

    public ChallengeDetailBean() {}

    private ChallengeDetailBean(Parcel in) {
        this.player = in.readParcelable(UserBean.class.getClassLoader());
        this.opponent = in.readParcelable(UserBean.class.getClassLoader());
        this.playerTrack = in.readParcelable(TrackSummaryBean.class.getClassLoader());
        this.opponentTrack = in.readParcelable(TrackSummaryBean.class.getClassLoader());
        this.challenge = in.readParcelable(ChallengeBean.class.getClassLoader());
        this.title = in.readString();
        this.points = in.readInt();
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public ChallengeDetailBean createFromParcel(Parcel in) {
            return new ChallengeDetailBean(in);
        }

        public ChallengeDetailBean[] newArray(int size) {
            return new ChallengeDetailBean[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(player, flags);
        dest.writeParcelable(opponent, flags);
        dest.writeParcelable(playerTrack, flags);
        dest.writeParcelable(opponentTrack, flags);
        dest.writeParcelable(challenge, flags);
        dest.writeString(title);
        dest.writeInt(points);
    }
}
