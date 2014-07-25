package com.raceyourself.raceyourself.matchmaking;

import android.content.Context;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.home.feed.ChallengeDetailBean;
import com.raceyourself.raceyourself.home.sendchallenge.SetChallengeView;
import com.squareup.picasso.Picasso;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Amerigo on 25/07/2014.
 */
@Slf4j
@EViewGroup(R.layout.activity_select_duration)
public abstract class DurationView extends RelativeLayout implements SeekBar.OnSeekBarChangeListener {

    @Getter
    protected int duration;

    @ViewById(R.id.duration)
    TextView durationTextView;

    @ViewById(R.id.furthestRunNumber)
    TextView furthestRunTextView;

    @ViewById(R.id.matchmaking_distance_bar)
    SeekBar seekBar;

    @ViewById(R.id.playerProfilePic)
    @Getter
    ImageView opponentProfilePic;

    protected Context context;

    public DurationView(Context context) {
        super(context);
        this.context = context;

    }

    @AfterViews
    public void afterDurationView(){
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(30);

        User user = User.get(AccessToken.get().getUserId());

        ImageView playerImage = (ImageView)findViewById(R.id.playerProfilePic);
        String url = user.getImage();
        Picasso.with(context).load(url).placeholder(R.drawable.default_profile_pic).transform(new PictureUtils.CropCircle()).into(playerImage);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int stepSize = 6;
        progress = (Math.round(progress/ stepSize))* stepSize;
        seekBar.setProgress(progress);
        duration = ((progress / stepSize) + 1) * 5;
        if(duration == 0) {
            duration = 5;
        }
        durationTextView.setText(duration + "");

        furthestRunTextView.setText(getFurthestRunText());

        checkRaceYourself();
    }

    public String getFurthestRunText() {
        StringBuilder text = new StringBuilder();
        text.append(" ");
        text.append(duration);
        text.append(" mins");
        return text.toString();
    }

    public void checkRaceYourself(){}

    public abstract void onDistanceClick();

    public abstract ChallengeDetailBean getChallengeDetail();
}
