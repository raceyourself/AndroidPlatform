package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.raceyourself.raceyourself.R;

import org.androidannotations.annotations.EViewGroup;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 16/07/2014.
 */
@Slf4j
@EViewGroup(R.layout.fragment_mission_element)
public class MissionView extends LinearLayout {

    private Context context;

    public MissionView(Context context) {
        super(context);
        this.context = context;
    }

    public MissionView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        this.context = context;
    }

    public MissionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void bind(MissionBean missionBean) {

    }
}
