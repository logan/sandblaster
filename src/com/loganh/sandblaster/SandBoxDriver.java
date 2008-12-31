package com.loganh.sandblaster;

import android.os.Debug;
import android.os.SystemClock;
import android.view.SurfaceHolder;

public class SandBoxDriver extends Thread {

  private SandBox sandbox;
  private SandBoxRenderer renderer;
  private float fps;
  private boolean stopped;

  public SandBoxDriver(SandBox sandbox, SandBoxRenderer renderer, float fps) {
    this.sandbox = sandbox;
    this.renderer = renderer;
    this.fps = fps;
  }

  @Override
  public void run() {
    long lastUpdate = 0;
    long dtms = (int) (1000 / fps);
    //Debug.startMethodTracing("update");
    while (!stopped) {
      long now = SystemClock.uptimeMillis();
      long remaining = lastUpdate + dtms - now;
      if (remaining <= 0) {
        computePhysics();
        draw();
        lastUpdate = now;
      } else {
        SystemClock.sleep(remaining);
      }
    }
    //Debug.stopMethodTracing();
  }

  public void shutdown() {
    stopped = true;
    try {
      join();
    } catch (InterruptedException ex) {
      Log.e("InterruptedException thrown on SandBoxDriver.join", ex);
    }
  }

  private void computePhysics() {
    sandbox.update();
  }

  private void draw() {
    renderer.draw(sandbox);
  }
}
