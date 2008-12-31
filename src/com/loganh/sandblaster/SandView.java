package com.loganh.sandblaster;

import android.content.Context;
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

  private SandBox sandbox;
  private SandBoxDriver driver;
  private SandBoxRenderer renderer;
  private PaletteView palette;
  private boolean penDown;
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

  public void clear() {
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
    sandbox = new SandBox((int) (getWidth() * scale), (int) (getHeight() * scale));
    startDriver();
  }

  @Override
  public void onLayout(boolean changed, int left, int top, int right, int bottom) {
    if (sandbox == null) {
      resetSandbox();
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
      sprinkle(sandbox.fromCanvasX(event.getX(), getWidth()), sandbox.fromCanvasY(event.getY(), getHeight()));
      return true;
    } else if (penDown && event.getAction() == MotionEvent.ACTION_MOVE) {
      sprinkle(sandbox.fromCanvasX(event.getX(), getWidth()), sandbox.fromCanvasY(event.getY(), getHeight()));
      return true;
    } else if (event.getAction() == MotionEvent.ACTION_UP) {
      penDown = false;
      return true;
    }
    return false;
  }

  private void sprinkle(int x, int y) {
    if (palette != null && sandbox != null) {
      sandbox.setParticle(x, y, PaletteView.ELEMENTS[palette.selected % PaletteView.ELEMENTS.length]);
    }
  }

  public void setScale(float scale) {
  }
}
