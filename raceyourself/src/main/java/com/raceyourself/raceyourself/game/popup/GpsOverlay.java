package com.raceyourself.raceyourself.game.popup;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
public class GpsOverlay {

    private GameService gameService;
    private int positionAccuracy = 1; // 1=gps_disabled, 2=no_fix, 3=bad_fix, 4=good_fix
    @Getter private boolean visible;

    private Activity activity;
    private ViewGroup layoutContainer;
    private View overlay;

    private TextView gameOverlayGpsTitle;
    private TextView gameOverlayGpsDescription;
    private ImageView gameOverlayGpsImage;
    private TextView gameOverlayGpsAction;
    private Button gameOverlayGpsCancelButton;
    private Button gameOverlayGpsActionButton;

    private View.OnTouchListener nullTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return true;
        }
    };

    public GpsOverlay(Activity activityToCover, ViewGroup layoutContainer) {
        this.activity = activityToCover;
        this.layoutContainer = layoutContainer;
    }

    public void setGameService(GameService gs) {
        gameService = gs;
        if (gameService != null) {
            gs.registerRegularUpdateListener(regularUpdateListener);
        }
    }

    public void popup() {

        // inflate the popup view
        activity.getLayoutInflater().inflate(R.layout.popup_game_gps, layoutContainer, true);
        overlay = activity.findViewById(R.id.gameOverlayGps);
        visible = true;

        gameOverlayGpsTitle = (TextView)overlay.findViewById(R.id.gameOverlayGpsTitle);
        gameOverlayGpsDescription = (TextView)overlay.findViewById(R.id.gameOverlayGpsDescription);
        gameOverlayGpsImage = (ImageView)overlay.findViewById(R.id.gameOverlayGpsImage);
        gameOverlayGpsAction = (TextView)overlay.findViewById(R.id.gameOverlayGpsAction);
        gameOverlayGpsCancelButton = (Button)overlay.findViewById(R.id.gameOverlayGpsCancelButton);
        gameOverlayGpsActionButton = (Button)overlay.findViewById(R.id.gameOverlayGpsActionButton);

        // consume any touches on the background to stop underlying view getting them
        overlay.setOnTouchListener(nullTouchListener);

        // click listener for action button
        gameOverlayGpsActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (positionAccuracy) {
                    case 1: {
                        Intent gpsOptionsIntent = new Intent(
                                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        activity.startActivity(gpsOptionsIntent);
                        break;
                    }
                    case 2: {
                        // nothing
                        break;
                    }
                    default: {
                        if (gameService != null) {
                            dismiss();  // dismiss the popup
                            gameService.start();  // and start the race
                        }

                    }
                }
            }
        });

        // click listener for cancel button
        gameOverlayGpsCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.finish();
            }
        });
    }

    public void dismiss() {
        layoutContainer.removeView(overlay);
        visible = false;
    }

    // called regularly to check/update state of popup
    private RegularUpdateListener regularUpdateListener = new RegularUpdateListener() {
        @Override
        public void onRegularUpdate() {
            log.trace("PositionAccuracy callback triggered");
            if (gameService == null) return;
            PositionController player = gameService.getLocalPlayer();
            if (player instanceof OutdoorPositionController) {
                OutdoorPositionController p = (OutdoorPositionController) player;
                positionAccuracy = 1;
                if (p.isLocationEnabled()) positionAccuracy++;
                if (p.isLocationAvailable()) positionAccuracy++;
                if (p.isLocationAccurateEnough()) positionAccuracy++;
            } else {
                positionAccuracy = 4;
            }
            log.trace("PositionAccuracy is " + positionAccuracy);
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isVisible()) {
                        log.trace("Updating GPS overlay");
                        switch (positionAccuracy) {
                            case 1:
                                gameOverlayGpsTitle.setText(R.string.gps_title_1);
                                gameOverlayGpsTitle.setTextColor(Color.parseColor("#ce5557"));
                                gameOverlayGpsDescription.setText(R.string.gps_description_1);
                                gameOverlayGpsImage.setImageResource(R.drawable.ic_gps_red1);
                                gameOverlayGpsAction.setText(R.string.gps_action_1);
                                gameOverlayGpsActionButton.setText(R.string.gps_button_1);
                                break;
                            case 2:
                                gameOverlayGpsTitle.setText(R.string.gps_title_2);
                                gameOverlayGpsTitle.setTextColor(Color.parseColor("#eab91e"));
                                gameOverlayGpsDescription.setText(R.string.gps_description_2);
                                gameOverlayGpsImage.setImageResource(R.drawable.ic_gps_yellow2);
                                gameOverlayGpsAction.setText(R.string.gps_action_2);
                                gameOverlayGpsActionButton.setText(R.string.gps_button_2);
                                break;
                            case 3:
                                gameOverlayGpsTitle.setText(R.string.gps_title_3);
                                gameOverlayGpsTitle.setTextColor(Color.parseColor("#eab91e"));
                                gameOverlayGpsDescription.setText(R.string.gps_description_3);
                                gameOverlayGpsImage.setImageResource(R.drawable.ic_gps_green3);
                                gameOverlayGpsAction.setText(R.string.gps_action_3);
                                gameOverlayGpsActionButton.setText(R.string.gps_button_3);
                                break;
                            case 4:
                                gameOverlayGpsTitle.setText(R.string.gps_title_4);
                                gameOverlayGpsTitle.setTextColor(Color.parseColor("#88cca3"));
                                gameOverlayGpsDescription.setText(R.string.gps_description_4);
                                gameOverlayGpsImage.setImageResource(R.drawable.ic_gps_green4);
                                gameOverlayGpsAction.setText(R.string.gps_action_4);
                                gameOverlayGpsActionButton.setText(R.string.gps_button_4);
                                break;
                        }

                        // if we have high accuracy, dismiss the dialog and start the race
                        if (positionAccuracy == 4) {
                            Timer timer = new Timer();
                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            dismiss();
                                        }
                                    });
                                    gameService.start();
                                }
                            }, 500);
                        }
                    }
                }
            });
        }
    }.setRecurrenceInterval(500);

}
