package com.raceyourself.platform.gpstracker.catmullrom;

public class CatmullRomSpline2D {
    private CatmullRomSpline splineXVals, splineYVals;

    public CatmullRomSpline2D(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
        assert p0 != null : "p0 cannot be null";
        assert p1 != null : "p1 cannot be null";
        assert p2 != null : "p2 cannot be null";
        assert p3 != null : "p3 cannot be null";

        splineXVals = new CatmullRomSpline(p0.getX(), p1.getX(), p2.getX(), p3.getX());
        splineYVals = new CatmullRomSpline(p0.getY(), p1.getY(), p2.getY(), p3.getY());
    }

    public Point2D q(float t) {
        return new Point2D(splineXVals.q(t), splineYVals.q(t));
    }

}
