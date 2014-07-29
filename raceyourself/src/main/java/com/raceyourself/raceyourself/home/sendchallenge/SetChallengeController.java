package com.raceyourself.raceyourself.home.sendchallenge;

import com.raceyourself.raceyourself.base.DurationView;
import com.raceyourself.raceyourself.base.NewChallengeController;
import com.raceyourself.raceyourself.home.HomeActivity;
import com.raceyourself.raceyourself.home.UserBean;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 29/07/2014.
 */
@Slf4j
public class SetChallengeController extends NewChallengeController {

    private final HomeActivity homeActivity;
    private final UserBean friend;

    public SetChallengeController(HomeActivity homeActivity, UserBean friend) {
        super(homeActivity);
        this.homeActivity = homeActivity;
        this.friend = friend;
    }

    @Override
    public void onConfirmDuration() {
        end();
    }

    @Override
    public DurationView getDurationView() {
        SetChallengeView view = SetChallengeView_.build(homeActivity);
        view.bind(friend);
        return view;
    }

    @Override
    public void start() {
        displayDurationPrompt();
    }
}
