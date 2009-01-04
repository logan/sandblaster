package com.loganh.sandblaster;

import android.os.SystemClock;
import android.view.SurfaceHolder;

public class SandBoxDriver extends Thread {

  private SandBox sandbox;
  private SandBoxRenderer renderer;
  private float fps;
  private boolean stopped;
  private boolean sleeping;

  public SandBoxDriver(SandBox sandbox, SandBoxRenderer renderer, float fps) {
    this.sandbox = sandbox;
    this.renderer = renderer;
    this.fps = fps;
  }

  @Override
  public void run() {
    long lastUpdate = 0;
    long dtms = (int) (1000 / fps);
    while (!stopped) {
      long now = SystemClock.uptimeMillis();
      long remaining = lastUpdate + dtms - now;
      if (remaining <= 0) {
        if (!sleeping) {
          computePhysics();
          draw();
        }
        lastUpdate = now;
      } else {
        SystemClock.sleep(remaining);
      }
    }
  }

  public void shutdown() {
    stopped = true;
    try {
      join();
    } catch (InterruptedException ex) {
      Log.e("InterruptedException thrown on SandBoxDriver.join", ex);
    }
  }

  public void sleep() {
    sleeping = true;
  }

  public void wake() {
    sleeping = false;
  }

  private void computePhysics() {
    sandbox.update();
  }

  private void draw() {
    renderer.draw(sandbox);
  }
}
