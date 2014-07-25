package com.raceyourself.raceyourself.matchmaking;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.home.feed.ChallengeDetailBean;
import com.squareup.picasso.Picasso;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by Amerigo on 25/07/2014.
 */
@Slf4j
@EViewGroup(R.layout.activity_select_duration)
public class MatchmakingDurationView extends DurationView {

    @ViewById
    TextView lengthWarning;

    @ViewById
    Button findBtn;

    @AfterViews
    public void afterMatchmakingView(){
        TextView furthestRunAfterTime = (TextView)findViewById(R.id.furthestRunAfterTime);
        furthestRunAfterTime.setVisibility(View.GONE);
        lengthWarning.setVisibility(View.GONE);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) findBtn.getLayoutParams();
        params.addRule(RelativeLayout.BELOW, R.id.matchmaking_distance_bar);
    }

    public MatchmakingDurationView(Context context) {
        super(context);
    }

    @Override
    public void onDistanceClick() {

    }

    @Override
    public ChallengeDetailBean getChallengeDetail() { return null; }
}
