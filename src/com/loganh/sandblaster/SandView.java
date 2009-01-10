package com.loganh.sandblaster;

import android.content.Context;
import android.graphics.Point;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ZoomControls;

public class SandView extends LinearLayout {
  static final private float ZOOM_FACTOR = 1.2f;
  static final private float MIN_SCALE = 1f;
  static final private float MAX_SCALE = 16f;
  static final private float SCROLL_FACTOR = -30;

  // If a single location is touched for this long (in ms), make it permanent.
  static final private long PEN_STICK_THRESHOLD = 500;

  private SurfaceView surface;
  private ZoomControls zoomControls;
  private SandBox sandbox;
  private SandBoxDriver driver;
  private SandBoxRenderer renderer;
  private SandBoxRenderer.Camera camera;
  private PaletteView palette;
  private boolean penDown;
  private long penDownTime;
  private Point lastPen;
  private float scale;
  private ImageButton playbackButton;
  private OnClickListener playListener;
  private OnClickListener pauseListener;

  public SandView(Context context, AttributeSet attrs) {
    super(context, attrs);
    scale = 1;
    camera = new SandBoxRenderer.Camera(scale) {
      public Point getViewSize() {
        return new Point(SandView.this.surface.getWidth(), SandView.this.surface.getHeight());
      }

      public Point getObjectSize() {
        return new Point(SandView.this.sandbox.getWidth(), SandView.this.sandbox.getHeight());
      }
    };
  }

  @Override
  protected void onFinishInflate() {
    Log.i("Finish inflate");
    surface = (SurfaceView) findViewById(R.id.surface);
    zoomControls = (ZoomControls) findViewById(R.id.zoom);
    playbackButton = (ImageButton) findViewById(R.id.playback);
    playbackButton.setImageResource(android.R.drawable.ic_media_play);
    //playbackButton.setEnabled(false);
    playListener = new OnClickListener() {
      public void onClick(View v) {
        Log.i("click on play");
        //playbackButton.setEnabled(false);
        if (sandbox != null) {
          ((SandActivity) getContext()).startDriver();
        }
      }
    };

    pauseListener = new OnClickListener() {
      public void onClick(View v) {
        Log.i("click on pause");
        //playbackButton.setEnabled(false);
        if (sandbox != null) {
          ((SandActivity) getContext()).stopDriver();
        }
      }
    };

    playbackButton.setOnClickListener(playListener);

    renderer = new SandBoxRenderer(surface, camera);

    zoomControls.setOnZoomInClickListener(new OnClickListener() {
          public void onClick(View v) {
            zoomIn();
          }
        });
    zoomControls.setOnZoomOutClickListener(new OnClickListener() {
          public void onClick(View v) {
            zoomOut();
          }
        });
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    if (sandbox != null) {
      zoomToFit();
    }
  }

  public void onStart() {
    playbackButton.setImageResource(android.R.drawable.ic_media_pause);
    playbackButton.setEnabled(true);
    playbackButton.setOnClickListener(pauseListener);
  }

  public void onStop() {
    playbackButton.setImageResource(android.R.drawable.ic_media_play);
    playbackButton.setEnabled(true);
    playbackButton.setOnClickListener(playListener);
    if (sandbox != null) {
      Log.i("rendering");
      renderer.draw();
    }
  }

  public SandBoxRenderer getRenderer() {
    return renderer;
  }

  public void setPaletteView(PaletteView palette) {
    this.palette = palette;
  }

  public void setSandBox(SandBox sandbox) {
    this.sandbox = sandbox;
    renderer.setSandBox(sandbox);
    playbackButton.setEnabled(true);
    Log.i("setSandBox calling zoomToFit");
    zoomToFit();
    Log.i("zoomed to {0}", scale);
  }

  public void setSandBoxDriver(SandBoxDriver driver) {
    this.driver = driver;
  }

  private void driverSleep() {
    if (driver != null) {
      driver.sleep();
    }
  }

  private void driverWake() {
    if (driver != null) {
      driver.wake();
    }
  }

  public void zoomIn() {
    if (canZoomIn()) {
      setScale(scale * ZOOM_FACTOR);
    }
  }

  public void zoomOut() {
    if (canZoomOut()) {
      setScale(scale / ZOOM_FACTOR);
    }
  }

  public void zoomToFit() {
    setScale(Math.min(getWidth() / (float) sandbox.getWidth(), getHeight() / (float) sandbox.getHeight()));
    camera.recenter();
  }

  private void setScale(float scale) {
    driverSleep();
    Log.i("setting scale from {0} to {1}", this.scale, scale);
    this.scale = scale;
    camera.setScale(scale);
    zoomControls.setIsZoomInEnabled(canZoomIn());
    zoomControls.setIsZoomOutEnabled(canZoomOut());
    if (sandbox != null) {
      Log.i("rendering");
      renderer.draw();
    }
    driverWake();
  }

  public boolean canZoomIn() {
    return scale * ZOOM_FACTOR <= MAX_SCALE;
  }

  public boolean canZoomOut() {
    return scale / ZOOM_FACTOR >= MIN_SCALE;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (sandbox == null) {
      return false;
    }
    Point eventPoint = new Point((int) event.getX(), (int) event.getY());
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      // TODO: pressure sensitivity
      penDown = true;
      penDownTime = SystemClock.uptimeMillis();
      lastPen = camera.viewToObject(eventPoint);
      driverSleep();
      sandbox.addSource(palette.getElement(), lastPen.x, lastPen.y);
      driverWake();
      return true;
    } else if (penDown && event.getAction() == MotionEvent.ACTION_MOVE) {
      Point newPen = camera.viewToObject(eventPoint);
      if (newPen.equals(lastPen)) {
        return true;
      }
      penDownTime = SystemClock.uptimeMillis();
      driverSleep();
      sandbox.removeSource(lastPen.x, lastPen.y);
      sandbox.line(palette.getElement(), lastPen.x, lastPen.y, newPen.x, newPen.y);
      lastPen = newPen;
      sandbox.addSource(palette.getElement(), lastPen.x, lastPen.y);
      renderer.draw();
      driverWake();
      return true;
    } else if (penDown && event.getAction() == MotionEvent.ACTION_UP) {
      if (SystemClock.uptimeMillis() - penDownTime < PEN_STICK_THRESHOLD) {
        driverSleep();
        sandbox.removeSource(lastPen.x, lastPen.y);
        driverWake();
      }
      penDown = false;
      return true;
    }
    return false;
  }

  @Override
  public boolean onTrackballEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_MOVE) {
      camera.pan(SCROLL_FACTOR * event.getX(), SCROLL_FACTOR * event.getY());
      return true;
    }
    return false;
  }
}
