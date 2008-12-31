package com.loganh.sandblaster;

import android.app.Activity;
import android.os.Bundle;
import android.os.Debug;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

public class SandActivity extends Activity {
  static final private int CLEAR = 0;
  static final private int BIGGER = 1;
  static final private int SMALLER = 2;
  static final private int DEMO = 3;
  static final private int TRACE_ON = 4;
  static final private int TRACE_OFF = 5;

  SandView view;
  PaletteView palette;

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
    Log.i("create: {0}, {1}", view, palette);
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.i("resume");
    view.resume();
  }

  @Override
  public void onPause() {
    super.onPause();
    Log.i("pause");
    view.pause();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, CLEAR, 0, R.string.menu_clear).setIcon(R.drawable.clear);
    menu.add(0, BIGGER, 0, R.string.menu_bigger).setIcon(R.drawable.bigger);
    menu.add(0, SMALLER, 0, R.string.menu_smaller).setIcon(R.drawable.smaller);
    menu.add(0, DEMO, 0, R.string.menu_demo);
    menu.add(0, TRACE_ON, 0, R.string.menu_trace_on);
    menu.add(0, TRACE_OFF, 0, R.string.menu_trace_off);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case CLEAR:
        view.clear();
        return true;
      case BIGGER:
        view.makeBigger();
        return true;
      case SMALLER:
        view.makeSmaller();
        return true;
      case DEMO:
        view.installDemo();
        return true;
      case TRACE_ON:
        Debug.startMethodTracing("sand");
        return true;
      case TRACE_OFF:
        Debug.stopMethodTracing();
        return true;
    }
    return false;
  }
}
