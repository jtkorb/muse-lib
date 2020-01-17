package edu.purdue.jtk;

// Numbers from here:  https://numbergenerator.org/randomnumbergenerator
// Calculations from here: https://www.calculator.net/standard-deviation-calculator.html

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatisticTest {
    Statistic s1, s2;
    static double values[];

    @BeforeEach
    void setUp() {
        s1 = new Statistic("T1");
        values = new double[]{10, 12, 23, 23, 16, 23, 21, 16};
        for (int i = 0; i < values.length; i++)
            s1.accumulate(values[i]);

        s2 = new Statistic("T2");
        values = new double[] {
                590,113,184,173,296,289,458,428,952,616,583,4,222,102,430,41,718,321,341,744,221,708,218,210,625,729,
                524,458,220,745,227,537,894,269,732,506,105,335,924,674,739,440,276,263,745,612,24,900,735,734,209,
                767,566,530,780,437,199,665,772,814,88,506,687,152,828,528,256,355,694,764,981,256,835,212,186,906,
                248,607,807,973,730,496,854,664,881,425,204,832,180,198,747,468,118,861,448,732,802,679,758,471};
        for (int i = 0; i < values.length; i++)
            s2.accumulate(values[i]);
    }

    @Test
    void accumulate() {
    }

    @Test
    void getMean() {
        assertEquals(18.000000000000004, s1.getMean());
        assertEquals(510.90000000000003, s2.getMean());
    }

    @Test
    void getSigma() {
        assertEquals(4.898979485566356, s1.getSigma());
        assertEquals(266.9258511272372, s2.getSigma());
    }
}
