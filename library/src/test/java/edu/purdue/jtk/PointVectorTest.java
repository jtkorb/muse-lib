package edu.purdue.jtk;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PointVectorTest {
    PointVector pv1, pv2;

    static double EPSILON = 0.00001;

    @BeforeEach
    void setUp() {
        pv1 = new PointVector(1, 1);
        pv2 = new PointVector(2, 2);
    }

    @Test
    void add() {
        pv1.add(1, 1);
        assertEquals(2, pv1.x, EPSILON);
        assertEquals(2, pv1.y, EPSILON);

        pv2.add(3, 5);
        assertEquals(5, pv2.x, EPSILON);
        assertEquals(7, pv2.y, EPSILON);
    }

    @Test
    void magnitude() {
        assertEquals(Math.sqrt(2), pv1.magnitude(), EPSILON);
        assertEquals(Math.sqrt(8), pv2.magnitude(), EPSILON);
    }
}
