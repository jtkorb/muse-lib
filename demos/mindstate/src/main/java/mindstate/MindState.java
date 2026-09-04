package mindstate;

import edu.purdue.jtk.Muse;
import edu.purdue.jtk.MuseControl;
import edu.purdue.jtk.Rainbow;
import processing.core.PApplet;

/**
 * Fullscreen thinking-vs-relaxing biofeedback. The marker moves toward Relaxing
 * when posterior alpha rises (eyes closed) and toward Thinking when beta/gamma
 * rise (mental effort).
 */
public class MindState extends PApplet {
    private Muse muse;
    private MuseControl mc;

    public void settings() {
        int display = 1;
        int screenCount = 1;
        try {
            screenCount = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length;
            if (screenCount >= 2) {
                display = 2;
            }
        } catch (Throwable ignored) {
            display = 1;
        }
        if (screenCount >= 2) {
            fullScreen(display);
        } else {
            // On one monitor the control panel already uses the whole screen; keep this
            // sketch windowed so the thinking/relaxing marker stays visible.
            size(1600, 900);
        }
        System.out.printf("MindState on screen %d of %d (%d x %d)\n", display, screenCount, displayWidth, displayHeight);
    }

    public void setup() {
        frameRate(30);
        surface.setTitle("MindState — Thinking vs Relaxing");
        muse = new Muse();
        mc = muse.getMuseControl();
        textAlign(CENTER, CENTER);
    }

    public void keyPressed() {
        mc.keyPressed(key, keyCode);
    }

    public void draw() {
        if (muse.isPaused() || muse.noSource()) {
            background(12, 16, 24);
            fill(230);
            textSize(titleSize());
            text(muse.noSource() ? "Select Headband or Generator" : "Paused", width / 2f, height / 2f - 40);
            textSize(bodySize());
            fill(180);
            text("Generator rehearsal: press R for Relaxing, T for Thinking", width / 2f, height / 2f + 30);
            return;
        }

        float relaxation = muse.computeRelaxation();
        boolean reliable = muse.isRelaxationReliable();
        float alpha = muse.getRelativeAlpha();
        float beta = muse.getRelativeBeta();

        int bg = lerpColor(color(150, 36, 12), color(12, 48, 120), relaxation);
        if (!reliable) {
            bg = lerpColor(bg, color(24, 24, 28), 0.55f);
        }
        background(bg);

        drawGauge(relaxation, reliable);
        drawBandBars(alpha, beta);
        drawProtocol(reliable);
        drawBanner(reliable);
    }

    private void drawGauge(float relaxation, boolean reliable) {
        float left = width * 0.12f;
        float right = width * 0.88f;
        float y = height * 0.42f;
        float trackH = height * 0.045f;

        noStroke();
        fill(0, 90);
        rect(left, y - trackH / 2f, right - left, trackH, trackH / 2f);

        // Warm-to-cool fill under the marker so the direction is obvious.
        for (int i = 0; i < 40; i++) {
            float t = i / 39f;
            float x0 = lerp(left, right, t);
            float x1 = lerp(left, right, (i + 1) / 39f);
            fill(Rainbow.get(1f - t), reliable ? 180 : 90);
            rect(x0, y - trackH / 2f, x1 - x0 + 1, trackH);
        }

        float markerX = lerp(left, right, relaxation);
        float diameter = height * 0.22f;
        int markerColor = Rainbow.get(1f - relaxation);
        fill(markerColor, reliable ? 255 : 140);
        stroke(255, reliable ? 220 : 100);
        strokeWeight(6);
        circle(markerX, y, diameter);

        fill(0, reliable ? 220 : 140);
        noStroke();
        textSize(labelSize());
        text(relaxation < 0.5f ? "THINKING" : "RELAXING", markerX, y);

        textSize(bodySize());
        fill(255);
        text("THINKING", left, y + trackH + diameter * 0.42f);
        text("RELAXING", right, y + trackH + diameter * 0.42f);
    }

    private void drawBandBars(float alpha, float beta) {
        float barLeft = width * 0.12f;
        float barWidth = width * 0.28f;
        float barH = height * 0.04f;
        float alphaY = height * 0.68f;
        float betaY = alphaY + barH * 2.1f;

        textAlign(LEFT, CENTER);
        textSize(bodySize());
        fill(255);
        text("Alpha  " + nfc(alpha, 2), barLeft, alphaY - barH * 0.85f);
        text("Beta   " + nfc(beta, 2), barLeft, betaY - barH * 0.85f);

        noStroke();
        fill(0, 80);
        rect(barLeft, alphaY, barWidth, barH, 8);
        rect(barLeft, betaY, barWidth, barH, 8);
        fill(Rainbow.get(0.15f));
        rect(barLeft, alphaY, barWidth * constrain(alpha, 0, 1), barH, 8);
        fill(Rainbow.get(0.85f));
        rect(barLeft, betaY, barWidth * constrain(beta, 0, 1), barH, 8);
        textAlign(CENTER, CENTER);
    }

    private void drawProtocol(boolean reliable) {
        textAlign(LEFT, TOP);
        textSize(bodySize());
        fill(255, reliable ? 235 : 180);
        float x = width * 0.48f;
        float y = height * 0.66f;
        text("Watch the marker, then try:", x, y);
        text("1. Close your eyes and breathe slowly for 20 seconds.", x, y + lineHeight());
        text("    Alpha should rise; the marker drifts toward Relaxing.", x, y + 2 * lineHeight());
        text("2. Open your eyes and subtract 7 from 100, then 93, 86...", x, y + 3 * lineHeight());
        text("    Beta/gamma should rise; the marker drifts toward Thinking.", x, y + 4 * lineHeight());
        textAlign(CENTER, CENTER);
    }

    private void drawBanner(boolean reliable) {
        if (reliable) {
            return;
        }
        noStroke();
        fill(40, 10, 10, 210);
        rect(0, 0, width, height * 0.11f);
        fill(255, 210, 180);
        textSize(bodySize());
        text("Fit the headband and wait for green contact. Jaw clench and blinks are muscle, not mind.",
                width / 2f, height * 0.055f);
    }

    private float titleSize() {
        return max(28, width * 0.032f);
    }

    private float labelSize() {
        return max(18, width * 0.018f);
    }

    private float bodySize() {
        return max(16, width * 0.014f);
    }

    private float lineHeight() {
        return bodySize() * 1.45f;
    }

    public static void main(String[] args) {
        PApplet.main(new String[] { "mindstate.MindState" });
    }
}
