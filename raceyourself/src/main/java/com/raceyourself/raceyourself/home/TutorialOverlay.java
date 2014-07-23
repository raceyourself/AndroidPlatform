package com.raceyourself.raceyourself.home;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.game.GameService;

import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

/**
 * Created by benlister on 23/07/2014.
 */
public class TutorialOverlay {

    @Getter private boolean visible = false;
    private List<View> speechBubbles = new ArrayList<View>();
    private int currentSpeechBubble = 0;

    private Activity activity;
    private ViewGroup layoutContainer;
    private View overlay;

    // UI components
    TextView welcomeText;
    TextView homeText;
    TextView opponentText;
    TextView runButtonText;
    TextView storeText;
    TextView automatchText;
    TextView missionText;
    TextView runListText;

    public TutorialOverlay(Activity activityToCover, ViewGroup layoutContainer) {
        this.activity = activityToCover;
        this.layoutContainer = layoutContainer;
    }

    public void popup() {

        // inflate the popup view
        activity.getLayoutInflater().inflate(R.layout.overlay_home_tutorial, layoutContainer, true);
        overlay = layoutContainer.findViewById(R.id.tutorialOverlay);
        visible = true;

        // a touch anywhere on the screen will trigger the next speech bubble
        overlay.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                onNextButtonClick(view);
                return true;  // don't allow touch through to homescreen
            }
        });

        // get references to all the UI elements
        welcomeText = (TextView)overlay.findViewById(R.id.welcomeText);
        homeText = (TextView)overlay.findViewById(R.id.homeText);
        opponentText = (TextView)overlay.findViewById(R.id.opponentText);
        runButtonText = (TextView)overlay.findViewById(R.id.runButtonText);
        storeText = (TextView)overlay.findViewById(R.id.storeText);
        automatchText = (TextView)overlay.findViewById(R.id.automatchText);
        missionText = (TextView)overlay.findViewById(R.id.missionText);
        runListText = (TextView)overlay.findViewById(R.id.runListText);

        // ordered list of the speech bubbles to show
        speechBubbles.add(welcomeText);
        speechBubbles.add(homeText);
        speechBubbles.add(opponentText);
        speechBubbles.add(runButtonText);
        speechBubbles.add(storeText);
        speechBubbles.add(automatchText);
        speechBubbles.add(missionText);
        speechBubbles.add(runListText);

    }


    public void onNextButtonClick(View view) {
        if (currentSpeechBubble + 1 > speechBubbles.size()-1) {
            dismiss();
        } else {
            speechBubbles.get(currentSpeechBubble).setVisibility(View.GONE);
            speechBubbles.get(currentSpeechBubble + 1).setVisibility(View.VISIBLE);
            currentSpeechBubble++;
        }
    }

    public void dismiss() {
        layoutContainer.removeView(overlay);
        visible = false;
    }
}
