package com.raceyourself.raceyourself.home.sendchallenge;

import com.raceyourself.raceyourself.base.DurationView;
import com.raceyourself.raceyourself.base.NewChallengeController;
import com.raceyourself.raceyourself.home.HomeActivity;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 29/07/2014.
 */
@Slf4j
public class SetChallengeController extends NewChallengeController {

    private HomeActivity homeActivity;

    public SetChallengeController(HomeActivity homeActivity) {
        super(homeActivity);
        this.homeActivity = homeActivity;
    }

    @Override
    public void onConfirmDuration() {
        end();
    }

    @Override
    public DurationView getDurationView() {
        return SetChallengeView_.build(homeActivity);
    }

    @Override
    public void start() {
        displayDurationPrompt();
    }
}
