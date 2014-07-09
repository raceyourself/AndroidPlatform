package com.raceyourself.raceyourself.home;

import android.content.Context;

import com.raceyourself.raceyourself.R;

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
public class DurationChallengeBean extends ChallengeBean {

    private Duration duration;
    private double distanceMetres;
//
//    @Override
//    public String getName(Context context) {
//        return context.getString(R.string.label_duration_race);
//    }
}
