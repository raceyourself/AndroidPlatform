package com.glassfitgames.glassfitplatform.gpstracker;

import java.util.ArrayDeque;
import com.glassfitgames.glassfitplatform.models.Position;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import com.javadocmd.simplelatlng.*;
//import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
//import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
   
public class BearingCalculationAlgorithm {

    private ArrayDeque<Position> recentPredictedPositions = new ArrayDeque<Position>();
    private int MAX_PREDICTED_POSITIONS = 5;
    private Position[] interpPath = new Position[MAX_PREDICTED_POSITIONS * CardinalSpline.getNumberPoints()];
    private float invR = (float)0.0000001569612306; // 1/earth's radius (meters)
    private int DELTA_TIME_MS = 1000 / CardinalSpline.getNumberPoints(); // delta time between predictions

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
        System.out.printf("interpolatePositionsSpline: aLastPos: %f %f\n",
                          aLastPos.getLatx(), aLastPos.getLngx());
        if (aLastPos == null) {
            return null;
        }
        // Need at least 3 positions
        if (recentPredictedPositions.size() < 2) {
            recentPredictedPositions.push(aLastPos);
            return null;
        }
        // predict next user position (in 1 sec) based on current speed and bearing
        // TODO: throw away static positions
        Position next = predictPosition(aLastPos, 1);
        if (next == null) {
            return null;
        }
        recentPredictedPositions.addLast(next);
        // Keep queue within maximal size limit
        if (recentPredictedPositions.size() > MAX_PREDICTED_POSITIONS) {
           System.out.printf("recentPredictedPositions.size() = %d\n", recentPredictedPositions.size());
           Position rm = recentPredictedPositions.removeFirst(); 
           System.out.printf("removed point %f %f\n", rm.getLatx(), rm.getLngx());
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
        // TODO: avoid conversion to array
        interpPath = CardinalSpline.create(points).toArray(interpPath);
        
        return next;
    }

    public Float predictCurrentBearing(long elapsedTimeMilliseconds) {
        if (interpPath == null || recentPredictedPositions.size() < 3)
        {
            return null;
        }
        // Find closest point (according to device timestamp) in interpolated path
        long firstPredictedPositionTs = recentPredictedPositions.getFirst().getDeviceTimestamp();
        int index = (int) (elapsedTimeMilliseconds - firstPredictedPositionTs) / DELTA_TIME_MS;
        // Predicting forward up to 1 second only
        if (index < 0 || index >= interpPath.length) {
            return null;
        }
        
        return interpPath[index].getBearing();
        
    }

    // Precise position prediction based on the last
    // position, bearing and speed
    // TODO: move to Position.predictPosition(int seconds)
    private Position predictPosition(Position aLastPosition, int aSeconds) {
      System.out.println("\n  predictPosition: Start");  
      System.out.printf("  - %f %f, %f m/s, %f\n", 
                              aLastPosition.getLatx(), aLastPosition.getLngx(), 
                              aLastPosition.getSpeed(), aLastPosition.getBearing());
      if (aLastPosition.getBearing() == null) {
          return null;
      }

       Position next = new Position();
       float d = aLastPosition.getSpeed() * aSeconds; // TODO: units? distance = speed(m/s)* 1s

       float dR = d*invR;
       // Convert bearing to radians
       float brng = (float)Math.toRadians(aLastPosition.getBearing());
       double lat1 = (float)Math.toRadians(aLastPosition.getLatx());
       double lon1 = (float)Math.toRadians(aLastPosition.getLngx());
       System.out.printf("  d: %f, dR: %f; brng: %f\n", d, dR, brng);
       // Predict lat/lon
       double lat2 = (float)Math.asin(Math.sin(lat1)*Math.cos(dR) + 
                    Math.cos(lat1)*Math.sin(dR)*Math.cos(brng) );
       double lon2 = lon1 + (float)Math.atan2(Math.sin(brng)*Math.sin(dR)*Math.cos(lat1), 
                     Math.cos(dR)-Math.sin(lat1)*Math.sin(lat2));
       // Convert back to degrees
       next.setLatx(Math.toDegrees(lat2));
       next.setLngx(Math.toDegrees(lon2));
       // Set predicted timestamps
       int deltaTimeMSec = aSeconds * 1000;
       next.setGpsTimestamp(aLastPosition.getGpsTimestamp() + deltaTimeMSec);
       next.setDeviceTimestamp(aLastPosition.getDeviceTimestamp() + deltaTimeMSec);
       
       System.out.printf("  predictPosition: End - %f %f\n", next.getLatx(), next.getLngx());  
       return next;
    }

    // TODO: move to lower-level Utils class
    public static float calcBearing(Position from, Position to) {
        LatLng fromL = new LatLng(from.getLatx(), from.getLngx());
        LatLng toL = new LatLng(to.getLatx(), to.getLngx());
        return (float)LatLngTool.initialBearing(fromL, toL);
    }

    public static float calcBearingInRadians(Position from, Position to) {
        LatLng fromL = new LatLng(from.getLatx(), from.getLngx());
        LatLng toL = new LatLng(to.getLatx(), to.getLngx());
        return (float)LatLngTool.initialBearingInRadians(fromL, toL);
    }
    
}
