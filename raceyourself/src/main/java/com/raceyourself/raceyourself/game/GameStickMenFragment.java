package com.raceyourself.raceyourself.game;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.raceyourself.raceyourself.R;
import com.raceyourself.platform.utils.UnitConversion;
import com.raceyourself.raceyourself.base.BlankFragment;
import com.raceyourself.raceyourself.game.event_listeners.RegularUpdateListener;
import com.raceyourself.raceyourself.game.placement_strategies.FixedWidthClamped2DPlacementStrategy;
import com.raceyourself.raceyourself.game.placement_strategies.PlacementStrategy;
import com.raceyourself.raceyourself.game.position_controllers.PositionController;

import java.util.ArrayList;
import java.util.List;

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
    private GameService gameService;  // passed in by the activity. Null when not bound (e.g. app is in the background).
    private RegularUpdateListener regularUpdateListener;

    // placement of stick-men
    PlacementStrategy placementStrategy = new FixedWidthClamped2DPlacementStrategy();

    // UI components
    private RelativeLayout stickMenLayout;
    private ImageView playerStickMan;
    private ImageView opponentStickMan;
    private ImageView playerPointer;
    private ImageView buildingImage;
    private ProgressBar playerProgressbar;
    private ProgressBar opponentProgressbar;

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
        playerPointer = (ImageView)view.findViewById(R.id.playerPointer);
        buildingImage = (ImageView)view.findViewById(R.id.gameBuildings);
        playerProgressbar = (ProgressBar)view.findViewById(R.id.gameProgressbar2);
        opponentProgressbar = (ProgressBar)view.findViewById(R.id.gameProgressbar1);
        stickMenLayout = (RelativeLayout)view.findViewById(R.id.gameStickMenLayout);

        // update listener to be called regularly by GameService - this will trigger all out UI updates
        // without the need for a thread/timer in this class
        regularUpdateListener = new RegularUpdateListener() {
            @Override
            public void onRegularUpdate() {
                updateUi();
            }
        }.setRecurrenceInterval(100);
    }

    // set when the service is bound, null when not
    public synchronized void setGameService(GameService gs) {
        // the first time gs is set, add a listener
        if (gs != null && gameService == null) {
            gs.registerRegularUpdateListener(regularUpdateListener);
        }
        this.gameService = gs;
    }

    private void updateUi() {
        if (gameService == null) return;  // cannot access game data till we're bound to the service
        if (getActivity() == null) return;  // activity is probably being destroyed, can't update the screen
        getActivity().runOnUiThread(new Runnable() {
            public void run() {

                synchronized (GameStickMenFragment.this) {

                    if (gameService == null) return;  // cannot access game data till we're bound to the service

                    // find position controllers
                    // TODO: make this work for >2 players
                    PositionController player = null;
                    PositionController opponent = null;
                    // TODO: catch null-pointer that occasionally occurs on the next line
                    for (PositionController p : gameService.getPositionControllers()) {
                        if (p.isLocalPlayer()) {
                            player = p;
                        } else {
                            opponent = p;
                        }
                    }
                    if (player == null || opponent == null) {
                        log.error("Can't find either player or opponent, cannot update fragment");
                        return;
                    }

                    // find width of stickMenContainer
                    stickMenLayout.measure(0, 0);
                    //log.trace("Measured width: " + stickMenLayout.getMeasuredWidth() + ", width: " + stickMenLayout.getWidth() + ", minWidth: " + stickMenLayout.getMinimumWidth());
                    int fragmentWidth = stickMenLayout.getWidth();
                    int trackLength = fragmentWidth - UnitConversion.pixels(48,getActivity());  // take off width of stick-man so he doesn't run off the screen

                    // update progressbars
                    float playerProgressPercent = Math.min(1.0f, player.getProgressTowardsGoal(gameService.getGameConfiguration()));
                    playerProgressbar.setProgress((int) (playerProgressPercent * 100));
                    float opponentProgressPercent = 0.0f;
                    switch(gameService.getGameConfiguration().getGameType()) {
                        case TIME_CHALLENGE: {
                            opponentProgressPercent = (float)(playerProgressPercent*opponent.getRealDistance()/player.getRealDistance());
                            break;
                        }
                        case DISTANCE_CHALLENGE: {
                            //TODO: is this really the same?
                            opponentProgressPercent = (float)(playerProgressPercent*opponent.getRealDistance()/player.getRealDistance());
                            break;
                        }
                    }
                    opponentProgressbar.setProgress((int) (opponentProgressPercent * 100));

                    // update stick-men
                    List<PositionController> stickMenControllers = new ArrayList<PositionController>(2);
                    stickMenControllers.add(player);  // add players in known order, as placementStrategy results are returned in this order
                    stickMenControllers.add(opponent);
                    List<Double> stickMenPositions = placementStrategy.get1dPlacement(stickMenControllers);
                    playerPointer.setPadding((int) (stickMenPositions.get(0) * trackLength) + UnitConversion.pixels(15, getActivity()), 0, 0, 0);
                    playerStickMan.setPadding((int) (stickMenPositions.get(0) * trackLength), 0, 0, 0);
                    opponentStickMan.setPadding((int) (stickMenPositions.get(1) * trackLength), 0, 0, 0);

                    // update rotation of buildings
                    //buildingImage.setRotation(buildingImage.getRotation() + (float)player.getRealDistance());
                    buildingImage.setPadding((int)-player.getRealDistance(), 0, 0, 0);

                }

            }
        });
    }



}
