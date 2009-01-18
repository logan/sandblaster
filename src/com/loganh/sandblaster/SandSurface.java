package com.loganh.sandblaster;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;


public class SandSurface extends SurfaceView {

  private AbsRenderer.Camera camera;
  private SandBoxDrawer drawer;
  private SandBoxPresenter presenter;

  public SandSurface(Context context, AttributeSet attrs) {
    super(context, attrs);
    drawer = new SandBoxDrawer();
  }

  public void setCamera(AbsRenderer.Camera camera) {
    this.camera = camera;
  }

  public void setSandBoxPresenter(SandBoxPresenter presenter) {
    this.presenter = presenter;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (camera == null) {
      return false;
    }
    Pen pen = presenter.getPen();
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      presenter.pauseDriver();
      Point p = getTouchPoint(event);
      pen.press(drawer, p.x, p.y);
      presenter.draw();
      presenter.resumeDriver();
      return true;
    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
      presenter.pauseDriver();
      Point p = getTouchPoint(event);
      pen.drag(drawer, p.x, p.y);
      presenter.draw();
      presenter.resumeDriver();
      return true;
    } else if (event.getAction() == MotionEvent.ACTION_UP) {
      presenter.pauseDriver();
      Point p = getTouchPoint(event);
      pen.release(drawer, p.x, p.y);
      presenter.draw();
      presenter.resumeDriver();
      return true;
    }
    return false;
  }

  private Point getTouchPoint(MotionEvent event) {
    Point p = new Point(Math.round(event.getX()), Math.round(event.getY()));
    return camera.viewToObject(p);
  }

  private class SandBoxDrawer implements Pen.Target {
    public void drawAt(Element element, int x, int y) {
      if (presenter.getPen().getSelectedTool() == Pen.Tool.SPOUT) {
        if (element == null) {
          presenter.removeSource(x, y);
        } else {
          presenter.addSource(element, x, y);
        }
      } else {
        presenter.removeSource(x, y);
        presenter.getSandBox().setParticle(x, y, element);
      }
    }
  }

}
