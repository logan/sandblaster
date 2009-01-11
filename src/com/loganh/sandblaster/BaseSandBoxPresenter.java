package com.loganh.sandblaster;

import java.util.*;

import android.view.SurfaceView;


public class BaseSandBoxPresenter implements SandBoxPresenter {
  protected Set<PlaybackListener> playbackListeners;
  protected Set<EditListener> editListeners;
  protected Set<LoadListener> loadListeners;
  protected SandBox sandbox;
  protected SandBoxRenderer renderer;

  public BaseSandBoxPresenter() {
    playbackListeners = new HashSet<PlaybackListener>();
    editListeners = new HashSet<EditListener>();
    loadListeners = new HashSet<LoadListener>();
    renderer = new SandBoxRenderer();
  }

  public void addPlaybackListener(PlaybackListener listener) {
    playbackListeners.add(listener);
  }

  public boolean removePlaybackListener(PlaybackListener listener) {
    return playbackListeners.remove(listener);
  }

  public void addEditListener(EditListener listener) {
    editListeners.add(listener);
  }

  public boolean removeEditListener(EditListener listener) {
    return editListeners.remove(listener);
  }

  public void addLoadListener(LoadListener listener) {
    loadListeners.add(listener);
    if (sandbox != null) {
      listener.onLoad();
    }
  }

  public boolean removeLoadListener(LoadListener listener) {
    return loadListeners.remove(listener);
  }

  public void setView(SurfaceView surface, SandBoxRenderer.Camera camera) {
    renderer.setSurfaceView(surface);
    renderer.setCamera(camera);
  }

  public SandBox getSandBox() {
    return sandbox;
  }

  public void setSandBox(SandBox sandbox) {
    this.sandbox = sandbox;
    renderer.setSandBox(sandbox);
    for (LoadListener loadListener : loadListeners) {
      loadListener.onLoad();
    }
  }

  public int getWidth() {
    if (sandbox == null) {
      return -1;
    }
    return sandbox.getWidth();
  }

  public int getHeight() {
    if (sandbox == null) {
      return -1;
    }
    return sandbox.getHeight();
  }

  public ElementTable getElementTable() {
    if (sandbox == null) {
      return null;
    } else {
      return sandbox.elementTable;
    }
  }

  public boolean isPaused() {
    return (sandbox == null || !sandbox.playing);
  }

  protected void notifyEditListeners() {
    for (EditListener editListener : editListeners) {
      editListener.onEdit();
    }
  }

  public void addSource(Element element, int x, int y) {
    if (sandbox != null) {
      notifyEditListeners();
      sandbox.addSource(element, x, y);
    }
  }

  public void removeSource(int x, int y) {
    if (sandbox != null) {
      notifyEditListeners();
      sandbox.removeSource(x, y);
    }
  }

  public void setParticle(Element element, int x, int y) {
    if (sandbox != null) {
      notifyEditListeners();
      sandbox.setParticle(x, y, element);
    }
  }

  public void line(Element element, int x1, int y1, int x2, int y2) {
    if (sandbox != null) {
      notifyEditListeners();
      sandbox.line(element, x1, y1, x2, y2);
    }
  }

  public void start() {
    for (PlaybackListener playbackListener : playbackListeners) {
      playbackListener.onStart();
    }
  }

  public void stop() {
    for (PlaybackListener playbackListener : playbackListeners) {
      playbackListener.onStop();
    }
  }

  public void draw() {
    if (sandbox != null) {
      renderer.draw();
    }
  }

  public void pause() {
  }

  public void unpause() {
  }

  public boolean loadSandBox(String name) {
    return false;
  }

  public boolean loadSandBoxFromAsset(String name) {
    return false;
  }

  public boolean newSandBox() {
    return false;
  }

  public boolean saveSandBox(String name) {
    return false;
  }

  public void pauseDriver() {
  }

  public void resumeDriver() {
  }

}
