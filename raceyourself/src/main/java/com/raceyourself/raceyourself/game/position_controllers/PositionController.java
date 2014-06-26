package com.raceyourself.raceyourself.game.position_controllers;

import com.raceyourself.platform.models.Position;
import com.raceyourself.raceyourself.game.placement_strategies.PlacementStrategy;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by benlister on 26/06/2014.
 */
public abstract class PositionController {

    @Getter
    @Setter
    private PlacementStrategy placementStrategy;

    public abstract void start();
    public abstract void stop();
    public abstract void reset();

    public abstract double getRealDistance();

    public float getOnScreenDistance() {
        return placementStrategy.get1DPlacement(getRealDistance());
    }

}
