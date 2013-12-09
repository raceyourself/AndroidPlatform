package com.glassfitgames.glassfitplatform.gpstracker;

import java.util.ArrayDeque;
import java.util.Iterator;

import com.glassfitgames.glassfitplatform.models.Bearing;
import com.glassfitgames.glassfitplatform.models.Position;

import com.javadocmd.simplelatlng.*;
import com.javadocmd.simplelatlng.util.*;

// The class keeps track of few last (predicted) position and may predict the future position and its bearing :)
// updatePosition() method should be used to update predictor state with the last GPS position.
// After that, predictPosition() and/or predictBearing() can be used to get up-to-date prediction
public class PositionPredictor {
    // Constant used to optimize calculations
    private double INV_DELTA_TIME_MS = CardinalSpline.getNumberPoints() / 1000.0; // delta time between predictions
    // Positions below this speed threshold will be discarded in bearing computation
    private float SPEED_THRESHOLD_MS = 1.0f;
    // Maximal number of predicted positions used for spline interpolation
    private int MAX_PREDICTED_POSITIONS = 3;

    // Last predicted positions
    private ArrayDeque<Position> recentPredictedPositions = new ArrayDeque<Position>();
    // Interpolated positions between recent predicted position
    private Position[] interpPath = new Position[MAX_PREDICTED_POSITIONS * CardinalSpline.getNumberPoints()];
    // Last GPS position
    private Position lastGpsPosition = new Position();
    // Accumulated GPS distance
    private double gpsTraveledDistance = 0;
    // Accumulated predicted distance
    private double predictedTraveledDistance = 0;
    int i = 0;
    
    public PositionPredictor() {
    }

    // Update prediction with new GPS position. 
    // Input: recent GPS positon, output: correspondent predicted position 
    public Position updatePosition(Position aLastGpsPos) {
    	//System.out.printf("\n------ %d ------\n", ++i);
        if (aLastGpsPos == null) {
            return null;
        }
        // Need at least 3 positions
        if (recentPredictedPositions.size() < 2) {
            recentPredictedPositions.addLast(aLastGpsPos);
            lastGpsPosition = aLastGpsPos;
            return aLastGpsPos;
        } else if (recentPredictedPositions.size() == 2) {
            recentPredictedPositions.addLast(extrapolatePosition(recentPredictedPositions.getLast(), 1));
        }
        // Update traveled distance
        updateDistance(aLastGpsPos);
        
        // correct last (predicted) position with last GPS position
        correctLastPredictedPosition(aLastGpsPos);
        
        // predict next user position (in 1 sec) based on current speed and bearing
        Position nextPos = extrapolatePosition(recentPredictedPositions.getLast(), 1);

        // Throw away static positions
        if (nextPos == null || aLastGpsPos.getSpeed() <= SPEED_THRESHOLD_MS) { // standing still
            return null;
        }
        
        // Add predicted position for the next round
        recentPredictedPositions.addLast(nextPos);
        // Keep queue within maximal size limit
        if (recentPredictedPositions.size() > MAX_PREDICTED_POSITIONS) {
           recentPredictedPositions.removeFirst(); 
        }
        // Fill input for interpolation
        Position[] points = new Position[recentPredictedPositions.size()];
        
        int i = 0;
        for (Position p : recentPredictedPositions) {
            points[i++] = p;
            //System.out.printf("CTL POINTS: points[%d], %.15f,,%.15f, bearing: %f\n",	i, p.getLngx(), p.getLatx(), p.getBearing());
        }        
        // interpolate using cardinal spline
        // TODO: avoid conversion to array
        interpPath = CardinalSpline.create(points).toArray(interpPath);
        lastGpsPosition = aLastGpsPos;
        return recentPredictedPositions.getLast();
    }
    
    // Returns predicted position at a given timestamp
    public Position predictPosition(long aDeviceTimestampMilliseconds) {
        if (interpPath == null || recentPredictedPositions.size() < 3)
        {
            return null;
        }
        // Find closest point (according to device timestamp) in interpolated path
        long firstPredictedPositionTs = recentPredictedPositions.getFirst().getDeviceTimestamp();
        int index = (int) ((aDeviceTimestampMilliseconds - firstPredictedPositionTs) * INV_DELTA_TIME_MS);
        // Predicting only within current path
        //System.out.printf("BearingAlgo::predictPosition: ts: %d, index: %d, path length: %d\n", aDeviceTimestampMilliseconds
        //                   ,index, interpPath.length);   
        if (index < 0 || index >= interpPath.length) {
            return null;
        }
        
        return interpPath[index];
    }

    // Returns bearing of the predicted position at given time
    public Float predictBearing(long aDeviceTimestampMilliseconds) {
        Position pos = predictPosition(aDeviceTimestampMilliseconds);
        if (pos == null)
        {
            return null;
        }
        return pos.getBearing();
        
    }
    
    // Extrapolate (predict) position based on last positions given time ahead
    private Position extrapolatePosition(Position aLastPos, long timeSec) {
    	// Simple method - calculate based on speed and bearing of last position
    	return Position.predictPosition(aLastPos, timeSec*1000);
    	
/*    	// More sophisticated solution - calculate average acceleration and angle
    	// speed
    	float acc = 0; //calcAcceleration(aLastPos);
    	float angleSpeed = 0; //calcAngleSpeed(aLastPos);
    	// Calculate distance
    	float d = aLastPos.getSpeed()*timeSec + 0.5f*(acc*timeSec*timeSec);
    	d = ( d < 0.0f ) ? 0.0f : d;
    	
    	Position correctedPos = new Position();
    	correctedPos.setLngx(aLastPos.getLngx());
    	correctedPos.setLatx(aLastPos.getLatx());
    	correctedPos.setGpsTimestamp(aLastPos.getGpsTimestamp());
    	correctedPos.setDeviceTimestamp(aLastPos.getDeviceTimestamp());
    	correctedPos.setSpeed(d/timeSec);
    	if (aLastPos.getBearing() != null) {
    		correctedPos.setBearing(aLastPos.getBearing() + 0.5f*angleSpeed);
    	}
    	System.out.printf("ORIGINAL SPEED: %f, NEW SPEED: %f\n" , aLastPos.getSpeed(),correctedPos.getSpeed());
    	// Return "simple" corrected prediction
    	return Position.predictPosition(correctedPos, timeSec*1000);
    	//return Position.predictPosition(aLastPos, timeSec*1000);
*/    	
    }
    
    // Update calculations for predicted and real traveled distances
    // TODO: unify distance calculations with GpsTracker distance calculations
    private void updateDistance(Position aLastPos) {
        Iterator<Position> reverseIterator = recentPredictedPositions.descendingIterator();
        reverseIterator.next();
        Position prevPredictedPos = reverseIterator.next();        
        double distancePredicted = calcDistance(prevPredictedPos, recentPredictedPositions.getLast());
        predictedTraveledDistance += distancePredicted;	
        
        double distanceReal = calcDistance(lastGpsPosition, aLastPos);
        gpsTraveledDistance += distanceReal;
    }
    
    private void correctLastPredictedPosition(Position aLastGpsPos) {
        // correct last (predicted) position with last GPS position
        Position lastPredictedPos = recentPredictedPositions.getLast();
        lastPredictedPos.setBearing(calcCorrectedBearing(aLastGpsPos));
        lastPredictedPos.setDeviceTimestamp(aLastGpsPos.getDeviceTimestamp());
        lastPredictedPos.setGpsTimestamp(aLastGpsPos.getGpsTimestamp());        
        lastPredictedPos.setSpeed(calcCorrectedSpeed(aLastGpsPos));
    }
    
    private float calcCorrectedSpeed(Position aLastPos) {
    	double offset = (gpsTraveledDistance - predictedTraveledDistance);
    	/*System.out.printf("GPS DIST: %f, EST DIST: %f, OFFSET: %f\n" , 
    			gpsTraveledDistance,predictedTraveledDistance, offset);
		*/
        double coeff = (offset > 0 ) ? 0.5 : -0.5;        
        coeff = Math.abs(offset) <= aLastPos.getSpeed() ? offset/aLastPos.getSpeed() : coeff;

        double correctedSpeed = aLastPos.getSpeed()*(1 + coeff);
        
        // System.out.printf("SPEED: %f, CORRECTED SPEED: %f, DISTANCE COEFF: %f\n",aLastPos.getSpeed(), correctedSpeed, coeff);
        return (float) correctedSpeed;
    	
    }
   
    // Correct bearing to adapt slowly to the GPS curve
    private float calcCorrectedBearing(Position aLastPos) {
    	Position lastPredictedPos = recentPredictedPositions.getLast();
    	Position nextPredictedGpsPos = Position.predictPosition(aLastPos, 5000);
    	float bearingToNextGpsPos = Bearing.calcBearing(lastPredictedPos, nextPredictedGpsPos);
    	float bearingDiff = bearingToNextGpsPos - aLastPos.getBearing();
    	
    	return Bearing.normalizeBearing(aLastPos.getBearing() + 0.3f*bearingDiff);
    	
    }
    
    // TODO: move to Position class
    public static float calcDistance(Position from, Position to) {
        LatLng fromL = new LatLng(from.getLatx(), from.getLngx());
        LatLng toL = new LatLng(to.getLatx(), to.getLngx());
        return (float)LatLngTool.distance(fromL, toL, LengthUnit.METER );
    }
    
    // TODO: use calculated acceleration in speed prediction
    private float calcAcceleration(Position aLastPos) {
    	Position prevPos = null;
    	float acc = 0;
    	for (Position pos: recentPredictedPositions) {
    		if (prevPos != null) {
    			acc += pos.getSpeed() - prevPos.getSpeed();
    		}
    		prevPos = pos;
    	}
    	// and take last position 
		acc = 0.8f*(aLastPos.getSpeed() - prevPos.getSpeed()) + 0.2f*acc;
		// return average acceleration
		return acc/recentPredictedPositions.size();
    }
    
    // TODO: use calculated angle speed in bearing prediction
    private float calcAngleSpeed(Position aLastPos) {
    	Position prevPos = null;
    	float angleSpeed = 0;
    	for (Position pos: recentPredictedPositions) {
    		if (pos.getBearing() == null) {
    			return 0.0f;
    		}
    		if (prevPos != null) {
    			// TODO: make sure it's calculated correctly for 0-360 degrees
    			angleSpeed += pos.getBearing() - prevPos.getBearing();
    		}
    		prevPos = pos;
    	}
    	// and take last position
    	angleSpeed = 0.8f*(aLastPos.getBearing() - prevPos.getBearing()) + 0.2f*angleSpeed;
		// return average acceleration
		return angleSpeed/recentPredictedPositions.size();
    }

}
