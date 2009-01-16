package com.loganh.sandblaster;

import android.view.SurfaceView;


public interface SandBoxPresenter {

  public interface PlaybackListener {
    public void onStart();
    public void onStop();
  }

  public interface EditListener {
    public void onEdit();
  }

  public interface LoadListener {
    public void onLoad();
  }

  public void addPlaybackListener(PlaybackListener listener);
  public boolean removePlaybackListener(PlaybackListener listener);
  public void addEditListener(EditListener listener);
  public boolean removeEditListener(EditListener listener);
  public void addLoadListener(LoadListener listener);
  public boolean removeLoadListener(LoadListener listener);
  public void setView(SurfaceView surface, AbsRenderer.Camera camera);
  public SandBox getSandBox();
  public UndoStack getUndoStack();
  public void setSandBox(SandBox sandbox);
  public int getWidth();
  public int getHeight();
  public ElementTable getElementTable();
  public void stop();
  public void start();
  public void pause();
  public void unpause();
  public boolean isPaused();
  public void addSource(Element element, int x, int y);
  public void removeSource(int x, int y);
  public void setParticle(Element element, int x, int y);
  public void setParticle(Element element, int radius, int x, int y);
  public void line(Element element, int x1, int y1, int x2, int y2);
  public void line(Element element, int radius, int x1, int y1, int x2, int y2);
  public boolean loadSandBox(String name);
  public boolean loadSandBoxFromAsset(String name);
  public boolean newSandBox();
  public boolean saveSandBox(String name);
  public void draw();
  public void pauseDriver();
  public void resumeDriver();
}
