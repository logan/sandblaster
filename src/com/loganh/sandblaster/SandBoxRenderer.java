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
          //drawFps(canvas);
        }
      }
    } finally {
      if (canvas != null) {
        surface.unlockCanvasAndPost(canvas);
      }
    }
  }

  private void draw(SandBox sandbox, Canvas canvas) {
    synchronized (sandbox) {
      if (sandbox.lastDirtyIndex == 0) {
        return;
      }
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
      Rect src = new Rect(0, 0, w, h);
      Rect dest = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
      canvas.drawBitmap(sandbox.bitmap, src, dest, new Paint());
    }
  }

  private void slowDraw(SandBox sandbox, Canvas canvas) {
    float elemWidth = Math.min(canvas.getWidth() / sandbox.getWidth(), canvas.getHeight() / sandbox.getHeight());
    float r = elemWidth / 2;
    Paint paint = new Paint();
    paint.setColor(Color.BLACK);
    canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
    synchronized (sandbox) {
      int h = sandbox.getHeight();
      int w = sandbox.getWidth();
      float scale = sandbox.getScale();
      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x = sandbox.rightNeighbors[x][y]) {
          Element e = sandbox.elements[x][y];
          if (e != null) {
            float cx = x / scale;
            float cy = (h - y - 1) / scale;
            paint.setColor(e.color);
            if (scale < 1) {
              if (e.mobile) {
                //canvas.drawCircle(cx + r, cy + r, r, paint);
                canvas.drawRect(cx, cy, cx + elemWidth, cy + elemWidth, paint);
              } else {
                canvas.drawRect(cx, cy, cx + elemWidth, cy + elemWidth, paint);
              }
            } else {
              canvas.drawPoint(cx, cy, paint);
            }
          }
        }
      }
      for (SandBox.Source source : sandbox.getSources()) {
        int x = sandbox.toCanvasX(source.x);
        int y = sandbox.toCanvasY(source.y);
        paint.setColor(source.element.color);
        canvas.drawRect(x, y, x + elemWidth, y + elemWidth, paint);
      }
    }
  }

  private void drawFps(Canvas canvas) {
    Paint paint = new Paint();
    paint.setColor(Color.WHITE);
    canvas.drawText("FPS: " + fpsCounter.getFps(), 0, 20, paint);
  }
}
