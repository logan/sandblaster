package com.loganh.sandblaster;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;

public class SandView extends SurfaceView {
  static public float FPS = 20;
  static public float SCALE = 1f / 8;

  private SandBox sandbox;
  private SandBoxDriver driver;
  private SandBoxRenderer renderer;
  private PaletteView palette;
  private boolean penDown;

  public SandView(Context context, AttributeSet attrs) {
    super(context, attrs);
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

  @Override
  synchronized public void onLayout(boolean changed, int left, int top, int right, int bottom) {
    if (sandbox == null) {
      Log.i("creating {0} x {1} sandbox", getWidth() * SCALE, getHeight() * SCALE);
      sandbox = new SandBox((int) (getWidth() * SCALE), (int) (getHeight() * SCALE));
      startDriver();
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
      sandbox.setParticle(x, y, SandBox.ELEMENTS[palette.selected % SandBox.ELEMENTS.length]);
    }
  }
}
