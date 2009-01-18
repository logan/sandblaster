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

  public static class Camera {

    private float scale;
    private PointF offset;
    private Point transformedOffset;
    private int objectWidth;
    private int objectHeight;
    private int viewWidth;
    private int viewHeight;

    public Camera() {
      this(1);
    }

    public Camera(float scale) {
      this.scale = scale;
      recenter();
    }

    public void setScale(float scale) {
      this.scale = scale;
      update();
      transformedOffset = getOffset();
    }

    public void pan(float x, float y) {
      offset.offset(x, y);
      transformedOffset = getOffset();
    }

    public void recenter() {
      offset = new PointF(0, 0);
      update();
    }

    private void update() {
      transformedOffset = getOffset();
    }

    final private Point getOffset() {
      return new Point(
          Math.round(offset.x + (viewWidth - objectWidth * scale) / 2),
          Math.round(offset.y + (viewHeight - objectHeight * scale) / 2));
    }

    final public Point viewToObject(Point pt) {
      Point o = transformedOffset;
      return new Point(Math.round((pt.x - o.x) / scale), objectHeight - Math.round((pt.y - o.y) / scale) - 1);
    }

    final public Point objectToView(Point pt) {
      Point o = transformedOffset;
      return new Point(o.x + Math.round(pt.x * scale), o.y + Math.round((objectHeight - pt.y - 1) * scale));
    }

    public void setObjectDimensions(int width, int height) {
      objectWidth = width;
      objectHeight = height;
      update();
    }

    public void setViewDimensions(int width, int height) {
      Log.i("view dimensions: {0} x {1}", width, height);
      viewWidth = width;
      viewHeight = height;
      update();
    }

  }

  protected SurfaceView surfaceView;
  protected Camera camera;
  protected SandBox sandbox;
  protected FrameRateCounter fpsCounter;
  protected int lastFpsRight;
  protected Bitmap bitmap;

  public AbsRenderer() {
    fpsCounter = new FrameRateCounter();
    camera = new Camera();
  }

  public void setSurfaceView(SurfaceView surfaceView) {
    this.surfaceView = surfaceView;
    surfaceView.getHolder().addCallback(this);
  }

  public Camera getCamera() {
    return camera;
  }

  public void setSandBox(SandBox sandbox) {
    this.sandbox = sandbox;
    camera.setObjectDimensions(sandbox.getWidth(), sandbox.getHeight());
  }

  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    camera.setViewDimensions(width, height);
  }

  public void surfaceDestroyed(SurfaceHolder holder) {
  }

  public void surfaceCreated(SurfaceHolder holder) {
    camera.setViewDimensions(surfaceView.getWidth(), surfaceView.getHeight());
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
