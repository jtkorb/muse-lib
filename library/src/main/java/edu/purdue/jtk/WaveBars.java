package edu.purdue.jtk;

import controlP5.ControlP5;
import controlP5.Group;
import processing.core.PFont;

/**
 * WaveBars is a controller group for displaying wave bars associated with the four Muse sensors.
 */
class WaveBars extends Group {
    final static int BAR_GAP = 50;
    final static int LEFT_MARGIN = 0;
    final static int TOP_MARGIN = 15;

    /**
     * Creates a set of wave bars, one for each sensor.
     *
     * @param cp5           the ControlP5 object where the bars are to be drawn
     * @param pf            the font to use for strings
     * @param fontColor     the font color
     * @param s             the name of the group
     * @param muse          the muse object associated with these wave bars
     */
    WaveBars(ControlP5 cp5, PFont pf, int fontColor, String s, Muse muse) {
        super(cp5, s);

        SensorBlockGroup sgLE = new SensorBlockGroup(cp5, pf, fontColor, "Left Ear", Sensor.LEFT_EAR, muse);
        SensorBlockGroup sgRE = new SensorBlockGroup(cp5, pf, fontColor, "Right Ear", Sensor.RIGHT_EAR, muse);
        SensorBlockGroup sgLF = new SensorBlockGroup(cp5, pf, fontColor, "Left FH", Sensor.LEFT_FH, muse);
        SensorBlockGroup sgRF = new SensorBlockGroup(cp5, pf, fontColor, "Right FH", Sensor.RIGHT_FH, muse);
        ColorSwatch cs = new ColorSwatch(cp5, pf, fontColor,"Color Swatch");

        int widthBlock = sgLE.getWidth();
        int heightBlock = sgLE.getHeight();

        sgLE.setPosition(LEFT_MARGIN, TOP_MARGIN + heightBlock + BAR_GAP).setGroup(s);
        sgRE.setPosition(LEFT_MARGIN + widthBlock + BAR_GAP / 4, TOP_MARGIN + heightBlock + BAR_GAP).setGroup(s);
        sgLF.setPosition(LEFT_MARGIN, TOP_MARGIN).setGroup(s);
        sgRF.setPosition(LEFT_MARGIN + widthBlock + BAR_GAP / 4, TOP_MARGIN).setGroup(s);

        cs.setPosition(LEFT_MARGIN + 2 * widthBlock + BAR_GAP / 2, TOP_MARGIN + 5).setGroup(s);
    }
}
