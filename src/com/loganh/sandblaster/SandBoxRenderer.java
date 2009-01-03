package com.loganh.sandblaster;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.Surface;
import android.view.SurfaceHolder;

public class SandBoxRenderer {

  private static final int VOID_COLOR = Color.rgb(0, 0, 0);
  private static final int PADDING_COLOR = Color.rgb(0xcc, 0xcc, 0xcc);

  public static abstract class Camera {

    private float scale;
    private Point offset;

    public Camera(float scale) {
      this.scale = scale;
      offset = new Point(0, 0);
    }

    public void setScale(float scale) {
      this.scale = scale;
    }

    private Point getOffset() {
      Point o = getObjectSize();
      Point v = getViewSize();
      return new Point(offset.x + Math.round((v.x - o.x / scale) / 2), offset.y + Math.round((v.y - o.y / scale) / 2));
    }

    public Point viewToObject(Point pt) {
      Point o = getOffset();
      return new Point(Math.round((pt.x - o.x) * scale), getObjectSize().y - Math.round((pt.y - o.y) * scale) - 1);
    }

    public Point objectToView(Point pt) {
      Point result = getOffset();
      result.offset(Math.round(pt.x / scale), Math.round((getObjectSize().y - pt.y - 1) / scale));
      return result;
    }

    abstract public Point getObjectSize();
    abstract public Point getViewSize();
  }

  private SurfaceHolder surface;
  private Camera camera;
  private FrameRateCounter fpsCounter;
  private int lastFpsRight;

  public SandBoxRenderer(SurfaceHolder surface, Camera camera) {
    this.surface = surface;
    this.camera = camera;
    fpsCounter = new FrameRateCounter();
  }

  public void draw(SandBox sandbox) {
    Canvas canvas = null;
    try {
      canvas = surface.lockCanvas();
      if (canvas != null) {
        synchronized (canvas) {
          synchronized (sandbox) {
            setPixels(sandbox);
            draw(sandbox, canvas);
            fpsCounter.update();
            if (true || SandActivity.DEBUG) {
              drawFps(canvas);
            }
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
    Point topLeft = camera.objectToView(new Point(0, sandbox.getHeight() - 1));
    Point bottomRight = camera.objectToView(new Point(sandbox.getWidth(), 0));
    Rect src = new Rect(0, 0, sandbox.getWidth(), sandbox.getHeight());
    Rect dest = new Rect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y);
    Paint paint = new Paint();
    paint.setColor(PADDING_COLOR);
    canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
    paint.setColor(VOID_COLOR);
    canvas.drawRect(dest, paint);
    canvas.drawBitmap(sandbox.bitmap, src, dest, paint);
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
