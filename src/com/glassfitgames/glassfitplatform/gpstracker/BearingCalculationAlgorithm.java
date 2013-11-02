package com.glassfitgames.glassfitplatform.gpstracker;

import java.util.ArrayDeque;
import com.glassfitgames.glassfitplatform.models.Position;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
   
class BearingCalculationAlgorithm {

    private ArrayDeque<Position> recentPositions;

    public BearingCalculationAlgorithm(ArrayDeque<Position> aPositions) {
        recentPositions = aPositions;
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
    
    private Position predictPosition(Position aLastPosition, int aSeconds) {
       Position next = new Position();
       float d = aLastPosition.getSpeed() * seconds; // TODO: units? distance = speed(m/s)* 1s
       float R = 6371000.0; // earth's radius
       // TODO: move to Position.predictPosition(int seconds)
       int brng = aLastPosition.getBearing();
       next.setLatx(Math.asin( Math.sin(aLastPosition.getLatx())*Math.cos(d/R) + 
                    Math.cos(aLastPosition.getLatx())*Math.sin(d/R)*Math.cos(brng) );
       next.setLony = aLastPosition.getLony() + Math.atan2(Math.sin(brng)*Math.sin(d/R)*Math.cos(aLastPosition.getLatx()), 
                     Math.cos(d/R)-Math.sin(aLastPosition.getLatx())*Math.sin(next.getLatx()));
 
    }

    public float[] calculateCurrentBearingSpline(long elapsedTimeMilliseconds) {
        SplineInterpolator splIn = new SplineInterpolator();


        int size = recentPositions.size() + 1;
        double[] x = new double[size];
        double[] y = new double[size];
        
        int i = 0;
        for (Position p : recentPositions) {
            x[i] = (double)p.getLatx());
            y[i] = (double)p.getLongx();
            ++i;
        }
        PolynomialSplineFunction splFun = splIn.interpolate(x, y);

        // use course to predict next position of user, and hence current bearing
        Position next = new Position();
        
        // extrapolate latitude in same direction as last few points
        //next.setLatx(2*recentPositions.getLast().getLatx() - recentPositions.getFirst().getLatx());
        next.setLongx((float)splFun.value(next.getLatx());

        float[] bearing = {
            (float)recentPositions.getLast().bearingTo(next)  % 360,  // % 360 converts negative angles to bearings
            (float)100.0,
            (float)100.0
        };
        return bearing;
    }


}
