package com.glassfitgames.glassfitplatform.gpstracker;

import java.util.ArrayDeque;
import com.glassfitgames.glassfitplatform.models.Position;
import com.glassfitgames.glassfitplatform.gpstracker.BearingCalculationAlgorithm;

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
  
  // Tigtness: 1 = straight line
  private static final double TIGHTNESS = 0.5;
  
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
  public static ArrayDeque<Position> create( Position[] points )
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
    int n = points.length;
    p[0] = new Position();
    p[0].setLngx(2*points[0].getLngx() - 2*points[1].getLngx()  + points[2].getLngx());
    p[0].setLatx(2*points[0].getLatx() - 2*points[1].getLatx() + points[2].getLatx());
    p[points.length+1] = new Position();
    p[points.length+1].setLngx(2*p[n-2].getLngx() - 2*p[n-1].getLngx() + p[n].getLngx());
    p[points.length+1].setLatx(2*p[n-2].getLatx() - 2*p[n-1].getLatx() + p[n].getLatx());

    path.addLast( p[1]);
    Position prevToLast = p[0];
    for( int i=1; i<p.length-2; i++ )
    {
      for( int j=0; j<NPOINTS; j++ )
      {
        double x = p[i].getLngx() * B0[j]
                 + (p[i].getLngx()+(p[i+1].getLngx()-p[i-1].getLngx())*TIGHTNESS)*B1[j]
                 + (p[i+1].getLngx()-(p[i+2].getLngx()-p[i].getLngx())*TIGHTNESS)*B2[j]
                 + (p[i+1].getLngx()*B3[j]);
        double y = p[i].getLatx() * B0[j]
                 + (p[i].getLatx()+(p[i+1].getLatx()-p[i-1].getLatx())*TIGHTNESS)*B1[j]
                 + (p[i+1].getLatx()-(p[i+2].getLatx()-p[i].getLatx())*TIGHTNESS)*B2[j]
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
        Float bearing = calcBearing(prevToLast, path.getLast(), pos);
        path.getLast().setBearing(bearing);        
        //System.out.printf("INTERP: %.15f,,%.15f %f %d\n", pos.getLngx(), pos.getLatx(), path.getLast().getBearing(), pos.getDeviceTimestamp());
        prevToLast = path.getLast();
        path.addLast(pos);
      }
    }
    return path;
  }
  // Calculate bearing for p1 based on previous (p0) and next (p2) points
  private static Float calcBearing(Position p0, Position p1, Position p2) {
      // Interpolate bearing 
      float bearing = (float)Math.toDegrees(TIGHTNESS * BearingCalculationAlgorithm.calcBearingInRadians(p0, p2)) % 360;
      bearing = bearing >= 0 ? bearing : 360 + bearing;    
      //float bearing = BearingCalculationAlgorithm.calcBearing(p1, p2);
      return bearing;
  }
  
  public static int getNumberPoints() {
      return NPOINTS;
  }
  
}
