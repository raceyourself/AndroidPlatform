package com.raceyourself.raceyourself.game;

import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.raceyourself.platform.utils.Format;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.BlankFragment;
import com.raceyourself.raceyourself.game.position_controllers.PositionController;

import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GameStatsPage1Fragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GameStatsPage1Fragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
@Slf4j
public class GameStatsPage1Fragment extends BlankFragment {

    @Getter
    private GameService gameService;  // passed in by the activity. Null when not bound (e.g. app is in the background).

    // timer and task to regularly refresh UI
    private Timer timer = new Timer();
    private UiTask task;

    // UI components
    private TextView aheadBehindTextView;
    private TextView aheadBehindLabel;
    private TextView remainingTextView;
    private TextView remainingLabel;

    // Time formatter
    private static final PeriodFormatter ACTIVITY_PERIOD_FORMAT = new PeriodFormatterBuilder()
            .minimumPrintedDigits(2)
            .printZeroAlways()
            .appendHours()
            .appendSuffix(":")
            .appendMinutes()
            .appendSuffix(":")
            .appendSeconds()
            .toFormatter();


    public GameStatsPage1Fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_game_stats_page1, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        log.trace("onViewCreated called");

        // find UI components we want to update from code
        aheadBehindTextView = (TextView)getActivity().findViewById(R.id.aheadBehindTextView);
        aheadBehindLabel = (TextView)getActivity().findViewById(R.id.aheadBehindLabel);
        remainingTextView = (TextView)getActivity().findViewById(R.id.timeRemainingTextView);
        remainingLabel = (TextView)getActivity().findViewById(R.id.timeRemainingLabel);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (task != null) task.cancel();
        task = new UiTask();
        timer.scheduleAtFixedRate(task, 0, 500);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (task != null) {
            task.cancel();
        }
    }

    public synchronized void setGameService(GameService gs) {
        this.gameService = gs;
    }


    private class UiTask extends TimerTask {
        public void run() {
            getActivity().runOnUiThread(new Runnable() {

                public void run() {

                    synchronized (GameStatsPage1Fragment.this) {

                        if (gameService == null)
                            return;  // cannot access game data till we're bound to the service

                        // update UI here
                        PositionController player = null;
                        PositionController opponent = null;
                        GameConfiguration configuration = gameService.getGameConfiguration();
                        for (PositionController p : gameService.getPositionControllers()) {
                            if (p.isLocalPlayer()) {
                                player = p;
                            } else {
                                opponent = p;
                                // TODO: update this to work with >1 opponent
                            }
                        }

                        // update ahead/behind textview
                        double aheadBehind = player.getRealDistance() - opponent.getRealDistance();
                        String aheadBehindText;
                        String aheadBehindLabelText;
                        if (gameService.getElapsedTime() < 0) {
                            // countdown (secs)
                            long countdown = (-gameService.getElapsedTime() / 1000) + 1;
                            aheadBehindText = Long.toString(countdown);
                            aheadBehindLabelText = "";
                        } else if (gameService.getElapsedTime() < 2000) {
                            // "GO!"
                            aheadBehindText = "GO!";
                            aheadBehindLabelText = "";
                        } else {
                            // ahead/behind distance (m)
                            aheadBehindText = Format.zeroDp(Math.abs(aheadBehind)) + "M";
                            aheadBehindLabelText = aheadBehind > 0 ? "AHEAD" : "BEHIND";
                        }
                        int aheadBehindColor = aheadBehind > 0 ? Color.parseColor("#88cca3") : Color.parseColor("#ce5557");
                        aheadBehindTextView.setText(aheadBehindText);
                        aheadBehindTextView.setTextColor(aheadBehindColor);

                        // update ahead/behind label
                        aheadBehindLabel.setText(aheadBehindLabelText);
                        aheadBehindLabel.setTextColor(aheadBehindColor);
                        //int backgroundResourceId = aheadBehind > 0 ? R.drawable.border_green_20px : R.drawable.border_red_20px;
                        //aheadBehindBackground.setImageResource(backgroundResourceId);

                        // update remaining textview
                        remainingLabel.setText(configuration.getGameType().getRemainingText());  // TODO: shouldn't update this every loop
                        switch (configuration.getGameType()) {
                            case TIME_CHALLENGE: {
                                String formatted = ACTIVITY_PERIOD_FORMAT.print(Duration.millis(player.getRemainingTime(configuration)).toPeriod());
                                remainingTextView.setText(formatted);
                                break;
                            }
                            case DISTANCE_CHALLENGE: {
                                remainingTextView.setText(Format.zeroDp(player.getRemainingDistance(configuration)));
                                break;
                            }
                        }
                    }

                }
            });
        }
    }



}
