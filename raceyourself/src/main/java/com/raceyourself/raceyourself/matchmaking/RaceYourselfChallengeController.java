package com.raceyourself.raceyourself.matchmaking;

import android.util.Pair;
import android.widget.ImageView;

import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.Track;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.base.DurationView;
import com.raceyourself.raceyourself.base.NewChallengeController;
import com.raceyourself.raceyourself.base.PreviouslyRunDurationView;
import com.raceyourself.raceyourself.game.GameConfiguration;
import com.raceyourself.raceyourself.home.HomeActivity;
import com.raceyourself.raceyourself.home.UserBean;
import com.raceyourself.raceyourself.home.feed.ChallengeBean;
import com.raceyourself.raceyourself.home.feed.ChallengeDetailBean;
import com.raceyourself.raceyourself.home.feed.TrackSummaryBean;
import com.raceyourself.raceyourself.home.sendchallenge.SetChallengeView;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 29/07/2014.
 */
@Slf4j
public class RaceYourselfChallengeController extends MatchmakingController
        implements DurationView.DurationViewListener {

    private final HomeActivity homeActivity;

    public RaceYourselfChallengeController(HomeActivity homeActivity) {
        super(homeActivity);
        this.homeActivity = homeActivity;
    }

    @Override
    public void onConfirmDuration() {
        ImageView opponentProfilePic = getDurationView().getPlayerProfilePic();
        onOpponentSelect(TODO, opponentProfilePic);
    }

    @Override
    public DurationView getDurationView() {
        return RaceYourselfDurationView_.build(homeActivity);
    }

    public void start() {
        displayDurationPrompt();
    }
}
