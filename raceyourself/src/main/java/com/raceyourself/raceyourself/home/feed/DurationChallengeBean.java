package com.raceyourself.raceyourself.home.feed;

import android.os.Parcelable;

import com.raceyourself.platform.models.Challenge;

import org.joda.time.Duration;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 27/06/2014.
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper=true)
public class DurationChallengeBean extends ChallengeBean implements Parcelable {

    private Duration duration;
    private double distanceMetres;
//
//    @Override
//    public String getName(Context context) {
//        return context.getString(R.string.label_duration_race);
//    }

    public DurationChallengeBean(Challenge challenge) {
        super(challenge);
    }
}
