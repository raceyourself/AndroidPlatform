package com.raceyourself.raceyourself.home;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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
    TextView continueText;
    ImageView continueIcon;
    TextView welcomeText;
    TextView homeText1;
    TextView homeText2;
    TextView homeText3;
    ViewGroup vsRow;
    TextView opponentText1;
    TextView opponentText2;
    ImageView runButtonIcon;
    TextView runButtonText;
    TextView storeText;
    TextView automatchText;

    public TutorialOverlay(Activity activityToCover, ViewGroup layoutContainer) {
        this.activity = activityToCover;
        this.layoutContainer = layoutContainer;
    }

    public void popup() {

        // inflate the popup view
        activity.getLayoutInflater().inflate(R.layout.overlay_home_tutorial, layoutContainer, true);
        overlay = layoutContainer.findViewById(R.id.tutorialOverlay);
        visible = true;

//        // a touch anywhere on the screen will trigger the next speech bubble
//        overlay.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                // nothing
//                return true; // consume event
//            }
//        });

        // a touch anywhere on the screen will trigger the next speech bubble
        overlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onNextButtonClick(view);
            }
        });

        // get references to all the UI elements
        continueText = (TextView)overlay.findViewById(R.id.continueText);
        continueIcon = (ImageView)overlay.findViewById(R.id.continueIcon);
        welcomeText = (TextView)overlay.findViewById(R.id.welcomeText);
        homeText1 = (TextView)overlay.findViewById(R.id.homeText1);
        homeText2 = (TextView)overlay.findViewById(R.id.homeText2);
        homeText3 = (TextView)overlay.findViewById(R.id.homeText3);
        vsRow = (ViewGroup)overlay.findViewById(R.id.vsRow);
        opponentText1 = (TextView)overlay.findViewById(R.id.opponentText1);
        opponentText2 = (TextView)overlay.findViewById(R.id.opponentText2);
        runButtonIcon = (ImageView)overlay.findViewById(R.id.runButtonIcon);
        runButtonText = (TextView)overlay.findViewById(R.id.runButtonText);
        storeText = (TextView)overlay.findViewById(R.id.storeText);
        automatchText = (TextView)overlay.findViewById(R.id.automatchText);

        // ordered list of the speech bubbles to show
        speechBubbles.add(welcomeText);
        speechBubbles.add(homeText1);
        speechBubbles.add(homeText2);
        speechBubbles.add(homeText3);
        speechBubbles.add(opponentText1);
        speechBubbles.add(opponentText2);
        speechBubbles.add(runButtonText);
        speechBubbles.add(storeText);
        speechBubbles.add(automatchText);

    }


    public void onNextButtonClick(View view) {
        if (currentSpeechBubble + 1 > speechBubbles.size()-1) {
            dismiss();
        } else {
            speechBubbles.get(currentSpeechBubble).setVisibility(View.GONE);
            speechBubbles.get(currentSpeechBubble + 1).setVisibility(View.VISIBLE);
            currentSpeechBubble++;

            // remove extras for old speech bubble
            if (speechBubbles.get(currentSpeechBubble-1).getId() == R.id.welcomeText) {
                continueIcon.setVisibility(View.GONE);
                continueText.setVisibility(View.GONE);
            }
            if (speechBubbles.get(currentSpeechBubble-1).getId() == R.id.runButtonText) {
                vsRow.setVisibility(View.GONE);
                runButtonIcon.setVisibility(View.GONE);
            }

            // add extras for new speech bubble
            if (speechBubbles.get(currentSpeechBubble).getId() == R.id.runButtonText) {
                runButtonIcon.setVisibility(View.VISIBLE);
            }
            if (speechBubbles.get(currentSpeechBubble).getId() == R.id.opponentText1) {
                vsRow.setVisibility(View.VISIBLE);
            }
        }
    }

    public void dismiss() {
        layoutContainer.removeView(overlay);
        visible = false;
    }
}
