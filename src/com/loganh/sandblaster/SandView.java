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

public class SandView extends LinearLayout
    implements SandBoxPresenter.PlaybackListener, SandBoxPresenter.LoadListener {
  static final private float ZOOM_FACTOR = 1.2f;
  static final private float MIN_SCALE = 1f;
  static final private float MAX_SCALE = 16f;
  static final private float SCROLL_FACTOR = -30;

  // If a single location is touched for this long (in ms), make it permanent.
  static final private long PEN_STICK_THRESHOLD = 500;

  private SurfaceView surface;
  private ZoomControls zoomControls;
  private SandBoxPresenter presenter;
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
        return new Point(surface.getWidth(), surface.getHeight());
      }

      public Point getObjectSize() {
        return new Point(presenter.getWidth(), presenter.getHeight());
      }
    };
  }

  public void setSandBoxPresenter(SandBoxPresenter presenter) {
    this.presenter = presenter;
    presenter.addPlaybackListener(this);
    presenter.addLoadListener(this);
    if (surface != null) {
      presenter.setView(surface, camera);
    }
    palette.setSandBoxPresenter(presenter);
  }

  @Override
  protected void onFinishInflate() {
    Log.i("Finish inflate");
    surface = (SurfaceView) findViewById(R.id.surface);
    zoomControls = (ZoomControls) findViewById(R.id.zoom);
    if (presenter != null) {
      presenter.setView(surface, camera);
    }

    playbackButton = (ImageButton) findViewById(R.id.playback);
    playbackButton.setImageResource(android.R.drawable.ic_media_play);
    playbackButton.setEnabled(false);
    playListener = new OnClickListener() {
      public void onClick(View v) {
        Log.i("click on play");
        playbackButton.setEnabled(false);
        presenter.unpause();
      }
    };

    pauseListener = new OnClickListener() {
      public void onClick(View v) {
        Log.i("click on pause");
        playbackButton.setEnabled(false);
        presenter.pause();
      }
    };

    playbackButton.setOnClickListener(playListener);

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
    zoomToFit();
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
  }

  public void onLoad() {
    playbackButton.setEnabled(true);
    zoomToFit();
  }

  public void setPaletteView(PaletteView palette) {
    this.palette = palette;
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
    setScale(Math.min(getWidth() / (float) presenter.getWidth(), getHeight() / (float) presenter.getHeight()));
    camera.recenter();
  }

  private void setScale(float scale) {
    presenter.pauseDriver();
    Log.i("setting scale from {0} to {1}", this.scale, scale);
    this.scale = scale;
    camera.setScale(scale);
    zoomControls.setIsZoomInEnabled(canZoomIn());
    zoomControls.setIsZoomOutEnabled(canZoomOut());
    Log.i("rendering");
    presenter.draw();
    presenter.resumeDriver();
  }

  public boolean canZoomIn() {
    return scale * ZOOM_FACTOR <= MAX_SCALE;
  }

  public boolean canZoomOut() {
    return scale / ZOOM_FACTOR >= MIN_SCALE;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    Point eventPoint = new Point((int) event.getX(), (int) event.getY());
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      // TODO: pressure sensitivity
      penDown = true;
      penDownTime = SystemClock.uptimeMillis();
      lastPen = camera.viewToObject(eventPoint);
      presenter.pauseDriver();
      presenter.addSource(palette.getElement(), lastPen.x, lastPen.y);
      presenter.resumeDriver();
      return true;
    } else if (penDown && event.getAction() == MotionEvent.ACTION_MOVE) {
      Point newPen = camera.viewToObject(eventPoint);
      if (newPen.equals(lastPen)) {
        return true;
      }
      penDownTime = SystemClock.uptimeMillis();
      presenter.pauseDriver();
      presenter.removeSource(lastPen.x, lastPen.y);
      presenter.line(palette.getElement(), lastPen.x, lastPen.y, newPen.x, newPen.y);
      lastPen = newPen;
      presenter.addSource(palette.getElement(), lastPen.x, lastPen.y);
      presenter.draw();
      presenter.resumeDriver();
      return true;
    } else if (penDown && event.getAction() == MotionEvent.ACTION_UP) {
      if (SystemClock.uptimeMillis() - penDownTime < PEN_STICK_THRESHOLD) {
        presenter.pauseDriver();
        presenter.removeSource(lastPen.x, lastPen.y);
        presenter.resumeDriver();
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
