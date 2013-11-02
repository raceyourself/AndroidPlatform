package com.glassfitgames.glassfitplatform.gpstracker;

import java.util.ArrayDeque;
import com.glassfitgames.glassfitplatform.models.Position;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
   
class BearingCalculationAlgorithm {

    private ArrayDeque<Position> recentPositions;
    private ArrayDeque<Position> recentPredictedPositions;
    private int MAX_PREDICTED_POSITIONS = 5;
    private float invR = (float)0.0000001569612306; // 1/earth's radius (meters)

    public BearingCalculationAlgorithm(ArrayDeque<Position> aPositions) {
        recentPositions = aPositions;
        recentPredictedPositions = new ArrayDeque<Position>(aPositions);
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
        // calculate user's course by drawing a least-squares best-fit line through the last 10 positions
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

    }

    public float[] calculateCurrentBearingSpline() {
        // TODO: override last (predicted) position with recent real one
        //recentPredictedPositions.getLast() = recentPositions.getLast();
        // predict next user position (in 1 sec) based on current speed and bearing
        // TODO: throw away static positions
        Position next = predictPosition(recentPositions.getLast(), 1);
        recentPredictedPositions.push(next);
        if (recentPredictedPositions.size() > MAX_PREDICTED_POSITIONS) {
           recentPredictedPositions.pop(); 
        }
        // Need at least two positions
        if (recentPredictedPositions.size() < 2) {
            return null;
        }
        // Fill input for interpolation
        int size = recentPredictedPositions.size();
        double[] x = new double[size];
        double[] y = new double[size];
        
        int i = 0;
        for (Position p : recentPredictedPositions) {
            x[i] = (double)p.getLatx();
            y[i] = (double)p.getLngx();
            ++i;
        }
 
        // interpolate using cubic spline
        SplineInterpolator splIn = new SplineInterpolator();
        PolynomialSplineFunction splFun = splIn.interpolate(x, y);
        
        // Correct next predicted (in 1s) position according to interpolation
        next = recentPredictedPositions.getLast();
        next.setLngx(splFun.value(next.getLatx()));
        

        float[] bearing = {
            (float)recentPositions.getLast().bearingTo(next)  % 360,  // % 360 converts negative angles to bearings
            (float)100.0,
            (float)100.0
        };
        return bearing;
    }

    public Float predictCurrentBearing(long elapsedTimeMilliseconds) {
        if (recentPredictedPositions.size() < 2)
        {
            return null;
        }
        // TODO
        return null;
        
    }

    // Precise position prediction based on the last
    // position, bearing and speed
    // TODO: move to Position.predictPosition(int seconds)
    private Position predictPosition(Position aLastPosition, int aSeconds) {
       Position next = new Position();
       float d = aLastPosition.getSpeed() * aSeconds; // TODO: units? distance = speed(m/s)* 1s

       float dR = d*invR;
       // Convert bearing to radians
       float brng = (float)Math.toRadians(aLastPosition.getBearing());
       
       // Predict lat/lon
       next.setLatx(Math.asin( Math.sin(aLastPosition.getLatx())*Math.cos(dR) + 
                    Math.cos(aLastPosition.getLatx())*Math.sin(dR)*Math.cos(brng) ));
       next.setLngx(aLastPosition.getLngx() + Math.atan2(Math.sin(brng)*Math.sin(dR)*Math.cos(aLastPosition.getLatx()), 
                     Math.cos(dR)-Math.sin(aLastPosition.getLatx())*Math.sin(next.getLatx())));
       next.setGpsTimestamp(aLastPosition.getGpsTimestamp() + aSeconds * 1000);
       return next;
    }

}
