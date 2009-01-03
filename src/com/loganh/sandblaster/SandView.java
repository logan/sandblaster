package com.loganh.sandblaster;

import java.io.*;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;

public class SandView extends SurfaceView {
  // Maximum FPS.
  static final private float FPS = 20;

  // Sorted from "biggest" (most coarse) to "smallest" (most granular).
  static final private float[] SCALES = { 1f / 16, 1f / 8, 1f / 4, 1f / 2, 1f };

  // Index of the scale to default to.
  static final private int INITIAL_SCALE = 2;

  // If a single location is touched for this long (in ms), make it permanent.
  static final private long PEN_STICK_THRESHOLD = 500;

  public SandBox sandbox;
  private SandBoxDriver driver;
  private SandBoxRenderer renderer;
  private PaletteView palette;
  private boolean penDown;
  private long penDownTime;
  private int lastPenX;
  private int lastPenY;
  private int scaleIndex;

  public SandView(Context context, AttributeSet attrs) {
    super(context, attrs);
    scaleIndex = INITIAL_SCALE;
    renderer = new SandBoxRenderer(getHolder());
  }

  public void setPaletteView(PaletteView palette) {
    this.palette = palette;
  }

  synchronized private void startDriver() {
    stopDriver();
    if (sandbox != null) {
      driver = new SandBoxDriver(sandbox, renderer, FPS);
      driver.start();
    }
  }

  synchronized private void stopDriver() {
    if (driver != null) {
      driver.shutdown();
    }
    driver = null;
  }

  synchronized public void setSandbox(SandBox sandbox) {
    stopDriver();
    this.sandbox = sandbox;
  }

  public void resume() {
    startDriver();
  }

  public void pause() {
    stopDriver();
    if (sandbox != null) {
      Snapshot snapshot = new Snapshot(sandbox);
      snapshot.name = "Autosave";
      try {
        snapshot.save(getContext());
      } catch (IOException ex) {
        Log.e("Failed to autosave snapshot", ex);
      }
    }
  }

  synchronized public void clear() {
    if (sandbox != null) {
      sandbox.clear();
    }
  }

  public boolean makeBigger() {
    if (scaleIndex > 0) {
      scaleIndex--;
      resetSandbox();
    }
    return scaleIndex > 0;
  }

  public boolean makeSmaller() {
    if (scaleIndex < SCALES.length - 1) {
      scaleIndex++;
      resetSandbox();
    }
    return scaleIndex < SCALES.length - 1;
  }

  synchronized private void resetSandbox() {
    float scale = SCALES[scaleIndex];
    Log.i("creating {0} x {1} sandbox", getWidth() * scale, getHeight() * scale);
    stopDriver();
    sandbox = new SandBox((int) (scale * getWidth()), (int) (scale * getHeight()));
    startDriver();
  }

  @Override
  public void onLayout(boolean changed, int left, int top, int right, int bottom) {
    if (sandbox == null) {
      try {
        Snapshot snapshot = new Snapshot("Autosave", getContext());
        setSandbox(snapshot.sandbox);
      } catch (IOException ex) {
        Log.e("unable to load Autosave", ex);
        installDemo();
      }
    }
  }

  private int scaleX(float x) {
    return Math.round(sandbox.getWidth() * x / getWidth());
  }

  private int scaleY(float y) {
    return Math.round(sandbox.getHeight() - sandbox.getHeight() * y / getHeight() - 1);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (sandbox == null) {
      return false;
    }
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      // TODO: line drawing
      // TODO: pressure sensitivity
      penDown = true;
      penDownTime = SystemClock.uptimeMillis();
      lastPenX = scaleX(event.getX());
      lastPenY = scaleY(event.getY());
      sandbox.addSource(palette.getElement(), lastPenX, lastPenY);
      return true;
    } else if (penDown && event.getAction() == MotionEvent.ACTION_MOVE) {
      int newPenX = scaleX(event.getX());
      int newPenY = scaleY(event.getY());
      if (newPenX == lastPenX && newPenY == lastPenY) {
        return true;
      }
      penDownTime = SystemClock.uptimeMillis();
      sandbox.removeSource(lastPenX, lastPenY);
      sandbox.line(palette.getElement(), lastPenX, lastPenY, newPenX, newPenY);
      lastPenX = newPenX;
      lastPenY = newPenY;
      sandbox.addSource(palette.getElement(), lastPenX, lastPenY);
      return true;
    } else if (penDown && event.getAction() == MotionEvent.ACTION_UP) {
      Log.i("pen was down for {0} ms", SystemClock.uptimeMillis() - penDownTime);
      if (SystemClock.uptimeMillis() - penDownTime < PEN_STICK_THRESHOLD) {
        sandbox.removeSource(lastPenX, lastPenY);
      }
      penDown = false;
      return true;
    }
    return false;
  }

  synchronized public void installDemo() {
    startDriver();
    clear();
    if (sandbox != null) {
      Demo.install(sandbox);
    }
  }
}
