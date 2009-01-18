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

  private SandSurface surface;
  private ZoomControls zoomControls;
  private SandBoxPresenter presenter;
  private AbsRenderer.Camera camera;
  private PaletteView palette;
  private Toolbar toolbar;
  private boolean penDown;
  private long penDownTime;
  private Point lastPen;
  private float scale;
  private ImageButton playbackButton;
  private ImageButton undoButton;
  private OnClickListener playListener;
  private OnClickListener pauseListener;

  public SandView(Context context, AttributeSet attrs) {
    super(context, attrs);
    scale = 1;
  }

  public void setSandBoxPresenter(SandBoxPresenter presenter) {
    this.presenter = presenter;
    camera = presenter.getRenderer().getCamera();
    surface.setSandBoxPresenter(presenter);
    surface.setCamera(camera);
    presenter.addPlaybackListener(this);
    presenter.addLoadListener(this);
    if (surface != null) {
      presenter.setView(surface);
    }
    palette.setSandBoxPresenter(presenter);
    toolbar.setPen(presenter.getPen());
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    surface = (SandSurface) findViewById(R.id.surface);
    zoomControls = (ZoomControls) findViewById(R.id.zoom);
    if (presenter != null) {
      presenter.setView(surface);
    }

    playbackButton = (ImageButton) findViewById(R.id.playback);
    playbackButton.setImageResource(android.R.drawable.ic_media_play);
    playbackButton.setEnabled(false);
    playListener = new OnClickListener() {
      public void onClick(View v) {
        playbackButton.setEnabled(false);
        presenter.unpause();
      }
    };

    pauseListener = new OnClickListener() {
      public void onClick(View v) {
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

    undoButton = (ImageButton) findViewById(R.id.undo);
    undoButton.setEnabled(presenter != null && !presenter.getUndoStack().isEmpty());
    undoButton.setOnClickListener(new OnClickListener() {
          public void onClick(View v) {
            undo();
          }
        });

    toolbar = (Toolbar) findViewById(R.id.toolbar);
  }

  private void undo() {
    SandBox sandbox = presenter.getUndoStack().pop();
    if (sandbox != null) {
      presenter.setSandBox(sandbox);
    }
    undoButton.setEnabled(!presenter.getUndoStack().isEmpty());
  }

  private void pushUndo() {
    presenter.getUndoStack().push(presenter.getSandBox());
    undoButton.setEnabled(!presenter.getUndoStack().isEmpty());
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    zoomToFit();
  }

  public void onStart() {
    playbackButton.setImageResource(android.R.drawable.ic_media_pause);
    playbackButton.setEnabled(true);
    playbackButton.setOnClickListener(pauseListener);
    pushUndo();
  }

  public void onStop() {
    playbackButton.setImageResource(android.R.drawable.ic_media_play);
    playbackButton.setEnabled(true);
    playbackButton.setOnClickListener(playListener);
  }

  public void onLoad() {
    playbackButton.setEnabled(true);
    undoButton.setEnabled(!presenter.getUndoStack().isEmpty());
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
    this.scale = scale;
    camera.setScale(scale);
    zoomControls.setIsZoomInEnabled(canZoomIn());
    zoomControls.setIsZoomOutEnabled(canZoomOut());
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
  public boolean onTrackballEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_MOVE) {
      camera.pan(SCROLL_FACTOR * event.getX(), SCROLL_FACTOR * event.getY());
      presenter.draw();
      return true;
    }
    return false;
  }

}
