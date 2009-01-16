package com.loganh.sandblaster;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

abstract public class AbsRenderer implements SurfaceHolder.Callback {

  public static abstract class Camera {

    private float scale;
    private PointF offset;

    public Camera(float scale) {
      this.scale = scale;
      recenter();
    }

    public void setScale(float scale) {
      this.scale = scale;
    }

    public void pan(float x, float y) {
      offset.offset(x, y);
    }

    public void recenter() {
      offset = new PointF(0, 0);
    }

    private Point getOffset() {
      Point o = getObjectSize();
      Point v = getViewSize();
      return new Point(Math.round(offset.x + (v.x - o.x * scale) / 2), Math.round(offset.y + (v.y - o.y * scale) / 2));
    }

    public Point viewToObject(Point pt) {
      Point o = getOffset();
      return new Point(Math.round((pt.x - o.x) / scale), getObjectSize().y - Math.round((pt.y - o.y) / scale) - 1);
    }

    public Point objectToView(Point pt) {
      Point result = getOffset();
      result.offset(Math.round(pt.x * scale), Math.round((getObjectSize().y - pt.y - 1) * scale));
      return result;
    }

    abstract public Point getObjectSize();
    abstract public Point getViewSize();
  }

  protected SurfaceView surfaceView;
  protected Camera camera;
  protected SandBox sandbox;
  protected FrameRateCounter fpsCounter;
  protected int lastFpsRight;
  protected Bitmap bitmap;

  public AbsRenderer() {
    fpsCounter = new FrameRateCounter();
  }

  public void setSurfaceView(SurfaceView surfaceView) {
    this.surfaceView = surfaceView;
    surfaceView.getHolder().addCallback(this);
  }

  public void setCamera(Camera camera) {
    this.camera = camera;
  }

  public void setSandBox(SandBox sandbox) {
    this.sandbox = sandbox;
  }

  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
  }

  public void surfaceDestroyed(SurfaceHolder holder) {
  }

  public void surfaceCreated(SurfaceHolder holder) {
    draw();
  }

  public void draw() {
    if (sandbox == null || surfaceView == null || surfaceView.getHolder() == null) {
      return;
    }
    Canvas canvas = null;
    try {
      canvas = surfaceView.getHolder().lockCanvas();
      if (canvas != null) {
        synchronized (canvas) {
          synchronized (sandbox) {
            draw(canvas);
            fpsCounter.update();
            if (SandActivity.DEBUG) {
              drawFps(canvas);
            }
          }
        }
      }
    } finally {
      if (canvas != null) {
        surfaceView.getHolder().unlockCanvasAndPost(canvas);
      }
    }
  }

  protected void drawFps(Canvas canvas) {
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

  abstract protected void draw(Canvas canvas);

}
