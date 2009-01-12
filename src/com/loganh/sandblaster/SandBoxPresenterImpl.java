package com.loganh.sandblaster;

import java.io.*;

import android.content.res.AssetManager;
import android.content.Context;

import org.xmlpull.v1.XmlPullParserException;


public class SandBoxPresenterImpl extends BaseSandBoxPresenter {

  private Context context;
  private AssetManager assets;
  private SandBoxDriver driver;
  private float fps;

  public SandBoxPresenterImpl(AssetManager assets, Context context, float fps) {
    super();
    this.assets = assets;
    this.context = context;
    this.fps = fps;
  }

  @Override
  public void setSandBox(SandBox sandbox) {
    if (driver != null) {
      stop();
    }
    super.setSandBox(sandbox);
    Log.i("setting sandbox at iteration {0}", sandbox.iteration);
    draw();
  }

  @Override
  public void stop() {
    if (driver != null) {
      driver.shutdown();
      driver = null;
      super.stop();
    }
  }

  @Override
  public void start() {
    Log.i("presenter start request");
    if (sandbox == null) {
      Log.i("creating new sandbox");
      if (!newSandBox()) {
        Log.e("unable to create new sandbox");
      }
    }
    if (driver == null && sandbox.playing) {
      super.start();
      driver = new SandBoxDriver(sandbox, renderer, fps);
      driver.start();
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
    if (sandbox != null) {
      sandbox.playing = true;
      start();
    }
  }

  @Override
  public boolean loadSandBox(String name) {
    try {
      Snapshot snapshot = new Snapshot(name, context);
      if (snapshot.sandbox != null) {
        undoStack = snapshot.undoStack;
        setSandBox(snapshot.sandbox);
        notifyLoadListeners();
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
      SandBox sandbox = XmlSnapshot.read(new InputStreamReader(assets.open(name)), context);
      if (sandbox != null) {
        setSandBox(sandbox);
        undoStack.clear();
        notifyLoadListeners();
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
      setSandBox(XmlSnapshot.read(new InputStreamReader(stream), context));
      undoStack.clear();
      notifyLoadListeners();
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
    if (sandbox != null) {
      Snapshot snapshot = new Snapshot(sandbox, undoStack);
      snapshot.name = name;
      try {
        snapshot.save(context);
        return true;
      } catch (IOException ex) {
        Log.e("failed to save sandbox to " + name, ex);
      }
    }
    return false;
  }

  @Override
  public void pauseDriver() {
    if (driver != null) {
      driver.sleep();
    }
  }

  @Override
  public void resumeDriver() {
    if (driver != null) {
      driver.wake();
    }
  }

}
