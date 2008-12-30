package com.loganh.sandblaster;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
          drawFps(canvas);
        }
      }
    } finally {
      if (canvas != null) {
        surface.unlockCanvasAndPost(canvas);
      }
    }
  }

  private void draw(SandBox sandbox, Canvas canvas) {
    float elemWidth = Math.min(canvas.getWidth() / sandbox.getWidth(), canvas.getHeight() / sandbox.getHeight());
    Paint paint = new Paint();
    paint.setColor(Color.BLACK);
    canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
    synchronized (sandbox) {
      for (SandBox.Particle particle : sandbox) {
        if (particle.element != null) {
          int cx = sandbox.toCanvasX(particle.x, canvas.getWidth());
          int cy = sandbox.toCanvasY(particle.y, canvas.getHeight());
          paint.setColor(particle.element.color);
          canvas.drawRect(cx, cy, cx + elemWidth, cy + elemWidth, paint);
        } else {
          Log.e("particle with null element at {0}, {1}", particle.x, particle.y);
        }
      }
    }
  }

  private void drawFps(Canvas canvas) {
    Paint paint = new Paint();
    paint.setColor(Color.WHITE);
    canvas.drawText("FPS: " + fpsCounter.getFps(), 0, 20, paint);
  }
}
