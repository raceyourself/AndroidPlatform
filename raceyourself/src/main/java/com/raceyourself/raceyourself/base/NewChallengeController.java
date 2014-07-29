package com.raceyourself.raceyourself.base;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.home.HomeActivity;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by Duncan on 29/07/2014.
 */
@Slf4j
public abstract class NewChallengeController implements Cancellable, DurationView.DurationViewListener {

    private boolean resume;

    private PopupWindow blackBgWindow;
    private PopupWindow currentPopup;

    Animation fadeOutAnim;
    Animation fadeInAnim;
    Animation translateFromRightAnim;
    Animation translateToRightAnim;

    HomeActivity homeActivity;

    public NewChallengeController(HomeActivity homeActivity) {
        this.homeActivity = homeActivity;

        ViewGroup blackBg = new RelativeLayout(homeActivity);
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        blackBg.setLayoutParams(rlp);
        blackBg.setBackgroundColor(Color.parseColor("#a0000000"));

        blackBgWindow = makePopup(blackBg, R.style.popup_fade_in_out_animation);

        // TODO refs not used. Is this necessary?
        fadeInAnim = AnimationUtils.loadAnimation(homeActivity, R.anim.popup_fade_in);
        fadeOutAnim = AnimationUtils.loadAnimation(homeActivity, R.anim.popup_fade_out);
        translateFromRightAnim = AnimationUtils.loadAnimation(homeActivity, R.anim.popup_translate_from_right);
        translateToRightAnim = AnimationUtils.loadAnimation(homeActivity, R.anim.popup_translate_to_right);
    }

    private PopupWindow makePopup(View view, int animationResId) {
        PopupWindow popup = new PopupWindow(view);
        popup.setAnimationStyle(animationResId);
        popup.setWindowLayoutMode(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        return popup;
    }

    private void showPopup(PopupWindow popup) {
        popup.showAtLocation(
                homeActivity.getWindow().getDecorView().findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
    }

    public void displayDurationPrompt() {
        DurationView view = getDurationView();
        view.setDurationViewListener(this);
        displayView(view);
    }

    public void displayView(View view) {
        if(!blackBgWindow.isShowing())
            showPopup(blackBgWindow);

        PopupWindow popup = makePopup(view, R.style.popup_translate_right_fade_out_animation);
        if (currentPopup != null)
            currentPopup.dismiss();
        currentPopup = popup;

        showPopup(popup);
    }

    public void end() {
        currentPopup.dismiss();
        blackBgWindow.dismiss();
    }

    public boolean isShowing() {
        return blackBgWindow.isShowing();
    }

    public abstract DurationView getDurationView();

    public abstract void start();

    public void onCancel() {
        end();
    }

    public void onResume() {
        if (resume) {
            showPopup(blackBgWindow);
            showPopup(currentPopup);
        }
    }

    public void onPause() {
        resume = blackBgWindow.isShowing();
        end();
    }
}
