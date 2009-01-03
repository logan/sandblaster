package com.loganh.sandblaster;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.Surface;
import android.view.SurfaceHolder;

public class SandBoxRenderer {

  private SurfaceHolder surface;
  private FrameRateCounter fpsCounter;
  private int lastFpsRight;
  private boolean clean;

  public SandBoxRenderer(SurfaceHolder surface) {
    this.surface = surface;
    fpsCounter = new FrameRateCounter();
  }

  public void draw(SandBox sandbox) {
    Canvas canvas = null;
    try {
      canvas = surface.lockCanvas();
      if (canvas != null) {
        synchronized (canvas) {
          draw(sandbox, canvas);
          fpsCounter.update();
          if (true || SandActivity.DEBUG) {
            drawFps(canvas);
          }
        }
      }
    } finally {
      if (canvas != null) {
        surface.unlockCanvasAndPost(canvas);
      }
    }
  }

  static public void setPixels(SandBox sandbox) {
    synchronized (sandbox) {
      int w = sandbox.getWidth();
      int h = sandbox.getHeight();
      for (int i = sandbox.lastCleanIndex; i != sandbox.lastDirtyIndex; i = (i + 1) % sandbox.dirtyPixels.length) {
        int offset = sandbox.dirtyPixels[i];
        int y = offset / w;
        int x = offset % w;
        Element e = sandbox.elements[x][y];
        sandbox.bitmap.setPixel(x, h - y - 1, e == null ? Color.BLACK : e.color);
      }
      sandbox.lastCleanIndex = sandbox.lastDirtyIndex;
    }
  }

  private void draw(SandBox sandbox, Canvas canvas) {
    synchronized (sandbox) {
      setPixels(sandbox);
      Rect src = new Rect(0, 0, sandbox.getWidth(), sandbox.getHeight());
      Rect dest = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
      if (!clean) {
        canvas.drawRect(dest, new Paint());
        clean = true;
      }
      canvas.drawBitmap(sandbox.bitmap, src, dest, new Paint());
    }
  }

  private void drawFps(Canvas canvas) {
    String fps = "FPS: " + fpsCounter.getFps();
    Rect bounds = new Rect();
    Paint paint = new Paint();
    paint.setColor(Color.BLACK);
    paint.getTextBounds(fps, 0, fps.length(), bounds);
    canvas.drawRect(0, 0, Math.max(lastFpsRight, bounds.right), -bounds.top, paint);
    lastFpsRight = bounds.right;
    paint.setColor(Color.WHITE);
    canvas.drawText(fps, 0, -bounds.top, paint);
  }
}
