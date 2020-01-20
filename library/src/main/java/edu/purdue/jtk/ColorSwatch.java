package edu.purdue.jtk;

import controlP5.ControlP5;
import controlP5.Controller;
import controlP5.ControllerView;
import processing.core.PFont;
import processing.core.PGraphics;

/**
 * ColorSwatch is a controller class for creating color swatch objects and their associated views.  Used with ControlP5.
 *
 * @author Tim Korb
 * @since 1.0.0
 */
class ColorSwatch extends Controller<ColorSwatch> {
    final static int WIDTH = 50;
    final static int HEIGHT = 285;

    /**
     * Creates a color swatch object for on-screen viewing.
     *
     * @param controlP5     the control where the object will appear
     * @param pf            the font to use
     * @param fontColor     the font color
     * @param s             the string to display in the view
     */
    ColorSwatch(ControlP5 controlP5, PFont pf, int fontColor, String s) {
        super(controlP5, s);
        setView(new ColorSwatchView(pf, fontColor, WIDTH, HEIGHT));
    }

    public int getWidth() { return WIDTH; }
    public int getHeight() { return HEIGHT; }

}

/**
 * ColorSwatchView is a class for viewing ColorSwatch objects.
 */
class ColorSwatchView implements ControllerView<ColorSwatch> {
    private PFont pf;
    private int fontColor;
    private int width;
    private int height;

    /**
     * Creates a view of the color swatch object.
     *
     * @param pf            the font to use for labeling the swatches
     * @param fontColor     the font color
     * @param width         the width of the swatch
     * @param height        the height of the entire swatch
     */
    ColorSwatchView(PFont pf, int fontColor, int width, int height) {
        this.pf = pf;
        this.fontColor = fontColor;
        this.width = width;
        this.height = height;
    }

    /**
     * Displays a color swatch.  Called by ControlP5.
     *
     * @param p             the PGraphics object for drawing
     * @param colorSwatch   the associated color swatch data (unused)
     */
    @Override
    public void display(PGraphics p, ColorSwatch colorSwatch) {
        p.fill(0);
        p.rect(0, 0, width, height);

        int SWATCHES = 18; // Number of swatches to display (labeled from 0.0 to 1.0)
        float thickness = (float) height / SWATCHES;

        for (int swatch = 0; swatch < SWATCHES; swatch++) {
            float c = swatch / (SWATCHES - 1.0f);
            float barTop = height - (swatch + 1) * thickness;

            p.fill(Rainbow.get(c));
            p.rect(0, barTop, width, thickness);

            p.textFont(pf);
            p.fill(fontColor);
            p.text(String.format("%4.2f", c), width + 10, barTop + thickness);
        }
    }
}
