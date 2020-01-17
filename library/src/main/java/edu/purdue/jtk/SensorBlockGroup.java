package edu.purdue.jtk;

import controlP5.Canvas;
import controlP5.ControlP5;
import controlP5.Group;
import processing.core.PFont;
import processing.core.PGraphics;


class SensorBlockGroup extends Group {
    Muse muse;
    Sensor sensor;

    int BAR_SEP = 0;
    int BAR_HEIGHT = 135;
    int BAR_WIDTH = 60;
    float SENSOR_MARGIN = cp5.papplet.textWidth(Sensor.RIGHT_EAR.getName());

    SensorBlockGroup(ControlP5 controlP5, PFont pf, int fontColor, String s, Sensor sensor, Muse muse) {
        super(controlP5, s);
        this.muse = muse;
        this.sensor = sensor;

        addCanvas(new SensorBlockCanvas(this, BAR_HEIGHT));

        for (int i = 0; i < Wave.values().length; i++) {
            Wave wave;

            if (sensor == Sensor.RIGHT_FH || sensor == Sensor.RIGHT_EAR)
                wave = Wave.values()[Wave.values().length - i - 1];
            else
                wave = Wave.values()[i];

            WaveBox wb = new WaveBox(cp5, pf, fontColor, s + "-" + wave.getName(), muse, wave, sensor)
                    .setCaptionLabel(wave.getName())
                    .setSize(BAR_WIDTH, BAR_HEIGHT)
                    .setPosition(SENSOR_MARGIN + i * (BAR_WIDTH + BAR_SEP),5)
                    .setGroup(this);
        }
    }

    public int getWidth() { return (int) SENSOR_MARGIN + Wave.values().length * (BAR_WIDTH + BAR_SEP) - BAR_SEP; }
    public int getHeight() { return BAR_HEIGHT; }
}

class SensorBlockCanvas extends Canvas {
    private SensorBlockGroup sbg;
    private int blockHeight;

    SensorBlockCanvas(SensorBlockGroup sbg, int blockHeight) {
        this.sbg = sbg;
        this.blockHeight = blockHeight;
    }

    @Override
    public void draw(PGraphics p) {
        float horseshoe = sbg.muse.model.getHorseshoe(sbg.sensor);
        int isGood = sbg.muse.model.isGood(sbg.sensor);
        float textHeight = p.textAscent() + p.textDescent();
        float x = 0;
        float y = blockHeight / 2.0f - 3.0f * textHeight;
        float eps = 0.2f * textHeight;

        p.strokeWeight(1f);
        p.stroke(0);

        int shoeColor = 0;
        int comboColor = 0;
        switch ((int) horseshoe) {
            case 1: shoeColor = 0xFF00FF00; comboColor = isGood == 1 ? shoeColor : 0x5500FF00; break;
            case 2: shoeColor = 0xFFFFFF00; comboColor = isGood == 1 ? shoeColor : 0xAAFFFF00; break;
            case 3: shoeColor = 0xFFFF0000; comboColor = shoeColor; break;
        }

        p.fill(sbg.muse.model.getTouchingForehead() ? comboColor : 0);
        p.rect(x, y, 40, textHeight - eps, 4);
    }
}
