package com.loganh.sandblaster;

import java.io.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebView;
import android.widget.TextView;

public class SandActivity extends Activity {

  // Maximum FPS.
  static final private float FPS = 20;

  // Request codes.
  static final private int LOAD_SNAPSHOT_REQUEST = 0;
  static final private int SAVE_SNAPSHOT_REQUEST = 1;

  // Menu codes.
  static final private int CLEAR = 0;
  static final private int BIGGER = 1;
  static final private int SMALLER = 2;
  static final private int DEMO = 3;
  static final private int TRACE_ON = 4;
  static final private int TRACE_OFF = 5;
  static final private int ABOUT = 6;
  static final private int SAVE = 9;
  static final private int LOAD = 10;

  static final public boolean DEBUG = Build.DEVICE.equals("generic");

  private SandView view;
  private PaletteView palette;

  private MenuItem biggerItem;
  private MenuItem smallerItem;

  private SandBox sandbox;
  private SandBoxDriver driver;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.sand);
    view = (SandView) findViewById(R.id.sand);
    palette = (PaletteView) findViewById(R.id.palette);
    palette.invalidate();
    assert(view != null);
    assert(palette != null);
    view.setPaletteView(palette);
    initializeSandBox();
    startDriver();
  }

  private void initializeSandBox() {
    try {
      Snapshot snapshot = new Snapshot("Autosave", this);
      setSandBox(snapshot.sandbox);
    } catch (IOException ex) {
      Log.e("unable to load Autosave", ex);
      installDemo();
    }
  }

  public void setSandBox(SandBox sandbox) {
    this.sandbox = sandbox;
    view.setSandBox(sandbox);
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.i("resume");
    startDriver();
  }

  @Override
  public void onPause() {
    super.onPause();
    Log.i("pause");
    stopDriver();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, CLEAR, 0, R.string.menu_clear).setIcon(R.drawable.clear);
    biggerItem = menu.add(0, BIGGER, 0, R.string.menu_bigger);
    biggerItem.setIcon(R.drawable.bigger);
    smallerItem = menu.add(0, SMALLER, 0, R.string.menu_smaller);
    smallerItem.setIcon(R.drawable.smaller);
    if (DEBUG) {
      // Ensure these are in the first five options, so we don't have to go
      // through the "More" submenu.
      menu.add(0, TRACE_ON, 0, R.string.menu_trace_on);
      menu.add(0, TRACE_OFF, 0, R.string.menu_trace_off);
    }
    menu.add(0, DEMO, 0, R.string.menu_demo);
    menu.add(0, ABOUT, 0, R.string.menu_about);
    menu.add(0, SAVE, 0, R.string.menu_save);
    menu.add(0, LOAD, 0, R.string.menu_load);
    return true;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.i("activity result: requestCode={0}, resultCode={1}, uri={2}",
        requestCode, resultCode, data == null ? null : data.getDataString());
    switch (requestCode) {
      case LOAD_SNAPSHOT_REQUEST:
        if (resultCode == Snapshot.LOAD_SNAPSHOT_RESULT) {
          String name = data.getData().getSchemeSpecificPart();
          try {
            Snapshot snapshot = new Snapshot(name, this);
            view.setSandBox(snapshot.sandbox);
            Log.i("loaded {0}", snapshot.name);
          } catch (IOException ex) {
            Log.e("failed to load " + name, ex);
          }
        }
        break;
      case SAVE_SNAPSHOT_REQUEST:
        if (resultCode == Snapshot.SAVE_SNAPSHOT_RESULT) {
          String name = data.getData().getSchemeSpecificPart();
          Snapshot snapshot = new Snapshot(sandbox);
          snapshot.name = name;
          try {
            snapshot.save(this);
          } catch (IOException ex) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);
            builder.setTitle(R.string.save_error_title);
            builder.setMessage(R.string.save_error_message);
            builder.show();
          }
        }
        break;
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case CLEAR:
        clear();
        return true;
      case BIGGER:
        biggerItem.setEnabled(view.makeBigger());
        smallerItem.setEnabled(true);
        return true;
      case SMALLER:
        smallerItem.setEnabled(view.makeSmaller());
        biggerItem.setEnabled(true);
        return true;
      case DEMO:
        installDemo();
        return true;
      case TRACE_ON:
        Debug.startMethodTracing("sand");
        return true;
      case TRACE_OFF:
        Debug.stopMethodTracing();
        return true;
      case ABOUT:
        about();
        return true;
      case SAVE:
        save();
        return true;
      case LOAD:
        load();
        return true;
    }
    return false;
  }

  private void about() {
    StringBuffer content = new StringBuffer();
    try {
      InputStream stream = getAssets().open("about.html");
      BufferedReader in = new BufferedReader(new InputStreamReader(stream), 8192);
      while (true) {
        String line = in.readLine();
        if (line == null) {
          break;
        }
        content.append(line).append("\n");
      }
    } catch (IOException ex) {
      Log.e("Failed to read about.html", ex);
      return;
    }

    WebView webView = new WebView(this);
    webView.loadDataWithBaseURL(null, content.toString(), "text/html", "ascii", null);

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setCancelable(true);
    builder.setTitle(R.string.about_title);
    builder.setView(webView);
    builder.show();
  }

  private void load() {
    Intent intent = new Intent(Intent.ACTION_PICK);
    intent.setDataAndType(Uri.EMPTY, "application/x-sandblaster-loadable");
    startActivityForResult(intent, LOAD_SNAPSHOT_REQUEST);
  }

  private void save() {
    Intent intent = new Intent(Intent.ACTION_PICK);
    intent.setDataAndType(Uri.EMPTY, "application/x-sandblaster-saveable");
    startActivityForResult(intent, SAVE_SNAPSHOT_REQUEST);
  }

  private void startDriver() {
    stopDriver();
    if (sandbox != null) {
      driver = new SandBoxDriver(sandbox, view.getRenderer(), FPS);
      driver.start();
    }
  }

  private void stopDriver() {
    if (driver != null) {
      driver.shutdown();
    }
    driver = null;
    if (sandbox != null) {
      Snapshot snapshot = new Snapshot(sandbox);
      snapshot.name = "Autosave";
      try {
        snapshot.save(this);
      } catch (IOException ex) {
        Log.e("Failed to autosave snapshot", ex);
      }
    }
  }

  private void clear() {
    if (sandbox != null) {
      sandbox.clear();
    }
  }

  private void installDemo() {
    startDriver();
    clear();
    if (sandbox != null) {
      Demo.install(sandbox);
    }
  }
}
