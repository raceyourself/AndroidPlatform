package com.raceyourself.raceyourself.game;

import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.raceyourself.raceyourself.Format;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.base.BlankFragment;
import com.raceyourself.raceyourself.game.position_controllers.PositionController;

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
    @Setter
    private GameService gameService;  // passed in by the activity. Null when not bound (e.g. app is in the background).

    // timer and task to regularly refresh UI
    private Timer timer = new Timer();
    private UiTask task;

    // UI components
    private TextView aheadBehindTextView;
    private TextView aheadBehindLabel;
    private TextView remainingTextView;
    private TextView remainingLabel;
    private ImageView aheadBehindBackground;


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
        aheadBehindBackground = (ImageView)getActivity().findViewById(R.id.aheadBehindBackground);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (task != null) task.cancel();
        task = new UiTask();
        timer.scheduleAtFixedRate(task, 1000, 500);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (task != null) {
            task.cancel();
        }
    }



    private class UiTask extends TimerTask {
        public void run() {
            if (gameService == null) return;  // cannot access game data till we're bound to the service
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    // update UI here
                    PositionController player = null;
                    PositionController opponent = null;
                    GameStrategy strategy = gameService.getGameStrategy();
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
                    int aheadBehindColor = aheadBehind > 0 ? Color.rgb(0,255,0) : Color.rgb(255,0,0);
                    aheadBehindTextView.setText(Format.zeroDp(Math.abs(aheadBehind)));
                    aheadBehindTextView.setTextColor(aheadBehindColor);

                    // update ahead/behind label
                    String aheadBehindText = aheadBehind > 0 ? "AHEAD (M)" : "BEHIND (M)";
                    aheadBehindLabel.setText(aheadBehindText);
                    aheadBehindLabel.setTextColor(aheadBehindColor);
                    int backgroundResourceId = aheadBehind > 0 ? R.drawable.border_green_20px : R.drawable.border_red_20px;
                    aheadBehindBackground.setImageResource(backgroundResourceId);

                    // update remaining textview
                    remainingLabel.setText(strategy.getGameType().getRemainingText());  // TODO: shouldn't update this every loop
                    switch (strategy.getGameType()) {
                        case TIME_CHALLENGE: {
                            DateFormat df = new SimpleDateFormat("HH:mm:ss");
                            String formatted = df.format(new Date(player.getRemainingTime(strategy)));
                            remainingTextView.setText(Long.toString(player.getRemainingTime(strategy) / 1000));
                            break;
                        }
                        case DISTANCE_CHALLENGE: {
                            remainingTextView.setText(Format.zeroDp(player.getRemainingDistance(strategy)));
                            break;
                        }
                    }

                }
            });
        }
    }



}
