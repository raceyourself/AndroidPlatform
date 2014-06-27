package com.raceyourself.platform.gpstracker.catmullrom;

import com.raceyourself.platform.models.Position;

public class Point2D {
	   private float x, y;

	    public Point2D() {
	        this(0f, 0f);
	    }

	    public Point2D(float x, float y) {
	        this.x = x;
	        this.y = y;
	    }

	    public Point2D(Position pos) {
	        this.x = (float) pos.getLngx();
	        this.y = (float) pos.getLatx();
	    }

	    public Position toPosition() {
	    	Position p = new Position();
	    	p.setLngx(x);
	    	p.setLatx(y);
	    	return p;
	    }
	    
	    /**
	     * @return the x
	     */
	    public float getX() {
	        return x;
	    }

	    /**
	     * @param x the x to set
	     */
	    public void setX(float x) {
	        this.x = x;
	    }

	    /**
	     * @return the y
	     */
	    public float getY() {
	        return y;
	    }

	    /**
	     * @param y the y to set
	     */
	    public void setY(float y) {
	        this.y = y;
	    }
}
