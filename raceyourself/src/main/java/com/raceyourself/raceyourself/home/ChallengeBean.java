package com.raceyourself.raceyourself.home;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.raceyourself.raceyourself.R;

import org.joda.time.Duration;

import java.util.Set;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 27/06/2014.
 */
@Slf4j
@Data
public class ChallengeBean implements Parcelable{
    private int challengeId;
    private String type;
    private int challengeGoal;

    public String getName(Context context) {
        return context.getString(R.string.label_duration_race);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public ChallengeBean() {}

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(challengeId);
        dest.writeString(type);
        dest.writeInt(challengeGoal);
    }

    public Duration getDuration() {
        return Duration.standardSeconds(challengeGoal);
    }

    private ChallengeBean(Parcel in) {
        this.challengeId = in.readInt();
        this.type = in.readString();
        this.challengeGoal = in.readInt();
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
