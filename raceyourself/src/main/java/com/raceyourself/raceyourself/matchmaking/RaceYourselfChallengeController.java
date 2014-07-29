package com.raceyourself.raceyourself.matchmaking;

import android.widget.ImageView;

import com.raceyourself.raceyourself.base.DurationView;
import com.raceyourself.raceyourself.home.HomeActivity;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 29/07/2014.
 */
@Slf4j
public class RaceYourselfChallengeController extends MatchmakingController
        implements DurationView.DurationViewListener {

    private final HomeActivity homeActivity;

    private RaceYourselfDurationView raceYourselfDurationView;

    public RaceYourselfChallengeController(HomeActivity homeActivity) {
        super(homeActivity);
        this.homeActivity = homeActivity;
    }

    @Override
    public void onConfirmDuration() {
        ImageView opponentProfilePic = getDurationView().getPlayerProfilePic();
        onOpponentSelect(raceYourselfDurationView.getChallengeDetail(), opponentProfilePic);
    }

    @Override
    public DurationView getDurationView() {
        return raceYourselfDurationView = RaceYourselfDurationView_.build(homeActivity);
    }

    @Override
    public void start() {
        displayDurationPrompt();
    }
}
