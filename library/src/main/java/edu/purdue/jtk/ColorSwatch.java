package edu.purdue.jtk;

import controlP5.ControlP5;
import controlP5.Controller;
import controlP5.ControllerView;
import processing.core.PFont;
import processing.core.PGraphics;


class ColorSwatch extends Controller<ColorSwatch> {
    final static int WIDTH = 50;
    final static int HEIGHT = 285;

    ColorSwatch(ControlP5 controlP5, PFont pf, int fontColor, String s, Muse muse) {
        super(controlP5, s);
        setView(new ColorSwatchView(muse, pf, fontColor, WIDTH, HEIGHT));
    }

    public int getWidth() { return WIDTH; }
    public int getHeight() { return HEIGHT; }

}

class ColorSwatchView implements ControllerView<ColorSwatch> {
    Muse muse;
    private PFont pf;
    private int fontColor;
    private int width;
    private int height;

    ColorSwatchView(Muse muse, PFont pf, int fontColor, int width, int height) {
        this.muse = muse;
        this.pf = pf;
        this.fontColor = fontColor;
        this.width = width;
        this.height = height;
    }

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
