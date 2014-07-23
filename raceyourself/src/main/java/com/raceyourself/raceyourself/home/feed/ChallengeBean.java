package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.raceyourself.platform.models.Challenge;
import com.raceyourself.raceyourself.R;

import org.joda.time.Duration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 27/06/2014.
 */
@Slf4j
@Data
public class ChallengeBean implements Parcelable{
    private final int deviceId;
    private final int challengeId;
    private String type;
    private int challengeGoal;
    private int points;

    public String getName(Context context) {
        return context.getString(R.string.label_duration_race);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public ChallengeBean(Challenge challenge) {
        if (challenge != null) {
            this.deviceId = challenge.device_id;
            this.challengeId = challenge.challenge_id;
            this.points = challenge.points_awarded;
        } else {
            // Synthetic challenge
            this.deviceId = 0;
            this.challengeId = 0;
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(deviceId);
        dest.writeInt(challengeId);
        dest.writeString(type);
        dest.writeInt(challengeGoal);
        dest.writeInt(points);
    }

    public Duration getDuration() {
        return Duration.standardSeconds(challengeGoal);
    }

    private ChallengeBean(Parcel in) {
        this.deviceId = in.readInt();
        this.challengeId = in.readInt();
        this.type = in.readString();
        this.challengeGoal = in.readInt();
        this.points = in.readInt();
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public ChallengeBean createFromParcel(Parcel in) {
            return new ChallengeBean(in);
        }

        public ChallengeBean[] newArray(int size) {
            return new ChallengeBean[size];
        }
    };
}
