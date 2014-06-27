package com.raceyourself.platform.gpstracker;

import java.util.ArrayDeque;

import com.raceyourself.platform.models.Position;
import com.raceyourself.platform.models.Bearing;

/**
 * CardinalSpline is responsible for creating GeneralPaths that
 *   connect a set of points with curves.
 * @author Antonio Vieiro (antonio@antonioshome.net)
 */
public class CardinalSpline 
{
  /**
   * Increment NPOINTS for better resolution (lower performance).
   */
  private static final int NPOINTS = 30;
  private static final int DELTA_MS = 1000 / NPOINTS;
  
  
  private static double[] B0;
  private static double[] B1;
  private static double[] B2;
  private static double[] B3;

  private static synchronized void initialize()
  {
    if ( B0 == null )
    {
      B0 = new double[ NPOINTS ];
      B1 = new double[ NPOINTS ];
      B2 = new double[ NPOINTS ];
      B3 = new double[ NPOINTS ];
      double deltat = 1.0 / (NPOINTS-1);
      double t = 0.0;
      double t1, t12, t2 = 0.0;
      for( int i=0; i<NPOINTS; i++ )
      {
        t1 = 1-t;
        t12 = t1*t1;
        t2 = t*t;
        B0[i] = t1*t12;
        B1[i] = 3*t*t12;
        B2[i] = 3*t2*t1;
        B3[i] = t*t2;
        t+=deltat;
      }
    }
  }

  /**
   * Creates a GeneralPath representing a curve connecting different
   * points.
   * @param points the points to connect (at least 3 points are required).
   * @return a GeneralPath that connects the points with curves.
   */
  public static ArrayDeque<Position> create( Position[] points)
  {
    initialize();
    if ( points.length <= 2 )
    {
      throw new IllegalArgumentException("At least 3 points are required to build a CardinalSpline");
    }
    // TODO: avoid new array allocation
    Position [] p = new Position[ points.length + 2 ];
    ArrayDeque<Position> path = new ArrayDeque<Position>();
    System.arraycopy( points, 0, p, 1, points.length );
    calcBoundaryPositions(p);

    System.out.printf("\nPNT[0]: %f,%f\n" ,p[0].getLatx(), p[0].getLngx());
    System.out.printf("PNT[N+1]: %f,%f\n" ,p[points.length+1].getLatx(), p[points.length+1].getLngx());

    path.addLast( p[1]);
    Position prevToLast = p[0];
    for( int i=1; i<p.length-2; i++ )
    {
      for( int j=0; j<NPOINTS; j++ )
      {
        double x = p[i].getLngx() * B0[j]
                 + (p[i].getLngx()+(p[i+1].getLngx()-p[i-1].getLngx())*0.1666667)*B1[j]
                 + (p[i+1].getLngx()-(p[i+2].getLngx()-p[i].getLngx())*0.1666667)*B2[j]
                 + (p[i+1].getLngx()*B3[j]);
        double y = p[i].getLatx() * B0[j]
                 + (p[i].getLatx()+(p[i+1].getLatx()-p[i-1].getLatx())*0.1666667)*B1[j]
                 + (p[i+1].getLatx()-(p[i+2].getLatx()-p[i].getLatx())*0.1666667)*B2[j]
                 + (p[i+1].getLatx()*B3[j]);
        Position pos = new Position();
        pos.setLngx(x);
        pos.setLatx(y);
        // Interpolate timestamps
        int deltaTimeMilliseconds = j*DELTA_MS;
        pos.setGpsTimestamp(p[i].getGpsTimestamp() + deltaTimeMilliseconds);
        pos.setDeviceTimestamp(p[i].getDeviceTimestamp() + deltaTimeMilliseconds);
        // TODO: interpolate speed
        pos.setSpeed(p[i].getSpeed());
        // Calculate bearing of last position in path
        Float bearing = calcBearing(prevToLast, pos);
        System.out.printf("SPLINE BEARING: %f\n" ,bearing);
        path.getLast().setBearing(bearing);        
        prevToLast = path.getLast();
        path.addLast(pos);
      }
    }
    // Calculate bearing for last position in path
    path.getLast().setBearing(calcBearing(prevToLast, p[p.length-1]));
    
    return path;
  }
  // Calculate bearing for p1 based on previous (p0) and next (p2) points
  private static Float calcBearing(Position p0, Position p2) {
	  // TODO: use tightness for more precise calculation
	  //return Bearing.calcBearing(p0, p2);	  
      // Interpolate bearing. TODO: check if tightness is required here 
      float bearing = (float)Math.toDegrees(/*TIGHTNESS * */Bearing.calcBearingInRadians(p0, p2)) % 360;
      System.out.printf("SPLINE BEARING BEFORE NORM: %f\n" ,bearing);

      return Bearing.normalizeBearing(bearing);
      
  }
 
  private static void calcBoundaryPositions(Position[] p) {
	    p[0] = new Position();
	    p[p.length-1] = new Position();
	    
	    p[0] = p[1]; 
	    p[p.length-1] = p[p.length-2];
	    
  }
  
  // Returns number of interpolated points in between control points
  public static int getNumberPoints() {
      return NPOINTS;
  }
  
}
