package com.raceyourself.raceyourself.matchmaking;

import android.view.View;
import android.widget.ImageView;

import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.AutoMatches;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.DurationView;
import com.raceyourself.raceyourself.base.NewChallengeController;
import com.raceyourself.raceyourself.home.HomeActivity;
import com.raceyourself.raceyourself.home.feed.ChallengeDetailBean;

import lombok.extern.slf4j.Slf4j;

/**
 * Controls process for choosing a matchmaking opponent.
 */
@Slf4j
public class AutomatchController extends MatchmakingController
        implements FitnessView.FitnessViewListener, FindingView.FindOpponentViewListener {

    int animationCount = 0;

    String fitness = "";

    HomeActivity homeActivity;

    private FindingView findingView;
    private FitnessView fitnessView;

    public AutomatchController(HomeActivity homeActivity) {
        super(homeActivity);
        this.homeActivity = homeActivity;
    }

    private void displayFitnessLevelPrompt() {
        if (fitnessView == null) {
            fitnessView = FitnessView_.build(homeActivity);
            fitnessView.setFitnessViewListener(this);
        }
        displayView(fitnessView);
        AutoMatches.update();
    }

    private void displayFindingOpponentDialog() {
        animationCount = 0;
        if (findingView == null) {
            findingView = FindingView_.build(homeActivity,
                    (int) MatchmakingDurationView.getDuration().getStandardMinutes(), fitness);
            findingView.setFindOpponentViewListener(this);
        }
//        findingView.findOpponent();
        displayView(findingView);
    }

    @Override
    public void start() {
        User player = User.get(AccessToken.get().getUserId());
        fitness = player.getProfile().running_fitness;

        User user = User.get(AccessToken.get().getUserId());
        if(user.getProfile().running_fitness == null) {
            displayFitnessLevelPrompt();
        } else {
            displayDurationPrompt();
        }
    }

    @Override
    public DurationView getDurationView() {
        return MatchmakingDurationView_.build(homeActivity);
    }

    @Override
    public void onConfirmFitness() {
        fitness = fitnessView.getFitness();
        if(fitness != null) {
            displayDurationPrompt();
        }
    }

    @Override
    public void onConfirmDuration() {
        displayFindingOpponentDialog();
    }

    @Override
    public void onConfirmOpponent() {
        ChallengeDetailBean challengeDetail = findingView.getChallengeDetail();
        ImageView opponentProfilePic = findingView.getOpponentProfilePic();
        onOpponentSelect(challengeDetail, opponentProfilePic);
    }

    @Override
    public void onSearchAgain() {
        findingView.restartSearch();
    }
}
