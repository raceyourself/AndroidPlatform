package com.raceyourself.raceyourself.matchmaking;

import android.animation.Animator;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.raceyourself.platform.models.AccessToken;
import com.raceyourself.platform.models.AutoMatches;
import com.raceyourself.platform.models.User;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.util.StringFormattingUtils;
import com.raceyourself.raceyourself.home.HomeActivity;
import com.raceyourself.raceyourself.home.feed.ChallengeDetailBean;

import lombok.extern.slf4j.Slf4j;

/**
 * Jesus wept.
 *
 * Created by Amerigo on 17/07/2014.
 */
@Slf4j
public class MatchmakingPopupController {
    // Duration for the matchmaking search
    int duration;

    RelativeLayout blackBg;

    // Main home activity
    HomeActivity homeActivity;

    // The three popups for matchmaking
    PopupWindow matchmakingFitnessPopup;
    PopupWindow matchmakingDurationPopup;
    PopupWindow matchmakingFindingPopup;

    PopupWindow blackBgWindow;

    // Inflater for the layouts of the popups
    LayoutInflater inflater;

    // Fitness view that gets inflated for fitness popup
    FitnessView fitnessView;

    // Finding opponent view that gets inflated for finding popup
    FindingView findingView;

    Animation fadeOutAnim;
    Animation fadeInAnim;
    Animation translateFromRightAnim;
    Animation translateToRightAnim;

    ImageView opponentProfilePic;

    // Challenge detail to pass onto the race
    ChallengeDetailBean challengeDetail;

    String fitness = "";

    // Boolean for race yourself
    private boolean raceYourself;

    int animationCount = 0;
    private DurationView durationView;

    public MatchmakingPopupController() {}

    public MatchmakingPopupController(HomeActivity homeActivity) {
        this.homeActivity = homeActivity;
        inflater = LayoutInflater.from(homeActivity);
        blackBg = new RelativeLayout(homeActivity);
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        blackBg.setLayoutParams(rlp);
        blackBg.setBackgroundColor(Color.parseColor("#a0000000"));
        blackBgWindow = new PopupWindow(blackBg);
        blackBgWindow.setAnimationStyle(R.style.popup_fade_in_out_animation);
        blackBgWindow.setWindowLayoutMode(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        User player = User.get(AccessToken.get().getUserId());
        fitness = player.getProfile().running_fitness;

        fadeInAnim = AnimationUtils.loadAnimation(homeActivity, R.anim.popup_fade_in);
        fadeOutAnim = AnimationUtils.loadAnimation(homeActivity, R.anim.popup_fade_out);
        translateFromRightAnim = AnimationUtils.loadAnimation(homeActivity, R.anim.popup_translate_from_right);
        translateToRightAnim = AnimationUtils.loadAnimation(homeActivity, R.anim.popup_translate_to_right);
    }

    public void displayFitnessPopup() {
        raceYourself = false;
        fitnessView = FitnessView_.build(homeActivity);
        matchmakingFitnessPopup = new PopupWindow(fitnessView);
        matchmakingFitnessPopup.setAnimationStyle(R.style.popup_translate_right_fade_out_animation);
        matchmakingFitnessPopup.setWindowLayoutMode(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        blackBgWindow.showAtLocation(
                homeActivity.getWindow().getDecorView().findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
        matchmakingFitnessPopup.showAtLocation(
                homeActivity.getWindow().getDecorView().findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
        AutoMatches.update();
    }

    public void onFitnessBtn(View view) {
        fitness = fitnessView.getFitness();
        if(fitness != null) {
            displayQuickmatchDurationPopup();
            matchmakingFitnessPopup.setAnimationStyle(R.style.popup_translate_right_fade_out_animation);
            matchmakingFitnessPopup.dismiss();
        }
    }

    public boolean isDisplaying() {
        if(matchmakingFindingPopup != null &&  matchmakingFindingPopup.isShowing()) {
            displayQuickmatchDurationPopup();
            matchmakingFindingPopup.dismiss();
            return true;
        } else if(matchmakingDurationPopup != null && matchmakingDurationPopup.isShowing()) {
            if (!raceYourself && fitness == null) {
                displayFitnessPopup();
            } else {
                blackBgWindow.dismiss();
            }
            matchmakingDurationPopup.dismiss();
            return true;
        } else if(matchmakingFitnessPopup != null && matchmakingFitnessPopup.isShowing()) {
            matchmakingFitnessPopup.dismiss();
            blackBgWindow.dismiss();
            return true;
        }

        return false;
    }

    public void displayRaceYourselfPopup() {
        displayDurationPopup(true);
    }

    public void displayQuickmatchDurationPopup() {
        raceYourself = false;
        displayDurationPopup(false);
    }

    public void displayDurationPopup(final boolean raceYourself) {
        this.raceYourself = raceYourself;

        if (raceYourself) {
            durationView = RaceYourselfDurationView_.build(homeActivity);
        } else {
            durationView = MatchmakingDurationView_.build(homeActivity);
        }

        if(!blackBgWindow.isShowing()) blackBgWindow.showAtLocation(
                homeActivity.getWindow().getDecorView().findViewById(android.R.id.content), Gravity.CENTER, 0, 0);

        matchmakingDurationPopup = new PopupWindow(durationView);
        matchmakingDurationPopup.setWindowLayoutMode(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        matchmakingDurationPopup.setAnimationStyle(R.style.popup_translate_right_fade_out_animation);

        matchmakingDurationPopup.showAtLocation(
                homeActivity.getWindow().getDecorView().getRootView(), Gravity.CENTER, 0, 0);
    }

    public void onDistanceClick() {
        durationView.onDistanceClick();
        if (raceYourself) {
            opponentProfilePic = durationView.getPlayerProfilePic();
            challengeDetail = durationView.getChallengeDetail();
            onOpponentSelect(true);
        } else {
            displayFindingPopup();
            matchmakingDurationPopup.dismiss();
        }
    }

    public void displayFindingPopup() {
        animationCount = 0;
        findingView = FindingView_.build(homeActivity, durationView.getDuration(), fitness);
//        findingView.findOpponent();

        if(matchmakingFindingPopup != null && matchmakingFindingPopup.isShowing()) matchmakingFindingPopup.dismiss();

        matchmakingFindingPopup = new PopupWindow(findingView);

        matchmakingFindingPopup.setAnimationStyle(R.style.popup_translate_right_fade_out_animation);

        matchmakingFindingPopup.setWindowLayoutMode(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        matchmakingFindingPopup.showAtLocation(homeActivity.getWindow().getDecorView().getRootView(),
                Gravity.CENTER, 0, 0);
    }

    public void onRaceClick() {
        challengeDetail = findingView.getChallengeDetail();
        opponentProfilePic = findingView.getOpponentProfilePic();
        onOpponentSelect(false);
    }

    public void onOpponentSelect(boolean raceYourself) {
        homeActivity.getPagerAdapter().getHomeFeedFragment().setSelectedChallenge(challengeDetail);
        TextView opponentName = (TextView) homeActivity.findViewById(R.id.opponentName);
        opponentName.setText(StringFormattingUtils.getForename(challengeDetail.getOpponent().getName()));

        // Clone profile image into root layout
        int[] location = new int[2];

        opponentProfilePic.getLocationOnScreen(location);

        final ViewGroup rl = (ViewGroup) homeActivity.findViewById(R.id.activity_home);
        int[] parent_location = new int[2];
        rl.getLocationOnScreen(parent_location);

        RelativeLayout.LayoutParams cp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        ViewGroup.LayoutParams pp = opponentProfilePic.getLayoutParams();
        cp.width = pp.width;
        cp.height = pp.height;
        final ImageView clone = new ImageView(rl.getContext());
        clone.setImageDrawable(opponentProfilePic.getDrawable());
        clone.setScaleType(opponentProfilePic.getScaleType());
        clone.setLayoutParams(cp);
        clone.setX(location[0] - parent_location[0]);
        clone.setY(location[1] - parent_location[1]);
        rl.addView(clone);

        // Animate to opponent versus location
        final ImageView opponent = (ImageView)rl.findViewById(R.id.opponentPic);
        final ImageView opponentRank = (ImageView)rl.findViewById(R.id.opponentRank);
        final Drawable rankDrawable = homeActivity.getResources().getDrawable(challengeDetail.getOpponent().getRankDrawable());
        opponent.getLocationOnScreen(location);
        clone.animate().x(location[0] - parent_location[0]).y(location[1] - parent_location[1]).setDuration(1500).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                opponent.setImageDrawable(clone.getDrawable());
                opponentRank.setImageDrawable(rankDrawable);
                opponentRank.setVisibility(View.VISIBLE);
                rl.removeView(clone);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                opponent.setImageDrawable(clone.getDrawable());
                opponentRank.setImageDrawable(rankDrawable);
                opponentRank.setVisibility(View.VISIBLE);
                rl.removeView(clone);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        if (!raceYourself) {
            matchmakingFindingPopup.dismiss();
            blackBgWindow.dismiss();
        } else {
            matchmakingDurationPopup.dismiss();
            blackBgWindow.dismiss();
        }
    }

    public void restartSearch() {
        findingView.restartSearch();
    }

}
