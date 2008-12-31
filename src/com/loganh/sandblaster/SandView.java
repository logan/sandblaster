package com.loganh.sandblaster;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;

public class SandView extends SurfaceView {
  // Maximum FPS.
  static final private float FPS = 20;

  // Sorted from "biggest" (most coarse) to "smallest" (most granular).
  static final private float[] SCALES = { 1f / 16, 1f / 8, 1f / 4 };

  // Index of the scale to default to.
  static final private int INITIAL_SCALE = 1;

  // If a single location is touched for this long (in ms), make it permanent.
  static final private long PEN_STICK_THRESHOLD = 2000;

  private SandBox sandbox;
  private SandBoxDriver driver;
  private SandBoxRenderer renderer;
  private PaletteView palette;
  private boolean penDown;
  private long penDownTime;
  private float lastPenX;
  private float lastPenY;
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

  public void resume() {
    startDriver();
  }

  public void pause() {
    stopDriver();
  }

  synchronized public void clear() {
    if (sandbox != null) {
      sandbox.clear();
    }
  }

  public void makeBigger() {
    if (scaleIndex > 0) {
      scaleIndex--;
      resetSandbox();
    }
  }

  public void makeSmaller() {
    if (scaleIndex < SCALES.length - 1) {
      scaleIndex++;
      resetSandbox();
    }
  }

  synchronized private void resetSandbox() {
    float scale = SCALES[scaleIndex];
    Log.i("creating {0} x {1} sandbox", getWidth() * scale, getHeight() * scale);
    stopDriver();
    sandbox = new SandBox((int) getWidth(), (int) getHeight(), scale);
    startDriver();
  }

  @Override
  public void onLayout(boolean changed, int left, int top, int right, int bottom) {
    if (sandbox == null) {
      resetSandbox();
      installDemo();
    }
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
      lastPenX = event.getX();
      lastPenY = event.getY();
      sandbox.addSource(palette.getElement(), sandbox.fromCanvasX(lastPenX), sandbox.fromCanvasY(lastPenY));
      return true;
    } else if (penDown && event.getAction() == MotionEvent.ACTION_MOVE) {
      penDownTime = SystemClock.uptimeMillis();
      sandbox.removeSource(sandbox.fromCanvasX(lastPenX), sandbox.fromCanvasY(lastPenY));
      lastPenX = event.getX();
      lastPenY = event.getY();
      sandbox.addSource(palette.getElement(), sandbox.fromCanvasX(lastPenX), sandbox.fromCanvasY(lastPenY));
      return true;
    } else if (penDown && event.getAction() == MotionEvent.ACTION_UP) {
      if (SystemClock.uptimeMillis() - penDownTime < PEN_STICK_THRESHOLD) {
        sandbox.removeSource(sandbox.fromCanvasX(lastPenX), sandbox.fromCanvasY(lastPenY));
      }
      lastPenX = event.getX();
      lastPenY = event.getY();
      penDown = false;
      return true;
    }
    return false;
  }

  synchronized public void installDemo() {
    startDriver();
    clear();
    Demo.install(sandbox);
  }
}
