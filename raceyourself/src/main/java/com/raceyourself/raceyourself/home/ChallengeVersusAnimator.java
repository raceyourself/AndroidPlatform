package com.raceyourself.raceyourself.home;

import android.animation.Animator;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nhaarman.listviewanimations.itemmanipulation.ExpandCollapseListener;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.home.feed.ExpandableChallengeListAdapter;
import com.raceyourself.raceyourself.home.feed.ChallengeNotificationBean;

import lombok.extern.slf4j.Slf4j;

/**
* Created by Duncan on 18/07/2014.
*/
@Slf4j
class ChallengeVersusAnimator implements ExpandCollapseListener {
    private Activity context;
    private final ExpandCollapseListener chained;
    private final ExpandableChallengeListAdapter adapter;

    // Delayed animation
    private final long DELAY = 500;
    private final Handler handler;
    private ChallengeNotificationBean item = null;
    private View view = null;
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (item == null || view == null) return;
            // Clone profile image into root layout
            ImageView profile = (ImageView)view.findViewById(R.id.challenge_notification_profile_pic);
            ImageView rankIcon = (ImageView)view.findViewById(R.id.rankIcon);
            int[] location = new int[2];
            profile.getLocationOnScreen(location);

            final RelativeLayout rl = (RelativeLayout) context.findViewById(R.id.activity_home);
            int[] parent_location = new int[2];
            rl.getLocationOnScreen(parent_location);

            RelativeLayout.LayoutParams cp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            ViewGroup.LayoutParams pp = profile.getLayoutParams();
            cp.width = pp.width;
            cp.height = pp.height;
            final ImageView clone = new ImageView(rl.getContext());
            clone.setImageDrawable(profile.getDrawable());
            clone.setScaleType(profile.getScaleType());
            clone.setLayoutParams(cp);
            clone.setX(location[0] - parent_location[0]);
            clone.setY(location[1] - parent_location[1]);
            rl.addView(clone);

            // Animate to opponent versus location
            final ImageView opponent = (ImageView)rl.findViewById(R.id.opponentPic);
            final ImageView opponentRank = (ImageView)rl.findViewById(R.id.opponentRank);
            final ChallengeNotificationBean opponentItem = item;
            final Drawable rankDrawable = rankIcon.getDrawable();
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

            item = null;
            view = null;
        }
    };

    public ChallengeVersusAnimator(Activity context, ExpandableChallengeListAdapter adapter) {
        this(context, adapter, null);
    }

    public ChallengeVersusAnimator(Activity context, ExpandableChallengeListAdapter adapter, ExpandCollapseListener chained) {
        this.context = context;
        this.adapter = adapter;
        this.chained = chained;
        this.handler = new Handler();
    }

    @Override
    public void onItemExpanded(int position) {
        if (this.item != null || this.view != null) {
            handler.removeCallbacks(runnable);
        }

        // Animate versus opponent after a delay (that allows the item to expand fully)
        this.view = adapter.getView(position);
        this.item = adapter.getItem(position);
        handler.postDelayed(runnable, DELAY);

        // Set versus opponent name immediately so the race now button makes sense
        final RelativeLayout rl = (RelativeLayout) context.findViewById(R.id.activity_home);
        final ImageView opponent = (ImageView)rl.findViewById(R.id.opponentPic);
        final TextView opponentName = (TextView)rl.findViewById(R.id.opponentName);
        final ImageView opponentRank = (ImageView)rl.findViewById(R.id.opponentRank);
        opponent.setImageDrawable(context.getResources().getDrawable(R.drawable.default_profile_pic));
        opponentName.setText(item.getOpponent().getName());
        opponentRank.setVisibility(View.INVISIBLE);

        // TODO: Set race now button immediately. Here or in ChallengeDetailView?

        if (chained != null) chained.onItemExpanded(position);
    }

    @Override
    public void onItemCollapsed(int position) {
        // Reset opponent
        final RelativeLayout rl = (RelativeLayout) context.findViewById(R.id.activity_home);
        final ImageView opponent = (ImageView)rl.findViewById(R.id.opponentPic);
        final TextView opponentName = (TextView)rl.findViewById(R.id.opponentName);
        final ImageView opponentRank = (ImageView)rl.findViewById(R.id.opponentRank);
        opponent.setImageDrawable(context.getResources().getDrawable(R.drawable.default_profile_pic));
        opponentName.setText("- ? -");
        opponentRank.setVisibility(View.INVISIBLE);

        // TODO: Reset race now button. Here or in ChallengeDetailView?

        if (chained != null) chained.onItemCollapsed(position);
    }
}
