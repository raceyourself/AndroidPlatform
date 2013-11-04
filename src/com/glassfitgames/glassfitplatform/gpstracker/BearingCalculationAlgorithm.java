package com.glassfitgames.glassfitplatform.gpstracker;

import java.util.ArrayDeque;
import com.glassfitgames.glassfitplatform.models.Position;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import com.glassfitgames.glassfitplatform.utils.CardinalSpline;
//import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
//import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
   
public class BearingCalculationAlgorithm {

    private ArrayDeque<Position> recentPredictedPositions = new ArrayDeque<Position>();
    private ArrayDeque<Position> interpPath;
    private int MAX_PREDICTED_POSITIONS = 5;
    private float invR = (float)0.0000001569612306; // 1/earth's radius (meters)

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
        System.out.printf("interpolatePositionsSpline %f %f\n",
                          aLastPos.getLatx(), aLastPos.getLngx());
        // Need at least 3 positions
        if (recentPredictedPositions.size() < 2) {
            recentPredictedPositions.push(aLastPos);
            return null;
        }
        // predict next user position (in 1 sec) based on current speed and bearing
        // TODO: throw away static positions
        Position next = predictPosition(aLastPos, 1);
        recentPredictedPositions.push(next);
        if (recentPredictedPositions.size() > MAX_PREDICTED_POSITIONS) {
           recentPredictedPositions.pop(); 
        }
        // Fill input for interpolation
        Position[] points = new Position[recentPredictedPositions.size()];
        
        int i = 0;
        for (Position p : recentPredictedPositions) {
            points[i] = p;
            System.out.printf("points[%d] %f %f\n", i, p.getLatx(), p.getLngx());
            ++i;
        }
 
        // interpolate using cardinal spline
        interpPath = CardinalSpline.create(points);
        
        return next;
    }

    public Float predictCurrentBearing(long elapsedTimeMilliseconds) {
        if (recentPredictedPositions.size() < 2)
        {
            return null;
        }
        /* TODO
         *         float[] bearing = {
            (float)recentPositions.getLast().bearingTo(next)  % 360,  // % 360 converts negative angles to bearings
            (float)100.0,
            (float)100.0
        };
        */
        return null;
        
    }

    // Precise position prediction based on the last
    // position, bearing and speed
    // TODO: move to Position.predictPosition(int seconds)
    private Position predictPosition(Position aLastPosition, int aSeconds) {
      System.out.println("predictPosition: Start\n");  
      System.out.printf("- %f %f, %f m/s, %f\n", 
                              aLastPosition.getLatx(), aLastPosition.getLngx(), 
                              aLastPosition.getSpeed(), aLastPosition.getBearing());

       Position next = new Position();
       float d = aLastPosition.getSpeed() * aSeconds; // TODO: units? distance = speed(m/s)* 1s

       float dR = d*invR;
       // Convert bearing to radians
       float brng = (float)Math.toRadians(aLastPosition.getBearing());
       float lat1 = (float)Math.toRadians(aLastPosition.getLatx());
       float lon1 = (float)Math.toRadians(aLastPosition.getLngx());
       System.out.printf("d: %f, dR: %f; brng: %f\n", d, dR, brng);
       // Predict lat/lon
       float lat2 = (float)Math.asin(Math.sin(lat1)*Math.cos(dR) + 
                    Math.cos(lat1)*Math.sin(dR)*Math.cos(brng) );
       float lon2 = lon1 + (float)Math.atan2(Math.sin(brng)*Math.sin(dR)*Math.cos(lat1), 
                     Math.cos(dR)-Math.sin(lat1)*Math.sin(lat2));
       // Convert back to degrees
       next.setLatx((float)Math.toDegrees(lat2));
       next.setLngx((float)Math.toDegrees(lon2));
       next.setGpsTimestamp(aLastPosition.getGpsTimestamp() + aSeconds * 1000);
       
       System.out.printf("predictPosition: End - %f %f\n", next.getLatx(), next.getLngx());  
       return next;
    }

}
