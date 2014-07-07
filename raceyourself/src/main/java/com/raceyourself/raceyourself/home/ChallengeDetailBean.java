package com.raceyourself.raceyourself.home;

import android.os.Parcel;
import android.os.Parcelable;

import com.raceyourself.platform.models.Challenge;
import com.raceyourself.platform.models.Notification;
import com.raceyourself.platform.models.Track;
import com.raceyourself.platform.models.User;

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
}
