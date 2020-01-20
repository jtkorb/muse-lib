package edu.purdue.jtk;

/**
 * PointVector defines a simple point vector class for use in managing (x,y) coordinates when visualizing data.
 */
public class PointVector {
    public float x, y;

    public PointVector(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void add(float x, float y) {
        this.x += x;
        this.y += y;
    }

    public float magnitude() {
        return (float) Math.sqrt(x * x + y * y);
    }

    public String toString() { return String.format("(%5.3f,%5.3f)", x, y); }
}
