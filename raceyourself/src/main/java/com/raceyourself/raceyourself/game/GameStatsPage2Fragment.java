package com.raceyourself.raceyourself.game;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.raceyourself.raceyourself.Format;
import com.raceyourself.raceyourself.R;
import com.raceyourself.raceyourself.UnitConversion;
import com.raceyourself.raceyourself.base.BlankFragment;
import com.raceyourself.raceyourself.game.position_controllers.PositionController;

import java.util.Timer;
import java.util.TimerTask;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * A simple {@link android.app.Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GameStatsPage2Fragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GameStatsPage2Fragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
@Slf4j
public class GameStatsPage2Fragment extends BlankFragment {

    @Getter
    @Setter
    private GameService gameService;  // passed in by the activity. Null when not bound (e.g. app is in the background).

    // timer and task to regularly refresh UI
    private Timer timer = new Timer();
    private UiTask task;

    // UI components
    private TextView distanceCompleteTextView;
    private TextView distanceCompleteLabel;
    private TextView currentPaceTextView;
    private TextView currentPaceLabel;
    private TextView averagePaceTextView;
    private TextView averagePaceLabel;


    public GameStatsPage2Fragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_game_stats_page2, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        log.trace("onViewCreated called");

        // find UI components we want to update from code
        distanceCompleteTextView = (TextView)getActivity().findViewById(R.id.distanceCompleteTextView);
        distanceCompleteLabel = (TextView)getActivity().findViewById(R.id.distanceCompleteLabel);
        currentPaceTextView = (TextView)getActivity().findViewById(R.id.currentPaceTextView);
        currentPaceLabel = (TextView)getActivity().findViewById(R.id.currentPaceLabel);
        averagePaceTextView = (TextView)getActivity().findViewById(R.id.averagePaceTextView);
        averagePaceLabel = (TextView)getActivity().findViewById(R.id.averagePaceLabel);
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

                    // find player position controller
                    PositionController player = null;
                    for (PositionController p : gameService.getPositionControllers()) {
                        if (p.isLocalPlayer()) {
                            player = p;
                            break;
                        }
                    }
                    if (player == null) { log.error("No local player found, cannot update fragment"); return; }

                    // update distance complete textview
                    distanceCompleteTextView.setText(Format.twoDp(UnitConversion.miles(player.getRealDistance())));

                    // update current pace textview
                    currentPaceTextView.setText(player.getCurrentSpeed() < 0.2f ? "-.-" : Format.oneDp(UnitConversion.minutesPerMile(player.getCurrentSpeed())));

                    // update average pace textview
                    currentPaceTextView.setText(player.getAverageSpeed() < 0.01f ? "-.-" : Format.oneDp(UnitConversion.minutesPerMile(player.getAverageSpeed())));

                }
            });
        }
    }



}
