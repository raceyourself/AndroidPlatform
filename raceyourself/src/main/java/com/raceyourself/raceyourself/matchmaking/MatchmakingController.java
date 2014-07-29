package com.raceyourself.raceyourself.matchmaking;

import android.animation.Animator;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.NewChallengeController;
import com.raceyourself.raceyourself.base.util.StringFormattingUtils;
import com.raceyourself.raceyourself.home.HomeActivity;
import com.raceyourself.raceyourself.home.feed.ChallengeDetailBean;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 29/07/2014.
 */
@Slf4j
public abstract class MatchmakingController extends NewChallengeController {

    private HomeActivity homeActivity;

    // Challenge detail to pass onto the race
    @Getter
    ChallengeDetailBean challengeDetail;

    public MatchmakingController(HomeActivity homeActivity) {
        super(homeActivity);
        this.homeActivity = homeActivity;
    }

    public void onOpponentSelect(@NonNull ChallengeDetailBean challengeDetailBean,
                                 @NonNull ImageView opponentProfilePic) {
        homeActivity.getPagerAdapter().getHomeFeedFragment().setSelectedChallenge(challengeDetailBean);

        TextView opponentName = (TextView) homeActivity.findViewById(R.id.opponentName);
        opponentName.setText(StringFormattingUtils.getForename(challengeDetailBean.getOpponent().getName()));

        new ToVsAnimationListener(homeActivity, challengeDetailBean, opponentProfilePic).listen();

        end();
    }

    private class ToVsAnimationListener implements Animator.AnimatorListener {
        private final ImageView opponent;
        private final ImageView opponentRank;
        private final ViewGroup rl;
        private final ImageView clone;
        private final Drawable rankDrawable;
        private final int[] parent_location;
        private int[] location;

        private ChallengeDetailBean challengeDetailBean;

        private ToVsAnimationListener(
                @NonNull HomeActivity homeActivity,
                @NonNull ChallengeDetailBean challengeDetailBean,
                @NonNull ImageView opponentProfilePic) {
            this.challengeDetailBean = challengeDetailBean;

            // Clone profile image into root layout
            location = new int[2];

            opponentProfilePic.getLocationOnScreen(location);

            rl = (ViewGroup) homeActivity.findViewById(R.id.activity_home);
            parent_location = new int[2];
            rl.getLocationOnScreen(parent_location);

            RelativeLayout.LayoutParams cp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            ViewGroup.LayoutParams pp = opponentProfilePic.getLayoutParams();
            cp.width = pp.width;
            cp.height = pp.height;
            clone = new ImageView(rl.getContext());
            clone.setImageDrawable(opponentProfilePic.getDrawable());
            clone.setScaleType(opponentProfilePic.getScaleType());
            clone.setLayoutParams(cp);
            clone.setX(location[0] - parent_location[0]);
            clone.setY(location[1] - parent_location[1]);
            rl.addView(clone);

            // Animate to opponent versus location
            opponent = (ImageView)rl.findViewById(R.id.opponentPic);
            opponentRank = (ImageView)rl.findViewById(R.id.opponentRank);
            rankDrawable = homeActivity.getResources().getDrawable(
                    challengeDetailBean.getOpponent().getRankDrawable());
            opponent.getLocationOnScreen(location);

        }

        public void listen() {
            clone.animate().x(location[0] - parent_location[0]).y(location[1] - parent_location[1])
                    .setDuration(1500).setListener(this);
        }

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
    }
}
