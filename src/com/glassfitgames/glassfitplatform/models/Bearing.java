package com.glassfitgames.glassfitplatform.models;

import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;

// Utilities for bearing calculation
public class Bearing {

	public static float calcBearing(Position from, Position to) {
		System.out.printf("\ncalcBearing from (%f, %f) to (%f,%f)\n", from.getLatx(), from.getLngx(),
				to.getLatx(), to.getLngx());
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
    
    public static float normalizeBearing(float bearing) {
    	return (float) LatLngTool.normalizeBearing(bearing);
    }
    
    // Calculate minimal angle difference (in degrees) between two 
    public static float bearingDiffDegrees(float bearing1, float bearing2) {
    	float diff = bearing1 - bearing2;
    	diff  += (diff>180) ? -360 : (diff<-180) ? 360 : 0;
    	return diff;
    }
    // Returns bearing which is in between bearing1 and bearing2 at percentile position.
    // E.g. for b1 = 120, b2 = 60, p = 0.4 will return 120 - (120 - 60)*0.4 = 96
    // for b1 = 300, b2 = 10, p = 0.8 will return 300 + (360+10 - 300) * 0.8 = 356
    public static float bearingDiffPercentile(float bearing1, float bearing2, float percentile) {
    	float diff = bearingDiffDegrees(bearing1, bearing2);
    	int sign = 1;
    	if (normalizeBearing(bearing1 - diff) == bearing2) {
    		sign = -1;
    	}
		return normalizeBearing(bearing1 + sign*percentile*diff);
    }

	// Change bearing direction - clock wise -> counter clockwise and vice versa
    public static float swapClockwise(float bearing) {
        return 360 - bearing;	
    }
    
}
