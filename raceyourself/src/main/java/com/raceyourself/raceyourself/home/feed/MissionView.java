package com.raceyourself.raceyourself.home.feed;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.raceyourself.platform.models.Mission;
import com.raceyourself.raceyourself.R;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 16/07/2014.
 */
@Slf4j
@EViewGroup(R.layout.fragment_mission_element)
public class MissionView extends LinearLayout {

    private Context context;

    @ViewById
    TextView missionName;
    @ViewById
    TextView missionDescription;
    @ViewById
    View missionClaim;
    @ViewById
    ProgressBar missionProgress;
    @ViewById
    TextView missionProgressText;
    @ViewById
    ImageView starLeft;
    @ViewById
    ImageView starRight;
    @ViewById
    ImageView starCenter;


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
        MissionBean.LevelBean level = missionBean.getCurrentLevel();
        missionName.setText(missionBean.getId());
        missionProgressText.setText(level.getProgressText());
        missionProgress.setProgress((int)level.getProgressPct());

        // Show description or claim "button"
        if (missionBean.getCurrentLevel().isCompleted() && !missionBean.getCurrentLevel().isClaimed()) {
            missionClaim.setVisibility(VISIBLE);
            missionDescription.setVisibility(INVISIBLE);
        } else {
            missionDescription.setText(level.getDescription());
            missionDescription.setVisibility(VISIBLE);
            missionClaim.setVisibility(INVISIBLE);
        }

        int effectiveLevel = level.getLevel();
        if (level.isClaimed()) effectiveLevel++;
        if (effectiveLevel > 1) starLeft.setImageDrawable(getResources().getDrawable(R.drawable.icon_star_filled_left));
        else starLeft.setImageDrawable(getResources().getDrawable(R.drawable.icon_star_empty_left));
        if (effectiveLevel > 2) starRight.setImageDrawable(getResources().getDrawable(R.drawable.icon_star_filled_right));
        else starRight.setImageDrawable(getResources().getDrawable(R.drawable.icon_star_empty_right));
        if (effectiveLevel > 3) starCenter.setImageDrawable(getResources().getDrawable(R.drawable.icon_star_filled_center));
        else starCenter.setImageDrawable(getResources().getDrawable(R.drawable.icon_star_empty_center));
    }
}
