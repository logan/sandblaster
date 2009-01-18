package com.loganh.sandblaster;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;


public class Toolbar extends LinearLayout implements Pen.ChangeListener {

  private ToolSelector toolSelector;
  private Button toolButton;
  private SeekBar toolSizeSlider;
  private ImageView toolDemo;
  public Pen pen;

  public Toolbar(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void setPen(Pen pen) {
    this.pen = pen;
    pen.addChangeListener(this);
    selectTool(Pen.DEFAULT_TOOL);
    onChange();
  }

  @Override
  protected void onFinishInflate() {
    toolButton = (Button) findViewById(R.id.tool_button);
    toolSizeSlider = (SeekBar) findViewById(R.id.tool_size_slider);
    toolDemo = (ImageView) findViewById(R.id.tool_demo);

    toolButton.setOnClickListener(new OnClickListener() {
          public void onClick(View v) {
            toggleToolSelector();
          }
        });

    toolSizeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
            float p = (float) progress / seekBar.getMax();
            pen.setRadius(Pen.MIN_RADIUS + p * (Pen.MAX_RADIUS - Pen.MIN_RADIUS));
          }

          public void onStartTrackingTouch(SeekBar seekBar) { }
          public void onStopTrackingTouch(SeekBar seekBar) { }
        });
  }

  private void showToolSelector() {
    toolSelector = new ToolSelector(this);
  }

  private void hideToolSelector() {
    if (toolSelector != null) {
      toolSelector.dismiss();
      toolSelector = null;
    }
  }

  private void toggleToolSelector() {
    if (toolSelector == null) {
      showToolSelector();
    } else {
      hideToolSelector();
    }
  }

  public void selectTool(Pen.Tool tool) {
    pen.selectTool(tool);
    toolButton.setText(tool.toString());
    hideToolSelector();
    updateToolDemo();
  }

  public Pen.Tool getSelectedTool() {
    return pen.getSelectedTool();
  }

  private void updateToolDemo() {
    int w = toolDemo.getWidth();
    int h = toolDemo.getHeight();
    if (w < 1 || h < 1) {
      return;
    }
    BitmapDrawer drawer = new BitmapDrawer(w, h);
    pen.drawLine(drawer, w / 4, h / 2, 3 * w / 4, h / 2);
    toolDemo.setImageBitmap(drawer.bitmap);
  }

  public void onChange() {
    updateToolDemo();
  }

  private class BitmapDrawer implements Pen.Target {

    public Bitmap bitmap;

    public BitmapDrawer(int w, int h) {
      bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    }

    public void drawAt(Element element, int x, int y) {
      bitmap.setPixel(x, y, element == null ? Color.BLACK : element.color);
    }
  }

}
