package bubbles;

import edu.purdue.jtk.*;
import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * Draw bubbles in response to brain waves.
 */
public class Bubbles extends PApplet {
    // Declare variables used to interact with the Muse.
    private Muse muse;
    private MuseControl mc;
    private PGraphics pg;

    public void settings() {
        fullScreen(1);
//        size(624, 416);
    }

    /**
     * Do standard Processing setup.  To use the Muse headband, must create a Muse object.
     */
    public void setup() {
        frameRate(30);

        muse = new Muse();
        mc = muse.getMuseControl();
        pg = createGraphics(width, height);
        resetGraphics();
    }

    public void keyPressed() {
        mc.keyPressed(key, keyCode);
    }

    private void resetGraphics() {
        pg.beginDraw();
        pg.background(0);
        pg.endDraw();
    }

    /**
     * Do standard Processing draw.  This example shows some of the functions available to query Muse parameters.
     */
    public void draw() {
        background(0);
        if (muse.isPaused() || muse.noSource()) {  // NB: Going from Generator to Headband doesn't resetGraphics.
            if (muse.noSource()) resetGraphics();
            return;
        }

        // Relocate origin to center of window...
        pg.beginDraw();
        pg.pushMatrix();
        pg.translate(width/2, height/2);

        float useScale = mc.getScaleValue() / 100F;


        // Draw the waves...
        for (Wave wave : Wave.values())
            if (mc.getShowWave(wave))
                drawWave(wave, useScale, pg);

        // Draw the focus...
        if (mc.getShowFocus())
            drawFocus(useScale, pg);

        // Draw central dot...
        pg.fill(0);
        pg.stroke(0);
        pg.circle(0, 0, 10);
        pg.popMatrix();
        pg.endDraw();

        pushMatrix();
        image(pg, 0, 0);

        // Draw the activity...
        translate(width/2, height/2);
        if (mc.getShowActivity())
            drawActivity(useScale);

        popMatrix();
    }

    private void drawWave(Wave wave, float useScale, PGraphics pg) {
        PointVector pvWave = muse.computeWaveVector(wave);

        pg.pushStyle();
        pg.stroke(0, 30);

        int fillColor = Rainbow.get(pvWave.magnitude());
        pg.fill(fillColor, 50);

        // Since pvWave.x and pvWave.y are in range [-1 .. +1], scale such that the values fill width and height of window...
        float x = pvWave.x * width / 2 * useScale;
        float y = -pvWave.y * height / 2 * useScale;

        float diameter = 25 + Math.abs(x) + Math.abs(y);
        pg.circle(x, y, diameter);

        pg.fill(0);
        pg.text(wave.getName(), x - pg.textWidth(wave.getName()) / 2, y);

        pg.popStyle();
    }

    private void drawFocus(float useScale, PGraphics pg) {
        // Focus is a vector (two-tuple) giving an x and y direction for the overall focus.  It is the vector sum
        // of all five waves at each of the four sensors.
        PointVector pvFocus = muse.computeFocus();

        // The magnitude of the vector represents the "amount" of focus the user has.  Lower values are more focus.
        float mag = pvFocus.magnitude();
        assert mag <= 1.0;

        // Draw a circle with centered at the current focus.
        // Use pvFocus.x and pvFocus.y (in range [-1..+1]) to scale to fill width and height of window...
        float x = pvFocus.x * width / 2 * useScale;
        float y = -pvFocus.y * height / 2 * useScale;
        assert pvFocus.x <= 1.0 && pvFocus.y <= 1.0;

        pg.pushStyle();

        // Rainbow.getGrid(mag) returns a perceptually-uniform color (int) for float values in the range [0..1].
        // Set fill color related to overall user focus...
        pg.fill(Rainbow.get(mag), 150);
        pg.stroke(0);

        float diameter = 25 + Math.abs(x)/2 + Math.abs(y)/2;
        pg.circle(x, y, diameter);

        pg.fill(0);
        pg.text("Focus", x - pg.textWidth("Focus") / 2, y);

        pg.popStyle();
    }

    private void drawActivity(float useScale) {
        float activity = muse.computeActivity();

        pushStyle();

        int w = (int) (width * activity);
        int x = -w / 2;
        int y = height / 2 - 70;

        noStroke();

        fill(Rainbow.get(activity));
        rect(x, y, w, 40, 15);

        popStyle();
    }

    public static void main(String[] args) {
        String[] appletArgs = new String[] { "Bubbles" };
        PApplet.main(appletArgs);
    }
}
