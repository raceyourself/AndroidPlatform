package com.raceyourself.raceyourself.game.placement_strategies;

import com.raceyourself.raceyourself.game.position_controllers.PositionController;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by benlister on 03/07/2014.
 */
@Slf4j
public class FixedWidthClamped2DPlacementStrategy implements PlacementStrategy {

    private double distanceShown = 200;

    public List<Double> get1dPlacement(List<PositionController> positionControllers) {

        // work out min/max real-world distances
        double realMinDist = Float.MAX_VALUE;
        double realMaxDist = Float.MIN_VALUE;
        for (PositionController p : positionControllers) {
            double dist = p.getRealDistance();
            if (dist < realMinDist)
                realMinDist = dist;
            if (dist > realMaxDist)
                realMaxDist = dist;
        }

        // work out left/right of screen in real-world numbers
        double realDistMidpoint = (realMinDist + realMaxDist)/2;
        double displayedMinDist = realDistMidpoint - distanceShown/2; // TODO should be at least 0
        double displayedMaxDist = realDistMidpoint + distanceShown/2; // TODO if min=0, max=distanceShown
        if (displayedMinDist < 0) {
            displayedMinDist = 0;
            displayedMaxDist = distanceShown;
        }

        // work out on-screen position on a scale from 0-1
        List<Double> relativePositions = new ArrayList<Double>();
        for (PositionController p : positionControllers) {
            double dist = p.getRealDistance();
            if (dist < displayedMinDist)
                dist = displayedMinDist;
            else if (dist > displayedMaxDist)
                dist = displayedMaxDist;

            relativePositions.add((dist-displayedMinDist)/distanceShown);
        }

        return relativePositions;
    }

}
