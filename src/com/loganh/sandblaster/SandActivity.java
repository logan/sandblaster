package com.loganh.sandblaster;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

public class SandActivity extends Activity {
  static final private int CLEAR = 0;

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
    menu.add(0, CLEAR, 0, R.string.menu_clear);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case CLEAR:
        view.clear();
        return true;
    }
    return false;
  }
}
