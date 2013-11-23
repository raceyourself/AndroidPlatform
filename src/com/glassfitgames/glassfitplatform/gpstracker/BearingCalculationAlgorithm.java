package com.glassfitgames.glassfitplatform.gpstracker;

import java.util.ArrayDeque;
import com.glassfitgames.glassfitplatform.models.Position;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import com.javadocmd.simplelatlng.*;
import com.javadocmd.simplelatlng.util.*;
//import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
//import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
   
public class BearingCalculationAlgorithm {

    private ArrayDeque<Position> recentPredictedPositions = new ArrayDeque<Position>();
    private int MAX_PREDICTED_POSITIONS = 5;
    private Position[] interpPath = new Position[MAX_PREDICTED_POSITIONS * CardinalSpline.getNumberPoints()];
    private float invR = (float)0.0000001569612306; // 1/earth's radius (meters)
    private double INV_DELTA_TIME_MS = CardinalSpline.getNumberPoints() / 1000.0; // delta time between predictions
    private float SPEED_THRESHOLD = 0.0f;
    
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
            return null;
        }
        // predict next user position (in 1 sec) based on current speed and bearing
        Position next = Position.predictPosition(aLastPos, 1000);
        // Throw away static positions
        if (next == null || aLastPos.getSpeed() <= SPEED_THRESHOLD) { // standing still
            return null;
        }

        // Correct previous predicted position and speed to head towards next predicted one
        recentPredictedPositions.getLast()
            .setBearing(calcBearing(recentPredictedPositions.getLast(), next));
        recentPredictedPositions.getLast()
            .setSpeed(aLastPos.getSpeed());
        
        // Add predicted position for the next round
        recentPredictedPositions.addLast(next);
        // Keep queue within maximal size limit
        if (recentPredictedPositions.size() > MAX_PREDICTED_POSITIONS) {
           Position rm = recentPredictedPositions.removeFirst(); 
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
        
        return next;
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
    
}
