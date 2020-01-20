package edu.purdue.jtk;


/**
 * The Statistic class is used to compute and display basic statistical values.
 *
 * Formulas from http://datagenetics.com/blog/november22017
 *
 * u_n = u_n-1 + (x_n - u_n-1) / n
 * S_n = S_n-1 + (x_n - u_n-1)(x_n - u_n)
 * s_n = sqrt(S_n / n)
 */
public class Statistic {
    private String name;
    private double max, min;
    private double mean, meanPrevious;
    private double sigma2n, sigma2nPrevious;
    private int n;

    public Statistic(String name) {
        this.name = name;
        reset();
    }

    public void reset() {
        max = Double.MIN_VALUE;
        min = Double.MAX_VALUE;
        mean = meanPrevious = 0.0;
        sigma2n = sigma2nPrevious = 0.0;
        n = 0;
    }

    public void accumulate(double x) {
        n += 1;

        max = Math.max(max, x);
        min = Math.min(min, x);

        mean = meanPrevious + (x - meanPrevious) / n;
        sigma2n = sigma2nPrevious + (x - meanPrevious) * (x - mean);

        meanPrevious = mean;
        sigma2nPrevious = sigma2n;
    }

    public double getMin() { return min; }
    public double getMax() { return max; }
    public double getMean() { return mean; }
    public double getSigma() { return Math.sqrt(sigma2n / n); }
    public int getN() { return n; }

    public String toString() {
        return String.format("%-15s: %14.6f <= %14.6f @ %14.6f <= %14.6f (N = %d)",
                name, getMin(), getMean(), getSigma(), getMax(), getN());
    }
}
