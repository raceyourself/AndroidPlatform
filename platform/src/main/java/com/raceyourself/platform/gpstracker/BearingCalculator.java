package com.raceyourself.platform.gpstracker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import android.os.Environment;
import android.util.Log;
import au.com.bytecode.opencsv.CSVWriter;

import com.raceyourself.platform.models.Bearing;
import com.raceyourself.platform.models.Position;
import com.raceyourself.platform.models.EnhancedPosition;

public class BearingCalculator {
	private boolean LOG_CSV = false;
	private BearingLogger bearingLogger = new BearingLogger();

    LinearRegressionBearing linearRegressionBearing = new LinearRegressionBearing();

    Position lastGpsPosition;
    Position lastPredictedPos;
    ArrayDeque<EnhancedPosition> recentGpsPositions;
    // Compass azimuth
    float azimuth;
    // Sight direction
    float yaw;
    // Bearing calculation algorithm
    Algorithm algo = Algorithm.COMBINED;
    
    public enum Algorithm { 
        YAW, 
        GPS,
        COMBINED
    };
 
    
    public BearingCalculator(ArrayDeque<EnhancedPosition> aRecentGpsPositions) {
    	recentGpsPositions = aRecentGpsPositions;
    }
    
    public void setAlgorithm(Algorithm aAlgo) {
    	if (aAlgo != null) {
    		algo = aAlgo;
    	}    	
    }
    
    // Main entry method - should be called on every position update
    public void updatePosition(EnhancedPosition aLastPredictedPos) {

    	lastPredictedPos = aLastPredictedPos;
    	updateAzimuth(aLastPredictedPos.getAzimuth());
    	updateYaw(aLastPredictedPos.getYaw());
    	
    	if (!recentGpsPositions.isEmpty()) {
    		lastGpsPosition = recentGpsPositions.getLast();
    	}
    	//System.out.printf("RECENT GPS POSITIONS SIZE: %d", recentGpsPositions.size());
    }
    
    private void updateAzimuth(float aAzimuth) {
    	azimuth = aAzimuth;
    	
        // Write debug
        bearingLogger.logAzimuth(azimuth);
        bearingLogger.writeLine();
    }

    private void updateYaw(float aYaw) {
    	yaw = aYaw;
    	
        // TODO: Write debug
        //bearingLogger.logYaw(yaw);
        //bearingLogger.writeLine();
    }

    
    public void reset() {
    	bearingLogger.close();
    }
    // Correct bearing to adapt slowly to the GPS curve
    public float calcBearing(Position aLastPos) {
    	if (algo == Algorithm.YAW) {
    		return yaw;
    	}
    	if (algo == Algorithm.GPS) {
    		return Bearing.calcBearing(lastGpsPosition, aLastPos);
    	}
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
    		// Switch to positions-based bearing
    		linearBearingWeight = 0.0f;
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
        		
       //System.out.printf("GPSPOS BEARING: %f, LINEAR BEARING: %f, CORRECTED BEARING: %f\n",
       // 		Bearing.calcBearing(lastGpsPosition, aLastPos),
      //  		linearBearing, bearing);
        // Debug log
        bearingLogger.logCombinedBearing(bearing);
        bearingLogger.logPositionBearing(Bearing.calcBearing(lastGpsPosition, aLastPos));

        return bearing;
    }

    private class YawBearing {
    	private float calculateBearing() {
    		return yaw;
    	}
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
	    public float[] calculateLinearBearing(ArrayDeque<EnhancedPosition> recentGpsPositions) {
	    	// First, try predicting based on last regression via big circle
	    	float[] prevRegressionResults = predictBearingByPreviousRegression(recentGpsPositions);
	    	// Next, run normal linear regression
	    	float[] currRegressionResults = predictBearingByCurrentRegression(recentGpsPositions);	

	    	// Log results (if on)
	    	bearingLogger.logWeightedRegression(prevRegressionResults);
	    	bearingLogger.logLinearRegression(currRegressionResults);	    		
	    	
	    	// Prefer "big circle" regression results
	    	// TODO: make algorithm selectable
	    	/*if (prevRegressionResults != null) {
	    		return prevRegressionResults;
	    	}*/
	    	return currRegressionResults;
	    }
	    // Predict bearing by last position (not including the lastest one)
	    private float[] predictBearingByPreviousRegression(ArrayDeque<EnhancedPosition> recentGpsPositions) {
	    	if (lastPosArray.size() < 3 || recentGpsPositions.size() < 3) {
	    		return null;
	    	}
			Position actualNext = recentGpsPositions.getLast();
	    	
	    	// First, try predicting based on last regression
			Position predictedNext = projectPosition(actualNext);//predictPosition(lastPosArray);
			if (predictedNext == null)
				return null;
	
/*			System.out.printf("calculateLinearBearing predictedNext: %f,%f; %f,%f; distance: %f\n",					
					predictedNext.getLatx(), predictedNext.getLngx(),
					actualNext.getLatx(), actualNext.getLngx(),
					calcDistance(predictedNext,actualNext)
					); */
			// If position predicted by previous regression, is closer than accuracy distance
			// bearing still can be used
			if (Position.distanceBetween(predictedNext,actualNext) < actualNext.getEpe()) {	        	
/*				System.out.printf("\nSTABLE LINEAR BEARING: %f\n", 
						Bearing.calcBearing(lastPosArray.getLast(), predictedNext)); */
				float[] bearing = {
							Bearing.normalizeBearing(Bearing.calcBearing(lastPosArray.getLast(), predictedNext)), 
							(float)linreg.getR(),
							(float)linreg.getSignificance()
				        	};
				return bearing;
			}	    		
	    	return null;
	    }
	    
	    private float[] predictBearingByCurrentRegression(ArrayDeque<EnhancedPosition> posArray) {
	        // calculate user's course by drawing a least-squares best-fit line through the last 10 positions
	    	populateRegression(posArray);
/*	    	System.out.printf("\nLINEAR REG SIZE: %d, SIGNIF: %f\n", posArray.size(),
	        		linreg.getSignificance()); */
	        // if there's a significant chance we don't have a good fit, don't calc a bearing
	        if (posArray.size() < 3 || linreg.getSignificance() > 0.05)  {
	        	linreg.clear();
	        	return null;
	        }
	        
/*	        System.out.printf("calculateLinearBearing LAST POS: %f,%f, slope: %f",  
	        		posArray.getLast().getLatx(), posArray.getLast().getLngx(),
	        		linreg.getSlope()); */
	
	        // use course to predict next position of user, and hence current bearing
	        Position next = predictPosition(posArray);
	        if (next == null)
	        	return null;
	        // return bearing to new point and some stats
	        //return Bearing.normalizeBearing(Bearing.calcBearing(recentExtPredictedPositions.getLast(), next));
	        try {
	            //System.out.printf("\nRAW LINEAR BEARING: %f\n", Bearing.calcBearing(posArray.getLast(), next));
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
	    
	    private void populateRegression(ArrayDeque<EnhancedPosition> posArray) {
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
	            //System.out.printf("LINEAR REG PTS: %f,%f\n",  p.getLatx(), p.getLngx());
	        }
	        // Reversing
	        if (Math.abs(linreg.getSlope()) > 10.0) {
	        	linreg.clear();
		    	reverseLatLng = true;

		        for (Position p : posArray) {
		            linreg.addData(p.getLngx(), p.getLatx());
		            //System.out.printf("LINEAR REG PTS: %f,%f\n",  p.getLatx(), p.getLngx());
		        }		        
	        }
	    }
	    private Position predictPosition(ArrayDeque<EnhancedPosition> posArray) {
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

    class BearingLogger {
    	private int INVALID_BEARING = -999;
    	float[] bearingLine = new float[5];
    	
    	CSVWriter csvWriter;
    	
    	BearingLogger() {
    		if (!LOG_CSV) return;
    		
    		reset();
            String fileName = Environment.getExternalStorageDirectory().getPath()+"/Downloads/bearing.csv";

            File outCsv = new File(fileName);
			try {
				csvWriter = new CSVWriter(new FileWriter(outCsv));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            String[] headerList = {"weightReg", "linReg", "spline", "azimuth", "combined_bearing"};
            csvWriter.writeNext(headerList);

    	}
    	
		public void logWeightedRegression(float[] prevRegressionResults) {
			if (!LOG_CSV) return;
			float bearing = INVALID_BEARING;
			if (prevRegressionResults != null) {
				bearing = prevRegressionResults[0];
			} 
			bearingLine[0] = bearing;
			
		}
		public void logLinearRegression(float[] regResults) {
			if (!LOG_CSV) return;
			float bearing = INVALID_BEARING;
			if (regResults != null) {
				bearing = regResults[0];
			} 
			bearingLine[1] = bearing;
		}

		public void logPositionBearing(Float splineBearing) {
			logBearing(splineBearing, 2);
		}

		public void logAzimuth(Float azimuth) {
			logBearing(azimuth, 3);
		}
			
		public void logCombinedBearing(Float bearing) {
			logBearing(bearing, 4);
		}
    	
    	public void close() {
    		try {
				csvWriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NullPointerException e) {
			    // CSVwriter is null, don't worry about it
			    Log.w("BearingCalculation", "CSVwriter was null on close.");
			}
    	}
		
		public void writeLine() {
			if (!LOG_CSV) return;			

			String[] bearingLineString = new String[bearingLine.length];
			for (int i = 0; i < bearingLine.length; ++i){
				bearingLineString[i] = Float.toString(bearingLine[i]);
			}
			
            csvWriter.writeNext(bearingLineString);
			
		}
		
		private void logBearing(Float aBearing, int index) {
			if (!LOG_CSV) return;			
			float bearing = INVALID_BEARING;
			if (aBearing != null) {
				bearing = aBearing;
			} 
			bearingLine[index] = bearing;

		}
		
		private void reset() {
			for (int i = 0; i < bearingLine.length; ++i){
				bearingLine[i] = INVALID_BEARING;
			}
		}		
    }

    
}
