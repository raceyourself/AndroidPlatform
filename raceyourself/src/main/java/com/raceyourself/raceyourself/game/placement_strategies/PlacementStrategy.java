package com.raceyourself.raceyourself.game.placement_strategies;

import com.raceyourself.raceyourself.game.position_controllers.PositionController;

import java.util.List;

/**
 * Created by benlister on 26/06/2014.
 */
public interface PlacementStrategy {

    public List<Double> get1dPlacement(List<PositionController> positionControllers);

}
