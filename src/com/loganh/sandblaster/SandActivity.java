package com.loganh.sandblaster;

import java.io.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebView;

import org.xmlpull.v1.XmlPullParserException;

public class SandActivity extends Activity {

  // Maximum FPS.
  static final private float FPS = 20;

  // Request codes.
  static final private int LOAD_SNAPSHOT_REQUEST = 0;
  static final private int SAVE_SNAPSHOT_REQUEST = 1;

  // Menu codes.
  static final private int CLEAR = 0;
  static final private int DEMO = 1;
  static final private int TRACE_ON = 2;
  static final private int TRACE_OFF = 3;
  static final private int DELETE_ALL = 4;
  static final private int ABOUT = 5;
  static final private int SAVE = 6;
  static final private int LOAD = 7;
  static final private int ZOOM_TO_FIT = 8;

  static final public boolean DEBUG = Build.DEVICE.equals("generic");

  private SandView view;
  private PaletteView palette;
  private SandBoxPresenter presenter;
  private boolean appRunning;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.sand);
    view = (SandView) findViewById(R.id.sand);
    palette = (PaletteView) findViewById(R.id.palette);
    presenter = new SandBoxPresenterImpl(getAssets(), this, FPS);
    view.setPaletteView(palette);
    view.setSandBoxPresenter(presenter);
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.i("resume");
    appRunning = true;
    presenter.unpause();
  }

  @Override
  public void onPause() {
    super.onPause();
    Log.i("pause");
    presenter.pause();
    appRunning = false;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, CLEAR, 0, R.string.menu_clear).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
    menu.add(0, ZOOM_TO_FIT, 0, R.string.menu_zoom_to_fit).setIcon(android.R.drawable.ic_menu_zoom);
    menu.add(0, ABOUT, 0, R.string.menu_about).setIcon(android.R.drawable.ic_menu_help);
    if (DEBUG) {
      // Ensure these are in the first five options, so we don't have to go
      // through the "More" submenu.
      menu.add(0, TRACE_ON, 0, R.string.menu_trace_on);
      menu.add(0, TRACE_OFF, 0, R.string.menu_trace_off);
      menu.add(0, DELETE_ALL, 0, R.string.menu_delete_all);
    }
    menu.add(0, SAVE, 0, R.string.menu_save).setIcon(android.R.drawable.ic_menu_save);
    menu.add(0, LOAD, 0, R.string.menu_load).setIcon(android.R.drawable.ic_menu_gallery);
    menu.add(0, DEMO, 0, R.string.menu_demo).setIcon(android.R.drawable.ic_menu_slideshow);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case CLEAR:
        clear();
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
      case DELETE_ALL:
        Snapshot.deleteAll(this);
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
      case ZOOM_TO_FIT:
        view.zoomToFit();
        return true;
    }
    return false;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.i("activity result: requestCode={0}, resultCode={1}, uri={2}",
        requestCode, resultCode, data == null ? null : data.getDataString());
    switch (requestCode) {
      case LOAD_SNAPSHOT_REQUEST:
        if (resultCode == Snapshot.LOAD_SNAPSHOT_RESULT) {
          String name = data.getData().getSchemeSpecificPart();
          if (!presenter.loadSandBox(name)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);
            builder.setTitle(R.string.load_error_title);
            builder.setMessage(String.format(getResources().getString(R.string.load_error_message), name));
            builder.show();
          }
        }
        break;
      case SAVE_SNAPSHOT_REQUEST:
        if (resultCode == Snapshot.SAVE_SNAPSHOT_RESULT) {
          String name = data.getData().getSchemeSpecificPart();
          if (!presenter.saveSandBox(name)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);
            builder.setTitle(R.string.save_error_title);
            builder.setMessage(String.format(getResources().getString(R.string.save_error_message), name));
            builder.show();
          }
        }
        break;
    }
  }

  private SandBox loadNewSandBox() {
    try {
      InputStream stream = getAssets().open("snapshot_new.xml");
      return XmlSnapshot.read(new BufferedReader(new InputStreamReader(stream)), this);
    } catch (IOException ex) {
      Log.e("Failed to load snapshot_new.xml", ex);
    } catch (XmlPullParserException ex) {
      Log.e("Failed to parse snapshot_new.xml", ex);
    }
    return null;
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
    presenter.stop();
    Intent intent = new Intent(Intent.ACTION_PICK);
    intent.setDataAndType(Uri.EMPTY, "application/x-sandblaster-loadable");
    startActivityForResult(intent, LOAD_SNAPSHOT_REQUEST);
  }

  private void save() {
    presenter.stop();
    Intent intent = new Intent(Intent.ACTION_PICK);
    intent.setDataAndType(Uri.EMPTY, "application/x-sandblaster-saveable");
    startActivityForResult(intent, SAVE_SNAPSHOT_REQUEST);
  }

  private void clear() {
    presenter.newSandBox();
  }

  private void installDemo() {
    presenter.loadSandBoxFromAsset("snapshot_demo.xml");
  }
}
