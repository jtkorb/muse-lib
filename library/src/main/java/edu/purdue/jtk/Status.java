package edu.purdue.jtk;

import controlP5.ControlP5;
import controlP5.Controller;
import controlP5.ControllerView;
import processing.core.PGraphics;
import processing.core.PImage;

public class Status extends Controller<Status> {
    PImage base;

    public Status(ControlP5 controlP5, String s, Muse muse) {
        super(controlP5, s);

        base = cp5.papplet.loadImage("images/status-base.png");

        setView(new StatusView(muse, this));
    }
}

class StatusView implements ControllerView<Status> {
    private Muse muse;
    private Status status;

    public StatusView(Muse muse, Status status) {
        this.muse = muse;
        this.status = status;
    }

    @Override
    public void display(PGraphics p, Status status) {
        p.pushMatrix();
//        p.scale(0.20f);
        p.image(status.base, 0, 0);
        p.popMatrix();
    }
}
