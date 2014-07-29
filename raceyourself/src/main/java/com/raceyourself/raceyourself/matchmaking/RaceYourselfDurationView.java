package com.raceyourself.raceyourself.matchmaking;

import android.content.Context;
import android.util.Pair;
import android.widget.ImageView;
import android.widget.TextView;

import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.Track;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.PreviouslyRunDurationView;
import com.raceyourself.raceyourself.game.GameConfiguration;
import com.raceyourself.raceyourself.home.UserBean;
import com.raceyourself.raceyourself.home.feed.ChallengeBean;
import com.raceyourself.raceyourself.home.feed.ChallengeDetailBean;
import com.raceyourself.raceyourself.home.feed.TrackSummaryBean;
import com.raceyourself.raceyourself.home.sendchallenge.SetChallengeView;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Amerigo on 25/07/2014.
 */
@Slf4j
@EViewGroup(R.layout.activity_select_duration)
public class RaceYourselfDurationView extends PreviouslyRunDurationView {

    @Getter // TODO set this challenge as the active challenge somehow.
    ChallengeDetailBean challengeDetail;

    public RaceYourselfDurationView(Context context) {
        super(context);
    }

    @Click(R.id.okButton)
    public void confirmDuration() {
        User player = User.get(AccessToken.get().getUserId());
        UserBean playerBean = new UserBean(player);

        GameConfiguration gameConfiguration = new GameConfiguration.GameStrategyBuilder(
                GameConfiguration.GameType.TIME_CHALLENGE).targetTime(
                getDuration().getMillis()).countdown(2999).build();

        Pair<Track,SetChallengeView.MatchQuality> p = getAvailableOwnTracksMap().get(
                (int) getDuration().getStandardMinutes());

        TrackSummaryBean opponentTrack = new TrackSummaryBean(p.first, gameConfiguration);

        ChallengeBean challengeBean = new ChallengeBean(null);
        challengeBean.setType("duration");
        challengeBean.setChallengeGoal((int) getDuration().getStandardSeconds());
        challengeBean.setPoints(20000);

        challengeDetail = new ChallengeDetailBean();
        challengeDetail.setOpponent(playerBean);
        challengeDetail.setPlayer(playerBean);
        challengeDetail.setOpponentTrack(opponentTrack);
        challengeDetail.setChallenge(challengeBean);

        super.confirmDuration();
    }

    protected int getButtonTextResId() {
        return R.string.raceyourself_button;
    }

    protected int getChallengeTextResId() {
        return R.string.duration_description_race_yourself;
    }
}
