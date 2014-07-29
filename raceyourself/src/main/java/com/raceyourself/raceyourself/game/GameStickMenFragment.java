package com.raceyourself.raceyourself.game;

import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
    private int pointerOffsetPixels = 0;
    private int wrapperHeight = 0;

    private static final double CIRCUMFERENCE_OFFSET = 355.0/1900; // Offset (0..1) from edge of image
    private int circumferenceOffset; // pixels


    // placement of background
    private Point deviceScreenSize;
    private int buildingDrawableWidthOnDevice;
    private int buildingDrawableHeightOnDevice;

    // UI components
    private RelativeLayout stickMenLayout;
    private ImageView playerStickMan;
    private ImageView opponentStickMan;
    private ImageView playerPointer;
    private ImageView buildingImage;

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
        stickMenLayout = (RelativeLayout)view.findViewById(R.id.gameStickMenLayout);

        // dimensions of screen and building image for positioning the building background
        deviceScreenSize = new Point();  //bottom-right of screen (x,y)
        getActivity().getWindowManager().getDefaultDisplay().getSize(deviceScreenSize);
        Drawable buildingDrawable = getResources().getDrawable(R.drawable.circular_background);
        buildingDrawableWidthOnDevice = buildingDrawable.getIntrinsicWidth();
        buildingDrawableHeightOnDevice = buildingDrawable.getIntrinsicHeight();
        circumferenceOffset = (int)(buildingDrawableHeightOnDevice * CIRCUMFERENCE_OFFSET);

        // convert some dp measurements into pixels
        pointerOffsetPixels = UnitConversion.pixels(15, getActivity());
        wrapperHeight = UnitConversion.pixels(270, getActivity());

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

                    // calculate stick-men positions using placement strategy
                    List<PositionController> stickMenControllers = new ArrayList<PositionController>(2);
                    stickMenControllers.add(player);  // add players in known order, as placementStrategy results are returned in this order
                    stickMenControllers.add(opponent);
                    List<Double> stickMenPositions = placementStrategy.get1dPlacement(stickMenControllers);

                    // rotation
                    // segment of building circle shown on screen is roughly 30 degrees.
                    // Player moves from -0.5 to +0.5, or -15 to +15 degrees of rotation
                    float playerRotation = (stickMenPositions.get(0).floatValue() - 0.5f) * 30.0f;
                    float opponentRotation = (stickMenPositions.get(1).floatValue() - 0.5f) * 30.0f;

                    // update stick-men positions and rotations
                    playerStickMan.setX(-playerStickMan.getWidth()/2 + deviceScreenSize.x/2);
                    playerStickMan.setY(-playerStickMan.getHeight() + circumferenceOffset);
                    playerStickMan.setPivotX(playerStickMan.getWidth() / 2);
                    playerStickMan.setPivotY(-playerStickMan.getY() + buildingDrawableHeightOnDevice/2);
                    playerStickMan.setRotation(playerRotation);

                    playerPointer.setX(-playerPointer.getWidth()/2 + deviceScreenSize.x/2);
                    playerPointer.setY(-playerPointer.getHeight() * 2 + playerStickMan.getY());
                    playerPointer.setPivotX(playerPointer.getWidth() / 2);
                    playerPointer.setPivotY(-opponentStickMan.getY() + buildingDrawableHeightOnDevice/2);
                    playerPointer.setRotation(playerRotation);

                    opponentStickMan.setX(-opponentStickMan.getWidth()/2 + deviceScreenSize.x/2);
                    opponentStickMan.setY(-opponentStickMan.getHeight() + circumferenceOffset);
                    opponentStickMan.setPivotX(opponentStickMan.getWidth()/2);
                    opponentStickMan.setPivotY(-opponentStickMan.getY() + buildingDrawableHeightOnDevice / 2);
                    opponentStickMan.setRotation(opponentRotation);

                    // update rotation of buildings
                    float buildingRotationDegrees = (float)-player.getRealDistance();
                    Matrix rotation = new Matrix();
                    rotation.setRotate(buildingRotationDegrees,buildingDrawableWidthOnDevice/2,buildingDrawableHeightOnDevice/2);
                    log.trace("Building rotation is " + buildingRotationDegrees + " degrees");

                    // translate buildings to show just middle/top
                    Matrix translation = new Matrix();
                    translation.setTranslate(-(buildingDrawableWidthOnDevice-deviceScreenSize.x)/2,0);

                    // apply the transforms:
                    translation.preConcat(rotation);
                    buildingImage.setImageMatrix(translation);

                    //buildingImage.setPadding((int)-player.getRealDistance(), 0, 0, 0);


                }

            }
        });
    }



}
