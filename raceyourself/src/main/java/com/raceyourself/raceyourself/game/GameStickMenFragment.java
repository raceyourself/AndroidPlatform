package com.raceyourself.raceyourself.game;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.raceyourself.platform.utils.Format;
import com.raceyourself.raceyourself.R;
import com.raceyourself.platform.utils.UnitConversion;
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
 * {@link com.raceyourself.raceyourself.game.GameStickMenFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link com.raceyourself.raceyourself.game.GameStickMenFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
@Slf4j
public class GameStickMenFragment extends BlankFragment {

    @Getter
    @Setter
    private GameService gameService;  // passed in by the activity. Null when not bound (e.g. app is in the background).

    // timer and task to regularly refresh UI
    private Timer timer = new Timer();
    private UiTask task;

    // UI components
    private RelativeLayout stickMenLayout;
    private ImageView playerStickMan;
    private ImageView opponentStickMan;
    private ProgressBar playerProgressbar;
    private ProgressBar opponentProgressbar;
    private TextView goalTextView;
    private int fragmentWidth;

    public GameStickMenFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_game_stick_men, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        log.trace("onViewCreated called");

        // find the UI components from the layout
        playerStickMan = (ImageView)view.findViewById(R.id.playerStickMan);
        opponentStickMan = (ImageView)view.findViewById(R.id.opponentStickMan);
        playerProgressbar = (ProgressBar)view.findViewById(R.id.gameProgressbar2);
        opponentProgressbar = (ProgressBar)view.findViewById(R.id.gameProgressbar1);
        stickMenLayout = (RelativeLayout)view.findViewById(R.id.gameStickMenLayout);
        goalTextView = (TextView)view.findViewById(R.id.goalTextView);
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

                    // find position controllers
                    // TODO: make this work for >2 players
                    PositionController player = null;
                    PositionController opponent = null;
                    for (PositionController p : gameService.getPositionControllers()) {
                        if (p.isLocalPlayer()) {
                            player = p;
                        } else {
                            opponent = p;
                        }
                    }
                    if (player == null || opponent == null) { log.error("Can't find either player or opponent, cannot update fragment"); return; }

                    // find with of stickMenContainer
                    stickMenLayout.measure(0,0);
                    log.info("Measured width: " + stickMenLayout.getMeasuredWidth() + ", width: " + stickMenLayout.getWidth() + ", minWidth: " + stickMenLayout.getMinimumWidth());
                    fragmentWidth = stickMenLayout.getWidth();

                    // update player progress
                    // TODO: use placementStrategy for stick men
                    float playerProgressPercent = Math.min(1.0f, player.getProgressTowardsGoal(gameService.getGameConfiguration()));
                    playerProgressbar.setProgress((int) (playerProgressPercent * 100));
                    playerStickMan.setTranslationX(playerProgressPercent*fragmentWidth);

                    // update opponent progress
                    // TODO: use placementStrategy for stick men
                    float opponentProgressPercent = opponent.getProgressTowardsGoal(gameService.getGameConfiguration());
                    opponentProgressbar.setProgress((int)(opponentProgressPercent*100));
                    opponentStickMan.setPadding((int)(opponentProgressPercent*fragmentWidth),0,0,0);

                    //log.info("Player progress = " + playerProgressPercent + ", opponent progress = " + opponentProgressPercent + ", fragmentWidth = " + fragmentWidth);

                    // update goal text
                    GameConfiguration strategy = gameService.getGameConfiguration();
                    switch (strategy.getGameType()) {
                        case DISTANCE_CHALLENGE: {
                            goalTextView.setText(Format.zeroDp(strategy.getTargetDistance()) + " " + strategy.getGameType().getTargetUnitMedium());
                            break;
                        }
                        case TIME_CHALLENGE: {
                            goalTextView.setText(Format.zeroDp(UnitConversion.minutes(strategy.getTargetTime())) + " " + strategy.getGameType().getTargetUnitMedium());
                            break;
                        }

                    }

                }
            });
        }
    }



}
