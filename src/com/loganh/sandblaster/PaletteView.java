package com.loganh.sandblaster;

import java.util.EnumSet;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class PaletteView extends View {

  public static Element[] ELEMENTS = EnumSet.range(Element.WALL, Element.FIRE).toArray(new Element[]{});

  public int selected;
  private String[] labels;
  private Point[] labelPositions;

  public PaletteView(Context context, AttributeSet attrs) {
    super(context, attrs);
    labels = new String[ELEMENTS.length];
    labelPositions = new Point[ELEMENTS.length];
  }

  @Override
  protected void onDraw(Canvas canvas) {
    int elemWidth = (int) (getWidth() / ELEMENTS.length);
    int offset = 0;
    Paint paint = new Paint();
    for (Element element : ELEMENTS) {
      Rect rect = new Rect(offset * elemWidth + 2, 2, (offset + 1) * elemWidth - 2, (int) getHeight() - 2);
      paint.setColor(element.color);
      paint.setStyle(Paint.Style.FILL);
      canvas.drawRect(rect, paint);
      if (offset == selected) {
        paint.setColor(Color.WHITE);
      } else {
        paint.setColor(Color.BLACK);
      }
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(2);
      canvas.drawRect(rect, paint);
      drawLabel(offset, canvas, rect, element);
      offset++;
    }
  }

  private void drawLabel(int offset, Canvas canvas, Rect button, Element element) {
    Paint paint = new Paint();
    if (labels[offset] == null) {
      String capName = element.toString();
      String label = new StringBuffer().append(capName.charAt(0)).append(capName.substring(1).toLowerCase()).toString();
      Rect bounds = new Rect();
      paint.getTextBounds(label, 0, label.length(), bounds);
      int left = Math.max(button.left, button.centerX() - bounds.centerX());
      int top = Math.max(button.top, button.centerY() - bounds.centerY());
      labels[offset] = label;
      labelPositions[offset] = new Point(left, top);
    }
    canvas.drawText(labels[offset], labelPositions[offset].x, labelPositions[offset].y, paint);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      int elemWidth = (int) (getWidth() / ELEMENTS.length);
      selected = (int) (event.getX() / elemWidth);
      Log.i("selected now {0}", selected);
      invalidate();
      return true;
    }
    return false;
  }

  public Element getElement() {
    return ELEMENTS[selected % ELEMENTS.length];
  }
}
