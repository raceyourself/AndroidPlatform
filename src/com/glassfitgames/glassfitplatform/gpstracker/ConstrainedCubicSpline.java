package com.glassfitgames.glassfitplatform.gpstracker;

import com.glassfitgames.glassfitplatform.models.Bearing;
import com.glassfitgames.glassfitplatform.models.Position;

// http://www.korf.co.uk/spline.pdf
public class ConstrainedCubicSpline {
	private static Position[] ctlPoints;
	private static int NPOINTS = 30;
	private static final int DELTA_MS = 1000 / NPOINTS;

	
	public static Position[] create(Position[] aCtlPoints) {
		ctlPoints = aCtlPoints;
		Position[] interpPoints = new Position[NPOINTS * (ctlPoints.length-1)];
		for (int i = 1; i < ctlPoints.length; ++i) {
			double Ai = Ai(i);
			double Bi = Bi(i);
			double Ci = Ci(i);
			double Di = Di(i);
			for (int j = 0; j < NPOINTS; ++j ) {
				double X = X(i-1) + j*((X(i) - X(i-1))/NPOINTS);
				double Y = Ai + Bi*X + Ci*Math.pow(X,2) + Di*Math.pow(X,3);
				// Update interpolated position
				Position pos = new Position();
				pos.setLngx(X);
				pos.setLatx(Y);
		        // Interpolate timestamps
		        int deltaTimeMilliseconds = j*DELTA_MS;
		        pos.setGpsTimestamp(ctlPoints[i-1].getGpsTimestamp() + deltaTimeMilliseconds);
		        pos.setDeviceTimestamp(ctlPoints[i-1].getDeviceTimestamp() + deltaTimeMilliseconds);
		        // TODO: interpolate speed
		        pos.setSpeed(ctlPoints[i-1].getSpeed());
		        
		        int index = (i-1)*NPOINTS + j;
		        interpPoints[index] = pos;
		        if (index > 1) {
			        // Calculate bearing of previous position in path based on two adjacent
		        	// positions
			        float bearing = Bearing
			        		.normalizeBearing(Bearing
			        				.calcBearing(interpPoints[index-2],
			        							  interpPoints[index]));
			        interpPoints[index-1].setBearing(bearing); 
		        	
		        }		        
			}
			// Set first position's bearing			
			interpPoints[0].setBearing(aCtlPoints[0].getBearing());
			// Set last position to be equal to last control point
			interpPoints[interpPoints.length-1] = aCtlPoints[aCtlPoints.length-1];
		}
		return interpPoints;
	}
	
	private static double X(int i) {
		return ctlPoints[i].getLngx();
	}

	private static double Y(int i) {
		return ctlPoints[i].getLatx();
	}
	// Polynom coefficients
	public static double Di(int i) {
		return (fDer2Xi(i) - fDer2Xi1(i))/(6.0*(X(i) - X(i-1)));
	}

	public static double Ci(int i) {
		return (X(i)*fDer2Xi1(i) - X(i-1)*fDer2Xi(i))/(2.0*(X(i) - X(i-1)));
	}

	public static double Bi(int i) {
		return ((Y(i) - Y(i-1)) - Ci(i)*(Math.pow(X(i),2) - Math.pow(X(i-1),2))
				-Di(i)* (Math.pow(X(i),3) - Math.pow(X(i-1),3))) 
				/ (X(i) - X(i-1));						
	}
	public	static double Ai(int i) {
		return Y(i-1) - Bi(i)*X(i-1) - Ci(i)*Math.pow(X(i-1), 2) - Di(i)*Math.pow(X(i-1), 3);
	}

	// Second derivative at point Xi-1
	public static double fDer2Xi1(int i) {
		return -2.0*(fDer1Xi(i) + 2.0*fDer1Xi(i-1))/(X(i) - X(i-1))
				+ 6.0*(Y(i) - Y(i-1))/Math.pow(X(i) - X(i-1), 2);
	}

	// Second derivative at point Xi
	public static double fDer2Xi(int i) {
		return 2.0*(2.0*fDer1Xi(i) + fDer1Xi(i-1))/(X(i) - X(i-1))
				- 6.0*(Y(i) - Y(i-1))/Math.pow(X(i) - X(i-1), 2);
	}
	
	// First derivative at point Xi, n > i > 0
	public static double fDer1Xi(int i ) {
		if (i == 0) 
			return fDer1X0();
		if (i == ctlPoints.length-1)
			return fDer1Xn();
		return 2.0/(((X(i+1) - X(i))/(Y(i+1) - Y(i)) +
				(X(i) - X(i-1))/(Y(i) - Y(i-1))));		
	}
	
	// First derivative at point X0
	private static double fDer1X0() {
		return 3.0*(Y(1) - Y(0)) / (2.0 * (X(1) - X(0)))
				- 0.5*fDer1Xi(1);
	}

	// First derivative at point Xn
	private static double fDer1Xn() {
		int n = ctlPoints.length-1;
		return 3.0*(Y(n) - Y(n-1)) / (2.0 * (X(n) - X(n-1)))
				- 0.5*fDer1Xi(n-1);
	}


	
}
