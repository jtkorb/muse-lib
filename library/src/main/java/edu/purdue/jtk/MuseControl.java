package edu.purdue.jtk;

import controlP5.Button;
import controlP5.CheckBox;
import controlP5.ControlEvent;
import controlP5.ControlP5;
import controlP5.ControllerStyle;
import controlP5.Label;
import controlP5.RadioButton;
import controlP5.Slider;
import controlP5.Textarea;
import controlP5.Textlabel;
import controlP5.Toggle;
import processing.core.PApplet;
import processing.core.PFont;

/**
 * MuseControl is a class that creates a control panel to manage the Muse/View
 * interaction. It also monitors the raw data coming from the Muse.
 */
public class MuseControl extends PApplet {
    private ControlP5 cp5 = null;

    private CheckBox cbWaveChooser, cbSensorChooser;
    private boolean[] enableWaves, enableSensors;

    private Toggle toggleFocus, toggleActivity, toggleSmoothing, toggleUpscaling;
    private boolean showFocus, showActivity;

    private Slider scaleSlider;
    private int scaleValue;

    private RadioButton source;
    private Muse muse;

    private boolean testing = false;

    Textlabel ipAddress;

    // Be sure the colors for background and fonts have adequate contrast...
    private final int BACKGROUND_COLOR = 0;
    private final int FONT_COLOR = 255;

    private int TOP_MARGIN = 30;
    private int LEFT_MARGIN = 10;
    private final String FONT_NAME = "SansSerif";
    private final int FONT_SIZE = 14;
    private final int SCALE_MAX = 1000;

    // Dependent on font size and label strings
    private final int LABEL_FUDGE = -3;

    PFont pf;

    protected MuseControl(Muse muse) {
        this(muse, false);
    }

    protected MuseControl(Muse muse, boolean testing) {
        super();
        this.muse = muse;
        this.testing = testing;
        if (!testing)
            PApplet.runSketch(new String[] { this.getClass().getSimpleName() }, this);

        enableWaves = new boolean[Wave.values().length];
        for (Wave wave : Wave.values())
            enableWaves[wave.value] = true;

        enableSensors = new boolean[Sensor.values().length];
        for (Sensor sensor : Sensor.values())
            enableSensors[sensor.value] = true;

        showFocus = true;
        showActivity = true;
        muse.model.setDoSmoothing(true);
    }

    @Override
    public void settings() {
        int display = 1;
        fullScreen(display);
        // size(900, 700);
        System.out.format("Control panel on display %d\n", display);
    }

    @Override
    public void setup() {
        TOP_MARGIN = TOP_MARGIN + (height - 650) / 2;
        LEFT_MARGIN = LEFT_MARGIN + (width - 900) / 2;

        frameRate(30);
        SensorBlockGroup sg;
        cp5 = new ControlP5(this);
        pf = createFont(FONT_NAME, FONT_SIZE, true);

        // Default Group
        addSource(cp5, LEFT_MARGIN + 28, TOP_MARGIN);
        addWaveBars(cp5, LEFT_MARGIN + 28, TOP_MARGIN + 50);

        float SENSOR_MARGIN = textWidth(Sensor.RIGHT_EAR.getName());

        int BOTTOM_HALF = 410;

        addWaveChooser(cp5, (int) (LEFT_MARGIN + 28 + 10 + SENSOR_MARGIN), TOP_MARGIN + BOTTOM_HALF);
        addSensorChooser(cp5, LEFT_MARGIN + 270, TOP_MARGIN + BOTTOM_HALF);

        int X_TOGGLES = LEFT_MARGIN + 464 - 10, Y_TOGGLES = BOTTOM_HALF - 4;
        int SEPARATION = 24;
        cp5.addLabel("Features")
                .setPosition(X_TOGGLES - 3, TOP_MARGIN + Y_TOGGLES)
                .setFont(pf)
                .setColor(FONT_COLOR);
        addToggleFocus(cp5, X_TOGGLES, TOP_MARGIN + Y_TOGGLES + SEPARATION);
        addToggleActivity(cp5, X_TOGGLES, TOP_MARGIN + Y_TOGGLES + 2 * SEPARATION);
        addToggleSmoothing(cp5, X_TOGGLES, TOP_MARGIN + Y_TOGGLES + 3 * SEPARATION);
        addToggleUpscaling(cp5, X_TOGGLES, TOP_MARGIN + Y_TOGGLES + 4 * SEPARATION);

        int BOTTOM_BARS = 590;

        addScaleSlider(cp5, (int) (LEFT_MARGIN + 28 + 10 + SENSOR_MARGIN), TOP_MARGIN + BOTTOM_BARS);
        addResetButton(cp5, X_TOGGLES, TOP_MARGIN + BOTTOM_BARS);

        addShowStats(cp5, width / 2 - 20, height - 25);

        // Console Group
        addConsole(cp5, LEFT_MARGIN, TOP_MARGIN, width - 2 * LEFT_MARGIN, height - 2 * TOP_MARGIN);

        // Test Group
        int dy = 0;
        for (int i = 0; i < 15; i++) {
            PFont pf1 = createFont(FONT_NAME, 10 + i, true);
            cp5.addTextlabel(String.format("test1%02d", i))
                    .setFont(pf1)
                    .setText(String.format("Test YyYyYy %d: %s; size = %d, smooth = %b", i, pf1.getName(),
                            pf1.getSize(), pf1.isSmooth()))
                    .setPosition(LEFT_MARGIN, TOP_MARGIN + dy)
                    .moveTo("Test")
                    .setColor(0);
            dy += pf1.getSize();

            PFont pf2 = createFont(FONT_NAME, 10 + i, false);
            cp5.addTextlabel(String.format("test2%02d", i))
                    .setFont(pf2)
                    .setText(String.format("Test YyYyYy %d: %s; size = %d, smooth = %b", i, pf2.getName(),
                            pf2.getSize(), pf2.isSmooth()))
                    .setPosition(LEFT_MARGIN, TOP_MARGIN + dy)
                    .moveTo("Test");
            dy += pf2.getSize();
        }

        // Global Group
        cp5.addFrameRate().setInterval(10).setPosition(width - LEFT_MARGIN - 20, height - 20).moveTo("global");

        // Let any processes know we're ready to roll...
        synchronized (this) {
            starting = false;
            notifyAll();
        }
    }

    int i = 0;

    @Override
    public void draw() {
        background(BACKGROUND_COLOR);
        // if (frameCount == 1)
        // surface.setLocation(displayWidth - width, displayHeight - height - 50);
    }

    @Override
    public void dispose() {
        // Add anything here that needs to be cleaned up.
    }

    public void keyPressed(char key, int keyCode) {
        this.key = key;
        this.keyCode = keyCode;
        keyPressed();
    }

    private KeyCallback kc = null;

    public void registerKeyPress(KeyCallback kc) {
        this.kc = kc;
    }

    /**
     * keyPressed() -- handle key press event from Processing. If not handled by
     * local handleKeyPress procedure, call a registered one, if available.
     */
    @Override
    public void keyPressed() {
        // Try local key handler; if that fails, call Muse app key callback handler (if
        // available)
        if (handleKeyPressed(key, keyCode))
            return;
        else if (kc != null && kc.handleKeyPressed(key, keyCode))
            return;
        System.err.printf("Key (%c, %d) to Control Panel ignored\n", key, keyCode);
    }

    public boolean handleKeyPressed(char key, int keyCode) {
        if (key == 'u')
            muse.setGenerate(Generate.Unfocused);
        else if (key == 'f')
            muse.setGenerate(Generate.Focused);
        else if (key == 'c')
            muse.setGenerate(Generate.Calm);
        else if (key == CODED && keyCode == UP)
            muse.setGenerate(Generate.FrontBrain);
        else if (key == CODED && keyCode == DOWN)
            muse.setGenerate(Generate.RearBrain);
        else if (key == CODED && keyCode == LEFT)
            muse.setGenerate(Generate.LeftBrain);
        else if (key == CODED && keyCode == RIGHT)
            muse.setGenerate(Generate.RightBrain);
        else if (key == ']')
            muse.setGenerate(Generate.MaxRight);
        else if (key == '[')
            muse.setGenerate(Generate.Zero);
        else if (key == 'p')
            muse.setPaused(!muse.isPaused());
        else if (key == 'w')
            muse.setGenerate(Generate.Winner);
        else if (key == 'l')
            muse.setGenerate(Generate.Loser);
        else
            return false;
        return true;
    }

    /*
     * THE INTERFACE ADDERS
     */
    private void addSource(ControlP5 p5, int x, int y) {
        Textlabel lb = cp5.addLabel("Source:", x, y + LABEL_FUDGE).setFont(pf).setColor(FONT_COLOR);
        ipAddress = cp5.addLabel("ipAddress")
                .setPosition(x + 400, y + LABEL_FUDGE)
                .setFont(pf)
                .setColor(FONT_COLOR)
                .setValue("");

        source = cp5.addRadioButton("source")
                .setPosition(x + 64, y)
                .setSize(15, 15)
                .setColorForeground(color(255, 255, 0))
                .setColorActive(color(0, 255, 0))
                .setColorLabels(FONT_COLOR)
                .setItemsPerRow(3)
                .setSpacingColumn(105)
                .setNoneSelectedAllowed(true)
                .addItem("Headband", 0)
                .addItem("Generator", 1)
                .addItem("File", 2);

        for (Toggle t : source.getItems()) {
            Label cl = t.getCaptionLabel();
            cl.setFont(pf).setColor(FONT_COLOR).toUpperCase(false);

            // NB: Needed...?
            ControllerStyle s = cl.getStyle();
            s.setMargin(-7, 0, 0, -3);
            s.movePadding(7, 0, 0, 3);
            s.backgroundWidth = 50;
            s.backgroundHeight = 30;
        }
    }

    private void addToggleUpscaling(ControlP5 cp5, int x, int y) {
        Textlabel lb = cp5.addLabel("Upscaling", x + 18, y + LABEL_FUDGE).setFont(pf).setColor(FONT_COLOR);
        toggleUpscaling = cp5.addToggle("toggleUpscaling")
                .setPosition(x, y)
                .setColorForeground(color(255, 255, 0))
                .setColorActive(color(0, 255, 0))
                .setCaptionLabel("")
                .setSize(15, 15)
                .setState(muse.model.getUpscaling());
    }

    private void addToggleActivity(ControlP5 cp5, int x, int y) {
        Textlabel lb = cp5.addLabel("Activity", x + 18, y + LABEL_FUDGE).setFont(pf).setColor(FONT_COLOR);
        toggleActivity = cp5.addToggle("toggleActivity")
                .setPosition(x, y)
                .setColorForeground(color(255, 255, 0))
                .setColorActive(color(0, 255, 0))
                .setCaptionLabel("")
                .setSize(15, 15)
                .setState(showActivity);
    }

    private void addToggleSmoothing(ControlP5 cp5, int x, int y) {
        Textlabel lb = cp5.addLabel("Smoothing", x + 18, y + +LABEL_FUDGE).setFont(pf).setColor(FONT_COLOR);
        toggleSmoothing = cp5.addToggle("toggleSmoothing")
                .setPosition(x, y)
                .setColorForeground(color(255, 255, 0))
                .setColorActive(color(0, 255, 0))
                .setCaptionLabel("")
                .setSize(15, 15)
                .setState(muse.model.getDoSmoothing());
    }

    private void addToggleFocus(ControlP5 cp5, int x, int y) {
        Textlabel lb = cp5.addLabel("Focus", x + 18, y + LABEL_FUDGE).setFont(pf).setColor(FONT_COLOR);
        toggleFocus = cp5.addToggle("toggleFocus")
                .setPosition(x, y)
                .setColorForeground(color(255, 255, 0))
                .setColorActive(color(0, 255, 0))
                .setCaptionLabel("")
                .setSize(15, 15)
                .setState(showFocus);
    }

    private void addWaveChooser(ControlP5 cp5, int x, int y) {
        Textlabel lb = cp5.addLabel("Active Waves", x - 4, y + LABEL_FUDGE).setFont(pf).setColor(FONT_COLOR);
        cbWaveChooser = cp5.addCheckBox("waveChooser")
                .setPosition(x, y + 20)
                .setColorForeground(color(255, 255, 0))
                .setColorActive(color(0, 255, 0))
                .setColorLabel(color(0, 0, 0))
                .setSize(15, 15)
                .setItemsPerRow(1)
                .setSpacingRow(10);

        for (Wave wave : Wave.values()) {
            String name = "+" + wave.getName();
            cbWaveChooser.addItem(name, wave.getValue());

            if (enableWaves[wave.getValue()])
                cbWaveChooser.activate(name);

            Toggle toggle = cbWaveChooser.getItem(wave.getValue());
            toggle.setCaptionLabel(wave.getName());
            toggle.getCaptionLabel().setFont(pf).setColor(FONT_COLOR).toUpperCase(false);
        }
    }

    private void addSensorChooser(ControlP5 cp5, int x, int y) {
        Textlabel lb = cp5.addLabel("Active Sensors", x - 4, y + LABEL_FUDGE).setFont(pf).setColor(FONT_COLOR);
        cbSensorChooser = cp5.addCheckBox("sensorChooser")
                .setPosition(x, y + 20)
                .setColorForeground(color(255, 255, 0))
                .setColorActive(color(0, 255, 0))
                .setColorLabel(color(0, 0, 0))
                .setSize(15, 15)
                .setItemsPerRow(1)
                .setSpacingRow(10);

        for (Sensor sensor : Sensor.values()) {
            String name = "+" + sensor.getName();
            cbSensorChooser.addItem(name, sensor.getValue());

            if (enableSensors[sensor.getValue()])
                cbSensorChooser.activate(name);

            Toggle toggle = cbSensorChooser.getItem(sensor.getValue());
            toggle.setCaptionLabel(sensor.getName());
            toggle.getCaptionLabel().setFont(pf).setColor(FONT_COLOR).toUpperCase(false);
        }
    }

    private void addWaveBars(ControlP5 cp5, int x, int y) {
        WaveBars wb = new WaveBars(cp5, pf, FONT_COLOR, "waveBars", muse);
        wb.setPosition(x, y);
    }

    private void addConsole(ControlP5 cp5, int x, int y, int width, int height) {
        Textarea myTextarea = cp5.addTextarea("txt")
                .setPosition(x, y)
                .setSize(width, height)
                .setColor(0)
                .setLineHeight(15)
                .setFont(createFont("monospaced", 12, true))
                .setBorderColor(color(0))
                .setColorBackground(color(200))
                .moveTo("Console");

        // cp5.addConsole(myTextarea);
    }

    private void addResetButton(ControlP5 cp5, int x, int y) {
        Button button = cp5.addButton("reset")
                .setPosition(x, y)
                .setCaptionLabel("Reset")
                .setWidth(150);
        button.getCaptionLabel().setFont(pf).setColor(FONT_COLOR).toUpperCase(false);

    }

    private void addShowStats(ControlP5 cp5, int x, int y) {
        cp5.addButton("showStats")
                .setPosition(x, y)
                .setCaptionLabel("Show Statistics")
                .moveTo("Console");
    }

    private void addScaleSlider(ControlP5 cp5, int x, int y) {
        scaleSlider = cp5.addSlider("scaleValue")
                .setCaptionLabel("Wave Scaling (%)")
                .setPosition(x, y)
                .setHeight(20)
                .setWidth(200)
                .setRange(0, SCALE_MAX)
                .setValue(100);
        // .moveTo("global");
        scaleSlider.getCaptionLabel().setFont(pf).setColor(FONT_COLOR).toUpperCase(false);
    }

    /*
     * THE CONTROLLERS
     */
    public void controlEvent(ControlEvent event) {
        if (event.isFrom(cbWaveChooser)) {
            float[] values = cbWaveChooser.getArrayValue();
            for (int i = 0; i < values.length; i++) {
                enableWaves[i] = values[i] == 1;
            }
        } else if (event.isFrom(cbSensorChooser)) {
            float[] values = cbSensorChooser.getArrayValue();
            for (int i = 0; i < values.length; i++) {
                enableSensors[i] = values[i] == 1;
            }
        } else if (event.isFrom(toggleFocus)) {
            showFocus = toggleFocus.getState();
        } else if (event.isFrom(toggleActivity)) {
            showActivity = toggleActivity.getState();
        } else if (event.isFrom(toggleUpscaling)) {
            muse.model.setUpscaling(toggleUpscaling.getState());
        } else if (event.isFrom(toggleSmoothing)) {
            muse.model.setDoSmoothing(toggleSmoothing.getState());
        } else if (event.isFrom(scaleSlider)) {
            scaleValue = (int) scaleSlider.getValue();
        } else if (event.isFrom(source)) {
            ipAddress.setValue("");
            switch ((int) source.getValue()) {
                case -1:
                    muse.clearSource();
                    break;
                case 0:
                    muse.setHeadbandSource();
                    ipAddress.setValue(((MuseListener) (muse.source)).getIPAddress());
                    break;
                case 1:
                    muse.setGeneratorSource();
                    break;
                case 2:
                    muse.setFileSource();
                    break;
                default:
                    assert false;
            }
        }
    }

    public void showStats(int value) {
        muse.ms.printStats();
    }

    public void reset() {
        muse.model.reset();
        setScaleValue(300);
    }

    public void waveChooser(float[] a) {
        // Nothing needs to be done.
        // System.out.format("waveChooser called\n");
    }

    public void sensorChooser(float[] a) {
        // Nothing needs to be done.
        // System.out.format("sensorChooser called\n");
    }

    /*
     * THE GETTERS
     */

    //@formatter:off
    public int getScaleValue() {
        // return (int) scaleSlider.getValue();
        return scaleValue;
    }

    public boolean getShowFocus() {
        return toggleFocus.getBooleanValue();
    }

    public boolean getShowActivity() { return toggleActivity.getBooleanValue(); }

    public boolean getShowWave(Wave wave) {
        return enableWaves[wave.getValue()];
    }

    public boolean getShowSensor(Sensor sensor) {
        return enableSensors[sensor.getValue()];
    }

    public boolean getDoSmoothing() { return toggleSmoothing.getBooleanValue(); }

    public boolean getUpscaling() { return toggleUpscaling.getBooleanValue(); }

    public int getScaleMax() { return SCALE_MAX; }
    //@formatting:on

    /*
     * THE SETTERS
     */
    public void setScaleValue(float value) {
        scaleSlider.setValue(value);
    }

    public void setShowWave(Wave wave, boolean value) {
        if (testing)
            enableWaves[wave.getValue()] = value;
        else {
            if (value)
                cbWaveChooser.activate(wave.getValue());
            else
                cbWaveChooser.deactivate(wave.getValue());
        }
    }

    public void setShowSensor(Sensor sensor, boolean value) {
        if (testing)
            enableSensors[sensor.getValue()] = value;
        else {
            if (value)
                cbSensorChooser.activate(sensor.getValue());
            else
                cbSensorChooser.deactivate(sensor.getValue());
        }
    }

    public void setDoSmoothing(boolean value) { toggleSmoothing.setValue(value); }

    public void setUpscaling(boolean value) { toggleUpscaling.setValue(value); }

    /*
     * Handle startup synchronization.
     */
    private Boolean starting = true;

    void waitForStartup() {
        if (testing)
            return;

        synchronized (this) {
            while (starting)
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
    }
}
