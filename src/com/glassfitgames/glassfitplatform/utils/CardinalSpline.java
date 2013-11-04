package com.glassfitgames.glassfitplatform.utils;

import java.awt.Point;
import java.awt.geom.GeneralPath;

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
  public static GeneralPath create( Point [] points )
  {
    initialize();
    if ( points.length <= 2 )
    {
      throw new IllegalArgumentException("At least 3 points are required to build a CardinalSpline");
    }
    Point [] p = new Point[ points.length + 2 ];
    GeneralPath path = new GeneralPath();
    System.arraycopy( points, 0, p, 1, points.length );
    int n = points.length;
    p[0] = new Point( 2*points[0].x - 2*points[1].x + points[2].x,
                      2*points[0].y - 2*points[1].y + points[2].y );
    p[points.length+1] = new Point( 2*p[n-2].x - 2*p[n-1].x + p[n].x,
                                    2*p[n-2].x - 2*p[n-1].x + p[n].x );

    path.moveTo( p[1].x, p[1].y );
    for( int i=1; i<p.length-2; i++ )
    {
      for( int j=0; j<NPOINTS; j++ )
      {
        double x = p[i].x * B0[j]
                 + (p[i].x+(p[i+1].x-p[i-1].x)/6)*B1[j]
                 + (p[i+1].x-(p[i+2].x-p[i].x)/6)*B2[j]
                 + (p[i+1].x*B3[j]);
        double y = p[i].y * B0[j]
                 + (p[i].y+(p[i+1].y-p[i-1].y)/6)*B1[j]
                 + (p[i+1].y-(p[i+2].y-p[i].y)/6)*B2[j]
                 + (p[i+1].y*B3[j]);
        path.lineTo( (float)x, (float)y );
      }
    }
    return path;
  }

}
