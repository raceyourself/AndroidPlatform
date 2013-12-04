package com.glassfitgames.glassfitplatform.gpstracker;

import java.util.ArrayDeque;
import java.util.Iterator;

import com.glassfitgames.glassfitplatform.models.Position;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import com.javadocmd.simplelatlng.*;
import com.javadocmd.simplelatlng.util.*;
//import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
//import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
   
public class BearingCalculationAlgorithm {

    private ArrayDeque<Position> recentPredictedPositions = new ArrayDeque<Position>();
    private int MAX_PREDICTED_POSITIONS = 3;
    private Position[] interpPath = new Position[MAX_PREDICTED_POSITIONS * CardinalSpline.getNumberPoints()];
    private double INV_DELTA_TIME_MS = CardinalSpline.getNumberPoints() / 1000.0; // delta time between predictions
    private float SPEED_THRESHOLD = 0.0f;
    private Position lastGpsPosition = null;
    
    private double gpsTraveledDistance = 0;
    private double predictedTraveledDistance = 0;
    
    
    public BearingCalculationAlgorithm() {

    }
/**
     * calculateCurrentBearing uses a best-fit line through the Positions in recentPositions to
     * determine the bearing the user is moving on. We know the raw GPS bearings jump around quite a
     * bit, causing the avatars to jump side to side, and this is an attempt to correct that. There
     * may be some inaccuracies when the bearing is close to due north or due south, as the
     * gradient numbers get close to infinity. We should consider using e.g. polar co-ordinates to
     * correct for this.
     * 
     * @return [corrected bearing, R^2, significance] or null if we're not obviously moving in a direction 
*/
    public float[] calculateCurrentBearing() { 
/*        // calculate user's course by drawing a least-squares best-fit line through the last 10 positions
        SimpleRegression linreg = new SimpleRegression();
        for (Position p : recentPositions) {
            linreg.addData(p.getLatx(), p.getLngx());
        }
        
        // if there's a significant chance we don't have a good fit, don't calc a bearing
        if (linreg.getSignificance() > 0.05) return null;
        
        // use course to predict next position of user, and hence current bearing
        Position next = new Position();
        // extrapolate latitude in same direction as last few points
        next.setLatx(2*recentPositions.getLast().getLatx() - recentPositions.getFirst().getLatx());
        // use regression model to predict longitude for the new point
        next.setLngx(linreg.predict(next.getLatx()));
        // return bearing to new point and some stats
        float[] bearing = {
            recentPositions.getLast().bearingTo(next) % 360,  // % 360 converts negative angles to bearings
            (float)linreg.getR(),
            (float)linreg.getSignificance()
        };
        return bearing;
*/
        return null;
    }
    // Extrapolates positions, return next predicted position 
    public Position interpolatePositionsSpline(Position aLastPos) {
        if (aLastPos == null) {
            return null;
        }
        // Need at least 3 positions
        if (recentPredictedPositions.size() < 2) {
            recentPredictedPositions.push(aLastPos);
            lastGpsPosition = aLastPos;
            return null;
        }
        // correct last (predicted) position with last GPS position
        recentPredictedPositions.getLast().setBearing(aLastPos.getBearing());
        recentPredictedPositions.getLast().setDeviceTimestamp(aLastPos.getDeviceTimestamp());
        recentPredictedPositions.getLast().setGpsTimestamp(aLastPos.getGpsTimestamp());
        
        recentPredictedPositions.getLast().setSpeed(calcCorrectedSpeed(aLastPos));
        // predict next user position (in 1 sec) based on current speed and bearing
        Position next = extrapolatePosition(/*aLastPos*/recentPredictedPositions.getLast(), 1);
        // Throw away static positions
        if (next == null || aLastPos.getSpeed() <= SPEED_THRESHOLD) { // standing still
            return null;
        }

        // Correct previous predicted position and speed to head towards next predicted one
        /* recentPredictedPositions.getLast()
            .setBearing(calcBearing(recentPredictedPositions.getLast(), next));
        recentPredictedPositions.getLast()
            .setSpeed(aLastPos.getSpeed());*/

        
        // Add predicted position for the next round
        recentPredictedPositions.addLast(next);
        // Keep queue within maximal size limit
        if (recentPredictedPositions.size() > MAX_PREDICTED_POSITIONS) {
           recentPredictedPositions.removeFirst(); 
        }
        // Fill input for interpolation
        Position[] points = new Position[recentPredictedPositions.size()];
        
        int i = 0;
        for (Position p : recentPredictedPositions) {
            points[i] = p;
            System.out.printf("CTL POINTS: points[%d], %.15f,,%.15f, bearing: %f\n", 
                              i, p.getLngx(), p.getLatx(), p.getBearing());
            ++i;

        }
        System.out.printf("---\n");
        
        // interpolate using cardinal spline
        // TODO: avoid conversion to array
        interpPath = CardinalSpline.create(points).toArray(interpPath);
        lastGpsPosition = aLastPos;
        return next;
    }
    // Extrapolate (predict) position based on last positions given time ahead
    private Position extrapolatePosition(Position aLastPos, long timeSec) {
    	// Simple method - calculate based on speed and bearing of last position
    	//return Position.predictPosition(aLastPos, timeSec*1000);
    	
    	// More sophisticated solution - calculate average acceleration and angle
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
    	
    }
    
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
    
    float calcCorrectedSpeed(Position aLastPos) {
        Iterator<Position> reverseIterator = recentPredictedPositions.descendingIterator();
        reverseIterator.next();
        Position prevPredictedPos = reverseIterator.next();        
        double distancePredicted = calcDistance(prevPredictedPos, recentPredictedPositions.getLast());
        predictedTraveledDistance += distancePredicted;	
        
        double distanceReal = calcDistance(lastGpsPosition, aLastPos);
        gpsTraveledDistance += distanceReal;
        
        
        double offset = (gpsTraveledDistance - predictedTraveledDistance);
    	System.out.printf("GPS DIST: %f, EST DIST: %f, OFFSET: %f\n" , 
    			gpsTraveledDistance,predictedTraveledDistance, offset);

        double coeff = (offset > 0 ) ? 0.5 : -0.5;        
        coeff = Math.abs(offset) < 0.3*aLastPos.getSpeed() ? offset/aLastPos.getSpeed() : coeff;

        double correctedSpeed = aLastPos.getSpeed()*(1 + coeff);
        
        System.out.printf("DISTANCE COEFF: %f\n", coeff);
        return (float) correctedSpeed;
    	
    }
    
    // TODO: move the function to PositionPredictor class
    public Position predictPosition(long aDeviceTimestampMilliseconds) {
        if (interpPath == null || recentPredictedPositions.size() < 3)
        {
            return null;
        }
        // Find closest point (according to device timestamp) in interpolated path
        long firstPredictedPositionTs = recentPredictedPositions.getFirst().getDeviceTimestamp();
        int index = (int) ((aDeviceTimestampMilliseconds - firstPredictedPositionTs) * INV_DELTA_TIME_MS);
        // Predicting only within current path
        System.out.printf("BearingAlgo::predictPosition: ts: %d, index: %d, path length: %d\n", aDeviceTimestampMilliseconds
                           ,index, interpPath.length);   
        if (index < 0 || index >= interpPath.length) {
            return null;
        }
        
        return interpPath[index];
        
    }

    
    public Float predictCurrentBearing(long aDeviceTimestampMilliseconds) {
        Position pos = predictPosition(aDeviceTimestampMilliseconds);
        if (pos == null)
        {
            return null;
        }
        return pos.getBearing();
        
    }

    // TODO: move to lower-level Utils class
    public static float calcBearing(Position from, Position to) {
        LatLng fromL = new LatLng(from.getLatx(), from.getLngx());
        LatLng toL = new LatLng(to.getLatx(), to.getLngx());
        return (float)LatLngTool.initialBearing(fromL, toL);
    }

    public static float calcBearingInRadians(Position from, Position to) {
        double lat1R = Math.toRadians(from.getLatx());
        double lat2R = Math.toRadians(to.getLatx());
        double dLngR = Math.toRadians(to.getLngx() - from.getLngx());
        double a = Math.sin(dLngR) * Math.cos(lat2R);
        double b = Math.cos(lat1R) * Math.sin(lat2R) - Math.sin(lat1R) * Math.cos(lat2R)
                                * Math.cos(dLngR);
        return (float)Math.atan2(a, b);
     }

    public static float calcDistance(Position from, Position to) {
        LatLng fromL = new LatLng(from.getLatx(), from.getLngx());
        LatLng toL = new LatLng(to.getLatx(), to.getLngx());
        return (float)LatLngTool.distance(fromL, toL, LengthUnit.METER );
    }
    
}
