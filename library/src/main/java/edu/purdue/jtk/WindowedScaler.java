package edu.purdue.jtk;

import java.util.LinkedList;

class WindowedScaler {
    /*
     * Track and Scale Wave Values over a Sliding Window
     *
     * References:
     *     https://stackoverflow.com/questions/27170216/keep-track-on-biggest-element-in-fifo-queue
     *     https://www.geeksforgeeks.org/window-sliding-technique/
     *     https://www.nayuki.io/page/sliding-window-minimum-maximum-algorithm
     */
    private LinkedList<Float> queue = new LinkedList<>();
    private LinkedList<Float> maxima = new LinkedList<>();
    private LinkedList<Float> minima = new LinkedList<>();

    private int countMax;

    WindowedScaler(int countMax) {
        this.countMax = countMax;
    }

    void resetScale() {
        queue = new LinkedList<>();
        maxima = new LinkedList<>();
        minima = new LinkedList<>();
    }

    float scale(float value, boolean allowUpscaling) {
        enqueue(value);

        float min = minima.peekFirst();
        float max = maxima.peekFirst();

        // If we're only downscaling (not upscaling), then don't let min float up or max float down...
        if (!allowUpscaling) {
            min = Math.min(0, min);
            max = Math.max(1, max);
        }

        if (min == max)
            return 0.5F;
        else
            return (value - min) / (max - min);
    }

    private void enqueue(float value) {
        while (!maxima.isEmpty() && value > maxima.peekLast())  // remove any smaller values starting at bottom of the maxima queue
            maxima.removeLast();
        maxima.addLast(value);  // add this value to the end of the maxima queue

        while (!minima.isEmpty() && value < minima.peekLast())
            minima.removeLast();
        minima.addLast(value);  // add this value to the end of the minima queue

        queue.addLast(value);  // add this value to the end of the main queue
        if (queue.size() > countMax) {
            dequeue();
            assert queue.size() == countMax;
        }
    }

    private void dequeue() {
        if (maxima.peekFirst().equals(queue.peekFirst()))
            maxima.removeFirst();
        if (minima.peekFirst().equals(queue.peekFirst()))
            minima.removeFirst();
        queue.removeFirst();
    }
}
