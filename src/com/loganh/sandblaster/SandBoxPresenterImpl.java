package com.loganh.sandblaster;

import java.io.*;
import java.util.Timer;
import java.util.TimerTask;

import android.content.res.AssetManager;
import android.content.Context;

import org.xmlpull.v1.XmlPullParserException;


public class SandBoxPresenterImpl extends BaseSandBoxPresenter {

  static public final String AUTOSAVE = "Autosave";

  private Context context;
  private AssetManager assets;
  private Timer timer;
  private SandBoxTimerTask task;
  private float fps;

  public SandBoxPresenterImpl(AssetManager assets, Context context, float fps) {
    super();
    this.assets = assets;
    this.context = context;
    this.fps = fps;
  }

  @Override
  public void setSandBox(SandBox sandbox) {
    stop();
    super.setSandBox(sandbox);
    Log.i("setting sandbox at iteration {0}", sandbox.iteration);
    draw();
  }

  @Override
  public void stop() {
    if (timer != null) {
      timer.cancel();
      timer = null;
      task = null;
      super.stop();
      Log.i("after stop, taking {0} snapshot", AUTOSAVE);
      saveSandBox(AUTOSAVE);
    }
  }

  @Override
  public void start() {
    Log.i("presenter start request");
    if (sandbox == null) {
      Log.i("no sandbox yet, trying {0}", AUTOSAVE);
      if (!loadSandBox(AUTOSAVE)) {
        Log.i("could not load autosave, loading demo");
        if (!loadSandBoxFromAsset("snapshot_demo.xml")) {
          Log.e("could not install demo, loading new sandbox");
          if (!newSandBox()) {
            Log.e("unable to create new sandbox");
          }
        }
      }
    }
    if (timer == null && sandbox.playing) {
      super.start();
      timer = new Timer();
      task = new SandBoxTimerTask();
      timer.scheduleAtFixedRate(task, 0, (long) (1000f / fps));
      Log.i("presenter playback started");
    }
  }

  @Override
  public void pause() {
    if (sandbox != null) {
      stop();
      sandbox.playing = false;
    }
  }

  @Override
  public void unpause() {
    Log.i("presenter unpause; sandbox = {0}", sandbox);
    if (sandbox != null) {
      sandbox.playing = true;
    }
    start();
  }

  @Override
  public boolean loadSandBox(String name) {
    try {
      Snapshot snapshot = new Snapshot(name, context);
      if (snapshot.sandbox != null) {
        undoStack = snapshot.undoStack;
        undoStack.push(snapshot.sandbox);
        setSandBox(snapshot.sandbox);
        notifyLoadListeners();
        unpause();
        return true;
      } else {
        Log.e("Error parsing {0}", name);
      }
    } catch (IOException ex) {
      Log.e("Unable to load " + name, ex);
    }
    return false;
  }

  @Override
  public boolean loadSandBoxFromAsset(String name) {
    try {
      SandBox sandbox = XmlSnapshot.read(new BufferedReader(new InputStreamReader(assets.open(name))), context);
      if (sandbox != null) {
        setSandBox(sandbox);
        undoStack.clear();
        undoStack.push(sandbox);
        notifyLoadListeners();
        unpause();
        return true;
      }
      Log.e("error loading sandbox from asset {0}", name);
    } catch (IOException ex) {
      Log.e("failed to load sandbox from asset " + name, ex);
    } catch (XmlPullParserException ex) {
      Log.e("failed to load sandbox from asset " + name, ex);
    }
    return false;
  }

  @Override
  public boolean newSandBox() {
    try {
      InputStream stream = assets.open("snapshot_new.xml");
      setSandBox(XmlSnapshot.read(new BufferedReader(new InputStreamReader(stream)), context));
      undoStack.clear();
      notifyLoadListeners();
      unpause();
      return true;
    } catch (IOException ex) {
      Log.e("Failed to load snapshot_new.xml", ex);
    } catch (XmlPullParserException ex) {
      Log.e("Failed to parse snapshot_new.xml", ex);
    }
    return false;
  }

  @Override
  public boolean saveSandBox(String name) {
    if (sandbox == null) {
      Log.i("no sandbox to save, loading from {0}", AUTOSAVE);
      if (!loadSandBox(AUTOSAVE)) {
        Log.e("failed to load {0}", AUTOSAVE);
      }
    }
    if (sandbox != null) {
      Snapshot snapshot = new Snapshot(sandbox, undoStack);
      snapshot.name = name;
      try {
        snapshot.save(context);
        return true;
      } catch (IOException ex) {
        Log.e("failed to save sandbox to " + name, ex);
      }
    } else {
      Log.e("no sandbox to save yet");
    }
    return false;
  }

  @Override
  public void pauseDriver() {
    if (task != null) {
      task.cancel();
      task = null;
    }
  }

  @Override
  public void resumeDriver() {
    if (timer != null) {
      task = new SandBoxTimerTask();
      timer.scheduleAtFixedRate(task, 0, (long) (1000f / fps));
    }
  }

  @Override
  public void addPlaybackListener(PlaybackListener listener) {
    super.addPlaybackListener(listener);
    if (sandbox == null || timer == null) {
      listener.onStop();
    } else {
      listener.onStart();
    }
  }

  @Override
  public void setLineOverlay(Element element, int x1, int y1, int x2, int y2) {
    renderer.setLineOverlay(element, x1, y1, x2, y2);
  }

  @Override
  public void clearLineOverlay() {
    renderer.clearLineOverlay();
  }

  private class SandBoxTimerTask extends TimerTask {

    @Override
    public void run() {
      try {
        sandbox.update();
        renderer.draw();
      } catch (Exception ex) {
        Log.e("update or draw failed", ex);
      }
    }
  }

}
