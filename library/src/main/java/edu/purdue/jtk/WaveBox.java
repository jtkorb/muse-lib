package edu.purdue.jtk;

import controlP5.ControlP5;
import controlP5.Controller;
import controlP5.ControllerView;
import processing.core.PFont;
import processing.core.PGraphics;

class WaveBox extends Controller<WaveBox> {
    Muse muse;

    WaveBox(ControlP5 controlP5, PFont pf, int fontColor, String s, Muse muse, Wave wave, Sensor sensor) {
        super(controlP5, s);
        this.muse = muse;
        setView(new WaveBoxView(this, pf, fontColor, wave, sensor));
    }
}

class WaveBoxView implements ControllerView<WaveBox> {
    private WaveBox waveBox;
    private PFont pf;
    private int fontColor;
    private Wave wave;
    private Sensor sensor;

    WaveBoxView(WaveBox waveBox, PFont pf, int fontColor, Wave wave, Sensor sensor) {
        this.waveBox = waveBox;
        this.pf = pf;
        this.fontColor = fontColor;
        this.wave = wave;
        this.sensor = sensor;
    }

    @Override
    public void display(PGraphics p, WaveBox b) {
        int MARGIN = 10;
        int LINE_SEP = 3;
        float value = waveBox.muse.model.getTouchingForehead() ? waveBox.muse.getGrid(wave, sensor) : 0;
        float textHeight = p.textAscent() + p.textDescent();
        float width = waveBox.getWidth();
        float height = waveBox.getHeight() - 2 * textHeight - 5;
        float barTop  = height * (1 - value);

        // Draw full wave box...
        p.fill(50);
        p.rect(MARGIN, 0, width - 2 * MARGIN, height);

        // Draw bottom portion of wave box with color and height...
        p.fill(Rainbow.get(value));
        p.rect(MARGIN, barTop, width - 2 * MARGIN, height - barTop);

        // Draw a marker line along the top of the colored bar...
        p.stroke(0);
        p.line(0, barTop, width, barTop);

        // Add the wave value and name to the bottom...
        p.fill(0);
        p.textFont(pf);
        p.fill(fontColor);
        p.text(String.format("%5.3f", value), MARGIN, height + textHeight);
        p.text(waveBox.getLabel(), MARGIN, height + 2 * textHeight + LINE_SEP);
    }
}
