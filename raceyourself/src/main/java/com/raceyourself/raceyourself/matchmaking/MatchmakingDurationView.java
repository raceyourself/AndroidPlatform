package com.raceyourself.raceyourself.matchmaking;

import android.content.Context;

import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.DurationView;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EViewGroup;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by Amerigo on 25/07/2014.
 */
@Slf4j
@EViewGroup(R.layout.activity_select_duration)
public class MatchmakingDurationView extends DurationView {

    public MatchmakingDurationView(Context context) {
        super(context);
    }

    protected int getButtonTextResId() {
        return R.string.find_opponent_button;
    }
}
