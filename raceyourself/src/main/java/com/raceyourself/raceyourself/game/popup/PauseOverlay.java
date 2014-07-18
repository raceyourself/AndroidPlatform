package com.raceyourself.raceyourself.game.popup;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.game.GameService;
import com.raceyourself.raceyourself.game.event_listeners.RegularUpdateListener;
import com.raceyourself.raceyourself.game.position_controllers.OutdoorPositionController;
import com.raceyourself.raceyourself.game.position_controllers.PositionController;

import java.util.Timer;
import java.util.TimerTask;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by benlister on 18/07/2014.
 */
@Slf4j
public class PauseOverlay {

    private GameService gameService;
    @Getter private boolean visible;

    private Activity activity;
    private ViewGroup layoutContainer;
    private View overlay;

    private ImageButton gameOverlayPauseContinueButton;
    private ImageButton gameOverlayPauseQuitButton;

    private View.OnTouchListener nullTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return true;
        }
    };

    public PauseOverlay(Activity activityToCover, ViewGroup layoutContainer) {
        this.activity = activityToCover;
        this.layoutContainer = layoutContainer;
    }

    public void setGameService(GameService gs) {
        gameService = gs;
    }

    public void popup() {

        // inflate the popup view
        activity.getLayoutInflater().inflate(R.layout.popup_game_pause, layoutContainer, true);
        overlay = activity.findViewById(R.id.gameOverlayPause);
        visible = true;

        gameOverlayPauseContinueButton = (ImageButton)overlay.findViewById(R.id.gameOverlayPauseContinueButton);
        gameOverlayPauseQuitButton = (ImageButton)overlay.findViewById(R.id.gameOverlayPauseQuitButton);

        // consume any touches on the background to stop underlying view getting them
        overlay.setOnTouchListener(nullTouchListener);

        // click listener for continue button
        gameOverlayPauseContinueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                log.info("Continue pressed, un-pausing game");
                if (gameService != null) gameService.start();
                dismiss();
            }
        });

        // click listener for quit button
        gameOverlayPauseQuitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                log.info("Quit pressed, exiting GameActivity");
                dismiss();
                activity.finish();
            }
        });
    }

    public void dismiss() {
        layoutContainer.removeView(overlay);
        visible = false;
    }

}
