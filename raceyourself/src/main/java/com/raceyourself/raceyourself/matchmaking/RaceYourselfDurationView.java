package com.raceyourself.raceyourself.matchmaking;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.Track;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.PreviouslyRunDurationView;
import com.raceyourself.raceyourself.base.util.PictureUtils;
import com.raceyourself.raceyourself.game.GameConfiguration;
import com.raceyourself.raceyourself.home.HomeActivity;
import com.raceyourself.raceyourself.home.HomeActivity_;
import com.raceyourself.raceyourself.home.UserBean;
import com.raceyourself.raceyourself.home.feed.ChallengeBean;
import com.raceyourself.raceyourself.home.feed.ChallengeDetailBean;
import com.raceyourself.raceyourself.home.feed.TrackSummaryBean;
import com.raceyourself.raceyourself.home.sendchallenge.SetChallengeView;
import com.squareup.picasso.Picasso;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import java.util.SortedMap;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Amerigo on 25/07/2014.
 */
@Slf4j
@EViewGroup(R.layout.activity_select_duration)
public class RaceYourselfDurationView extends PreviouslyRunDurationView {

    @ViewById
    TextView lengthWarning;

    @ViewById
    Button findBtn;

    @ViewById(R.id.playerProfilePic)
    @Getter
    ImageView opponentProfilePic;

    @ViewById(R.id.furthestRunText)
    TextView furthestRunBeforeDurationText;

    ChallengeDetailBean challengeDetail;

    @AfterViews
    public void afterViews() {
        furthestRunBeforeDurationText.setText(R.string.duration_description_raceyourself);

        TextView furthestRunAfterTime = (TextView)findViewById(R.id.furthestRunAfterTime);
        furthestRunAfterTime.setVisibility(View.VISIBLE);

        lengthWarning.setVisibility(View.VISIBLE);
    }

    public RaceYourselfDurationView(Context context) {
        super(context);
    }

    @Override
    public void onDistanceClick() {
        User player = User.get(AccessToken.get().getUserId());
        UserBean playerBean = new UserBean(player);

        GameConfiguration gameConfiguration = new GameConfiguration.GameStrategyBuilder(
                GameConfiguration.GameType.TIME_CHALLENGE).targetTime(duration.toStandardDuration().getMillis()).countdown(2999).build();

        // TODO refactor to avoid this dependency on SetChallengeView.
        Pair<Track,SetChallengeView.MatchQuality> p = availableOwnTracksMap.get(duration);

        TrackSummaryBean opponentTrack = new TrackSummaryBean(p.first, gameConfiguration);

        ChallengeBean challengeBean = new ChallengeBean(null);
        challengeBean.setType("duration");
        challengeBean.setChallengeGoal(duration * 60);
        challengeBean.setPoints(20000);

        challengeDetail = new ChallengeDetailBean();
        challengeDetail.setOpponent(playerBean);
        challengeDetail.setPlayer(playerBean);
        challengeDetail.setOpponentTrack(opponentTrack);
        challengeDetail.setChallenge(challengeBean);
    }

    protected int getButtonTextResId() {
        return R.string.raceyourself_button;
    }

    protected int getChallengeTextResId() {
        return R.string.duration_description_race_yourself;
    }

    @Override
    public ChallengeDetailBean getChallengeDetail() {
        return challengeDetail;
    }
}
