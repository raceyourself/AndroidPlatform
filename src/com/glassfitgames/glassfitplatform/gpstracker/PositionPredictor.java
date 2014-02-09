package com.glassfitgames.glassfitplatform.gpstracker;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayDeque;
import java.util.Iterator;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import android.os.Environment;

import com.glassfitgames.glassfitplatform.gpstracker.kml.GFKml;
import com.glassfitgames.glassfitplatform.models.Bearing;
import com.glassfitgames.glassfitplatform.models.Position;

import com.javadocmd.simplelatlng.*;
import com.javadocmd.simplelatlng.util.*;

// The class keeps track of few last (predicted) position and may predict the future position and its bearing :)
// updatePosition() method should be used to update predictor state with the last GPS position.
// After that, predictPosition() and/or predictBearing() can be used to get up-to-date prediction
public class PositionPredictor {
	private boolean LOG_KML = false;
	private GFKml kml = new GFKml();
    // Constant used to optimize calculations
    private double INV_DELTA_TIME_MS = CardinalSpline.getNumberPoints() / 1000.0; // delta time between predictions
    // Positions below this speed threshold will be discarded in bearing computation
    private float SPEED_THRESHOLD_MS = 1.25f;
    // Maximal number of predicted positions used for spline interpolation
    private int MAX_PREDICTED_POSITIONS = 3;
    private int MAX_EXT_PREDICTED_POSITIONS = 5;
    
    LinearRegressionBearing linearRegressionBearing = new LinearRegressionBearing();

    // Last predicted positions - used for spline
    private ArrayDeque<Position> recentPredictedPositions = new ArrayDeque<Position>();
    // Extended queue for predicted positions - used for linear regression 
    private ArrayDeque<Position> recentExtPredictedPositions = new ArrayDeque<Position>();

    private ArrayDeque<Position> recentGpsPositions = new ArrayDeque<Position>();
    // Interpolated positions between recent predicted position
    private Position[] interpPath = new Position[MAX_PREDICTED_POSITIONS * CardinalSpline.getNumberPoints()];
    // Last GPS position
    private Position lastGpsPosition = new Position();
    private Position lastPredictedPos = new Position();
    // Accumulated GPS distance
    private double gpsTraveledDistance = 0;
    // Accumulated predicted distance
    private double predictedTraveledDistance = 0;
    
    private int numStaticPos = 0;
    
    private int MAX_NUM_STATIC_POS = 2;
        
    public PositionPredictor() {
    }

    // Update prediction with new GPS position. 
    // Input: recent GPS positon, output: correspondent predicted position 
    public Position updatePosition(Position aLastGpsPos) {
    	//System.out.printf("\n------ %d ------\n", ++i);
        if (aLastGpsPos == null || aLastGpsPos.getBearing() == null) {
            return null;
        }
        if(LOG_KML) kml.addPosition(GFKml.PathType.GPS, aLastGpsPos);
        // Need at least 3 positions
        if (recentPredictedPositions.size() < 2) {
            recentPredictedPositions.addLast(aLastGpsPos);
            recentExtPredictedPositions.addLast(recentPredictedPositions.getLast());
            lastGpsPosition = aLastGpsPos;
            return aLastGpsPos;
        } else if (recentPredictedPositions.size() == 2) {
            recentPredictedPositions.addLast(extrapolatePosition(recentPredictedPositions.getLast(), 1));
            recentExtPredictedPositions.addLast(recentPredictedPositions.getLast());
        }
        // Update traveled distance
        updateDistance(aLastGpsPos);
        
        // correct last (predicted) position with last GPS position
        correctLastPredictedPosition(aLastGpsPos);
        
        // predict next user position (in 1 sec) based on current speed and bearing
        Position nextPos = extrapolatePosition(recentPredictedPositions.getLast(), 1);
        if(LOG_KML) kml.addPosition(GFKml.PathType.EXTRAPOLATED, nextPos);

        // Update number static positions
        numStaticPos = (aLastGpsPos.getSpeed() < SPEED_THRESHOLD_MS) ? numStaticPos+1 : 0;
        // Throw away static positions and flush predicted path/traveled distance
        // TODO: may be wait for 2-3 invalid updates before flushing?
        if (nextPos == null || numStaticPos > MAX_NUM_STATIC_POS 
        		|| aLastGpsPos.getSpeed() == 0.0) { // standing still
        	recentPredictedPositions.clear();
        	recentExtPredictedPositions.clear();
        	recentGpsPositions.clear();
        	predictedTraveledDistance = gpsTraveledDistance;
        	numStaticPos = 0;
            return null;
        }
        recentGpsPositions.add(aLastGpsPos);
        if (recentGpsPositions.size() > MAX_EXT_PREDICTED_POSITIONS) {
        	recentGpsPositions.removeFirst();
        }
        
        
        // Add predicted position for the next round
        recentPredictedPositions.addLast(nextPos);
        recentExtPredictedPositions.addLast(nextPos);
        // Keep queue within maximal size limit
        Position firstToRemove = null;
        if (recentPredictedPositions.size() > MAX_PREDICTED_POSITIONS) {
        	firstToRemove = recentPredictedPositions.removeFirst(); 
        }
        if (recentExtPredictedPositions.size() > MAX_EXT_PREDICTED_POSITIONS) {
        	recentExtPredictedPositions.removeFirst(); 
        }
        // Fill input for interpolation
        Position[] points = new Position[recentPredictedPositions.size()];
        
        int i = 0;
        for (Position p : recentPredictedPositions) {
            points[i++] = p;
            System.out.printf("CTL POINTS: points[%d], %.15f,,%.15f, bearing: %f\n",	i, p.getLngx(), p.getLatx(), p.getBearing());
        }
        // interpolate points using spline
        interpPath = interpolatePositions(points);
        
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
        if(LOG_KML) kml.addPosition(GFKml.PathType.PREDICTION, interpPath[index]);

        return interpPath[index];
    }

    public void stopTracking() {
    	// Dump KML
        if(LOG_KML) {
            String fileName = Environment.getExternalStorageDirectory().getPath()+"/Downloads/track.kml";
            System.out.println("Dumping KML: " + fileName); 
            FileOutputStream out;
			try {
				out = new FileOutputStream(fileName);
	            kml.write(out);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
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
    	// 1. Simple method - calculate based on speed and bearing of last position
    	return Position.predictPosition(aLastPos, timeSec*1000);

 /*   	// 2. Calculate bearing based on fusion of spline-based bearing and input GPS bearing
    	Position correctedPos = new Position();
    	correctedPos.setLngx(aLastPos.getLngx());
    	correctedPos.setLatx(aLastPos.getLatx());
    	correctedPos.setGpsTimestamp(aLastPos.getGpsTimestamp());
    	correctedPos.setDeviceTimestamp(aLastPos.getDeviceTimestamp());
    	correctedPos.setSpeed(aLastPos.getSpeed());
    	// Calculate bearing based on fusion of spline-based bearing and input GPS bearing
    	float splineWeight = 0.7f;
    	Float splineBearing = predictBearing(aLastPos.getDeviceTimestamp());
    	float bearing;
    	if (splineBearing != null) {
    		bearing = (1 - splineWeight)*aLastPos.getBearing() + 
    				splineWeight*splineBearing;
    	} else {
    		bearing = aLastPos.getBearing();
    	}
    	
    	return Position.predictPosition(correctedPos, timeSec*1000);
 */   	
    	/* 3. More sophisticated solution - calculate average acceleration and angle
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
    
    private Position[] interpolatePositions(Position[] ctrlPoints) {
    	if (!constrainControlPoints(ctrlPoints)) {
            // TODO: avoid conversion to array
    		return CardinalSpline.create(ctrlPoints).toArray(interpPath);
    	} else {
    		return ConstrainedCubicSpline.create(ctrlPoints);
    	}
    }
    
    private boolean constrainControlPoints(Position[] pts) {
    	float prevDistance = calcDistance(pts[0], pts[1]);;
    	if (prevDistance == 0) {
    		return false;
    	}
    	for (int i = 1; i < pts.length; ++i) {
    		float distance = calcDistance(pts[i], pts[i-1]);
    		float ratio = distance/prevDistance;
    		System.out.printf("constrainControlPoints i = %d, ratio: %f\n", i, ratio);
    		if (ratio >= 8.0 || ratio <= 0.125) {
    			return true;
    		}
    		prevDistance = distance;
    	}
    	return false;
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
        lastPredictedPos = recentPredictedPositions.getLast();
        lastPredictedPos.setBearing(calcCorrectedBearing(aLastGpsPos));
        lastPredictedPos.setDeviceTimestamp(aLastGpsPos.getDeviceTimestamp());
        lastPredictedPos.setGpsTimestamp(aLastGpsPos.getGpsTimestamp());        
        lastPredictedPos.setSpeed(calcCorrectedSpeed(aLastGpsPos));
    }
    
    // Correct bearing to adapt slowly to the GPS curve
    private float calcCorrectedBearing(Position aLastPos) {
    /*	Position lastPredictedPos = recentPredictedPositions.getLast();
    	// Predict position in 5 sec 
    	Position nextPredictedGpsPos = Position.predictPosition(aLastPos, 5000);
    	float bearingToNextGpsPos = Bearing.calcBearing(lastPredictedPos, nextPredictedGpsPos);
    	float bearingDiff = Bearing.bearingDiffDegrees(bearingToNextGpsPos, aLastPos.getBearing());
        System.out.printf("BEARING: %f, BEARING DIFF: %f, CORRECTED BEARING: %f\n"
        		,aLastPos.getBearing(), bearingDiff, Bearing.normalizeBearing(aLastPos.getBearing() + 0.3f*bearingDiff));
        // Correct bearing a bit to point towards 5-sec predicted position 
    	return Bearing.normalizeBearing(aLastPos.getBearing() + 0.3f*bearingDiff); */

    	// Calculate bearing from linear regression for predicted and GPS positions
    	//float[] linearPredictedBearing = calculateLinearBearing(recentExtPredictedPositions);
    	float[] linearGpsBearing = linearRegressionBearing.calculateLinearBearing(recentGpsPositions);

    	float linearBearing;    	
    	float linearBearingWeight = 0.8f;
    	
    	// If no linear heading -> we are making significant turn, switch to the GPS
    	// heading smoothly
    	if (linearGpsBearing == null) { //|| linearPredictedBearing == null) {
    		linearGpsBearing = new float[] {1.0f, 0.05f, 0.0f};
    		// Smoothen the bearing from last predicted position
    		linearBearingWeight = 0.5f;
    		linearBearing = lastPredictedPos.getBearing(); //linearPredictedBearing[0];
    	// Significance is less than X% - we are definitely sure about straight line	
    	} else if (linearGpsBearing[1] <= 0.05f) {
    		linearBearingWeight = 1.0f;
    		linearBearing = linearGpsBearing[0];
    	} else {
    		// Use linearly smoothed bearing
    		linearBearing = linearGpsBearing[0]; //linearPredictedBearing[0];
    	}
        // Combine bearing from linear regression with bearing between gps positions  		
        float bearing = 
        		Bearing.bearingDiffPercentile(linearBearing, 
        				Bearing.calcBearing(lastGpsPosition, aLastPos), 
        				(1.0f - linearBearingWeight));
        		
        System.out.printf("GPSPOS BEARING: %f, LINEAR BEARING: %f, CORRECTED BEARING: %f\n",
        		Bearing.calcBearing(lastGpsPosition, aLastPos),
        		linearBearing, bearing);
        return bearing;
    }

    private float calcCorrectedSpeed(Position aLastPos) {
    	// Do not correct position below threshold
    	if (aLastPos.getSpeed() < SPEED_THRESHOLD_MS) {
    		return aLastPos.getSpeed();
    	}
    	double offset = (gpsTraveledDistance - predictedTraveledDistance);
    	/*System.out.printf("GPS DIST: %f, EST DIST: %f, OFFSET: %f\n" , 
    			gpsTraveledDistance,predictedTraveledDistance, offset);
		*/
        double coeff = (offset > 0 ) ? 0.3 : -0.3;        
        coeff = Math.abs(offset) <= aLastPos.getSpeed() ? offset/aLastPos.getSpeed() : coeff;

        double correctedSpeed = aLastPos.getSpeed()*(1 + coeff);
        
        // System.out.printf("SPEED: %f, CORRECTED SPEED: %f, DISTANCE COEFF: %f\n",aLastPos.getSpeed(), correctedSpeed, coeff);
        return (float) correctedSpeed;
    	
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
    private class LinearRegressionBearing {
        private SimpleRegression linreg = new SimpleRegression();
        private boolean reverseLatLng = false;
        private ArrayDeque<Position> lastPosArray = new ArrayDeque<Position>();

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
	    public float[] calculateLinearBearing(ArrayDeque<Position> posArray) {
	    	// First, try predicting based on last regression via big circle
	    	float[] res = predictBearingByPreviousRegression(posArray);
	    	if (res != null) {
	    		return res;
	    	}
	        // calculate user's course by drawing a least-squares best-fit line through the last 10 positions
	    	populateRegression(posArray);
	    	System.out.printf("\nLINEAR REG SIZE: %d, SIGNIF: %f\n", posArray.size(),
	        		linreg.getSignificance());
	        // if there's a significant chance we don't have a good fit, don't calc a bearing
	        if (posArray.size() < 3 || linreg.getSignificance() > 0.05)  {
	        	linreg.clear();
	        	return null;
	        }
	        
	        System.out.printf("calculateLinearBearing LAST POS: %f,%f, slope: %f",  
	        		posArray.getLast().getLatx(), posArray.getLast().getLngx(),
	        		linreg.getSlope());
	
	        // use course to predict next position of user, and hence current bearing
	        Position next = predictPosition(posArray);
	        if (next == null)
	        	return null;
	        // return bearing to new point and some stats
	        //return Bearing.normalizeBearing(Bearing.calcBearing(recentExtPredictedPositions.getLast(), next));
	        try {
	            System.out.printf("\nRAW LINEAR BEARING: %f\n", Bearing.calcBearing(posArray.getLast(), next));
	        	float[] bearing = {
	        			Bearing.normalizeBearing(Bearing.calcBearing(posArray.getLast(), next)), 
	        			(float)linreg.getR(),
	        			(float)linreg.getSignificance()
	        	};
	            return bearing;
	        } catch(java.lang.IllegalArgumentException e) {
	        	return null;
	        }
	    }
	    // Predict bearing by last position (not including the lastest one)
	    private float[] predictBearingByPreviousRegression(ArrayDeque<Position> posArray) {
	    	if (lastPosArray.size() < 3 || posArray.size() < 3) {
	    		return null;
	    	}
			Position actualNext = posArray.getLast();
	    	
	    	// First, try predicting based on last regression
			Position predictedNext = projectPosition(actualNext);//predictPosition(lastPosArray);
			if (predictedNext == null)
				return null;
	
			System.out.printf("calculateLinearBearing predictedNext: %f,%f; %f,%f; distance: %f\n",					
					predictedNext.getLatx(), predictedNext.getLngx(),
					actualNext.getLatx(), actualNext.getLngx(),
					calcDistance(predictedNext,actualNext)
					);
			// If position predicted by previous regression, is closer than accuracy distance
			// bearing still can be used
			if (calcDistance(predictedNext,actualNext) < actualNext.getEpe()) {	        	
				System.out.printf("\nSTABLE LINEAR BEARING: %f\n", 
						Bearing.calcBearing(lastPosArray.getLast(), predictedNext));
				float[] bearing = {
							Bearing.normalizeBearing(Bearing.calcBearing(lastPosArray.getLast(), predictedNext)), 
							(float)linreg.getR(),
							(float)linreg.getSignificance()
				        	};
				return bearing;
			}	    		
	    	return null;
	    }
	    
	    private void populateRegression(ArrayDeque<Position> posArray) {
	    	reverseLatLng = false;
	    	// TODO: do not clear the whole regression but rather first and add last pos
	    	linreg.clear();
	    	lastPosArray.clear();
	        // calculate user's course by drawing a least-squares best-fit line through the last 10 positions
	    	float roundCoeff = 10.0e7f;
	        for (Position p : posArray) {
	            linreg.addData(Math.round(p.getLatx()*roundCoeff)/roundCoeff, 
	            				Math.round(p.getLngx()*roundCoeff)/roundCoeff);
	            lastPosArray.addLast(p);
	            System.out.printf("LINEAR REG PTS: %f,%f\n",  p.getLatx(), p.getLngx());
	        }
	        // Reversing
	        if (Math.abs(linreg.getSlope()) > 10.0) {
	        	linreg.clear();
		    	reverseLatLng = true;

		        for (Position p : posArray) {
		            linreg.addData(p.getLngx(), p.getLatx());
		            System.out.printf("LINEAR REG PTS: %f,%f\n",  p.getLatx(), p.getLngx());
		        }		        
	        }
	    }
	    private Position predictPosition(ArrayDeque<Position> posArray) {
	        // use course to predict next position of user, and hence current bearing
	        Position next = new Position();
	        // extrapolate latitude in same direction as last few points
	        // use regression model to predict longitude for the new point
	        if (!reverseLatLng) {
	        	next.setLatx(2*posArray.getLast().getLatx() - posArray.getFirst().getLatx());
		        next.setLngx(linreg.predict(next.getLatx()));
	        } else {
	        	next.setLngx(2*posArray.getLast().getLngx() - posArray.getFirst().getLngx());	        	
		        next.setLatx(linreg.predict(next.getLngx()));
	        }
	        if(Double.isNaN(next.getLatx())|| Double.isNaN(next.getLngx())) {
	        	return null;
	        }
	        return next;
	    	
	    }
	    
	   /* private double calcDistanceToLine(Position pos) {
	    	double diff = 0.01;
	    	double ax, ay, bx, by, px, py;
	    	if (!reverseLatLng) {
	    		ax = pos.getLatx() + diff;
	    		bx = pos.getLatx() - diff;
	    		px = projectPos.getLatx();
	    		py = projectPos.getLngx();
	    	} else {
	    		ax = pos.getLngx() + diff;
	    		bx = pos.getLngx() - diff;
	    		px = projectPos.getLngx();
	    		py = projectPos.getLatx();
	    		
	    	}
    		ay = linreg.predict(ax);
    		by = linreg.predict(bx);
	    	
	    }*/
	    
	    private Position projectPosition(Position pos) {
	    	Position projectPos = new Position();
	    	
	    	double ax, ay, bx, by, px, py;
	    	double diff = 0.01;
	    	if (!reverseLatLng) {
	    		ax = pos.getLatx() + diff;
	    		bx = pos.getLatx() - diff;
	    		px = pos.getLatx();
	    		py = pos.getLngx();
	    	} else {
	    		ax = pos.getLngx() + diff;
	    		bx = pos.getLngx() - diff;
	    		px = pos.getLngx();
	    		py = pos.getLatx();
	    		
	    	}
    		ay = linreg.predict(ax);
    		by = linreg.predict(bx);

    		double apx = px - ax;
            double apy = py - ay;
            double abx = bx - ax;
            double aby = by - ay;

            double ab2 = abx * abx + aby * aby;
            double ap_ab = apx * abx + apy * aby;
            double t = ap_ab / ab2;
            if (t < 0) {
            	t = 0;
            } else if (t > 1) {
            	t = 1;
            }
            if (!reverseLatLng) {
            	projectPos.setLatx(ax + abx * t);
            	projectPos.setLngx(ay + aby * t);
            } else {
            	projectPos.setLngx(ax + abx * t);
            	projectPos.setLatx(ay + aby * t);            	
            }
	        if(Double.isNaN(projectPos.getLatx())|| Double.isNaN(projectPos.getLngx())) {
	        	return null;
	        }
            return projectPos;
	    } 
    }
    
}
