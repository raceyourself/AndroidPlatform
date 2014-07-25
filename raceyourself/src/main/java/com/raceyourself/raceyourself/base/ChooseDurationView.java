package com.raceyourself.raceyourself.base;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.squareup.picasso.Picasso;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Used for multiple purposes - choosing duration for quickmatch and for setting a challenge.
 *
 * Created by Duncan on 08/07/2014.
 */
@Slf4j
@EViewGroup(R.layout.activity_select_duration)
public abstract class ChooseDurationView extends RelativeLayout implements SeekBar.OnSeekBarChangeListener {

    protected static final int MIN_DURATION_MINS = 5;
    protected static final int MAX_DURATION_MINS = 30;
    protected static final int STEP_SIZE_MINS = 5;

    @Getter(AccessLevel.PROTECTED)
    private int duration;

    @ViewById(R.id.duration)
    protected TextView textView;
    @ViewById(R.id.furthestRunNumber)
    protected TextView furthestRunTextView;
    @ViewById(R.id.matchmaking_distance_bar)
    protected SeekBar seekBar;
    @ViewById(R.id.playerProfilePic)
    protected ImageView playerImage;
    @ViewById(R.id.lengthWarning)
    protected TextView warning;

    private Context context;

    protected ChooseDurationView(Context context) {
        super(context);
        this.context = context;
    }

    @AfterViews
    protected void afterViews() {
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(MAX_DURATION_MINS);

        User user = User.get(AccessToken.get().getUserId());
        String url = user.getImage();
        Picasso
            .with(context)
            .load(url)
            .placeholder(R.drawable.default_profile_pic)
            .transform(new PictureUtils.CropCircle())
            .into(playerImage);

        // Non-empty string in XML for ease of layout... but needs to be initialised to empty string.
        warning.setText("");
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int nSteps = 6;
        progress = (Math.round(progress / nSteps))* nSteps;
        seekBar.setProgress(progress);
        duration = ((progress / nSteps) + 1) * MIN_DURATION_MINS;
        if(duration == 0) {
            duration = MIN_DURATION_MINS;
        }
        textView.setText(duration + "");
        furthestRunTextView.setText(duration + "mins?");
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    public abstract void onMatchClick(View view);
}
