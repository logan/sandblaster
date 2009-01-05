package com.loganh.sandblaster;

import java.util.ArrayList;

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

  public Element[] elements;
  public int selected;
  private String[] labels;
  private Point[] labelPositions;

  public PaletteView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void setElementTable(ElementTable elementTable) {
    ArrayList<Element> elementList = new ArrayList();
    for (Element element : elementTable.elements) {
      if (element.drawable) {
        elementList.add(element);
      }
    }
    elements = elementList.toArray(new Element[]{});
    labelPositions = new Point[elements.length];
    invalidate();
  }

  @Override
  protected void onDraw(Canvas canvas) {
    if (elements == null) {
      return;
    }
    int elemWidth = (int) (getWidth() / elements.length);
    int offset = 0;
    Paint paint = new Paint();
    for (Element element : elements) {
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
    if (labelPositions[offset] == null) {
      String capName = element.toString();
      String label = elements[offset].name;
      Rect bounds = new Rect();
      paint.getTextBounds(label, 0, label.length(), bounds);
      int left = Math.max(button.left, button.centerX() - bounds.centerX());
      int top = Math.max(button.top, button.centerY() - bounds.centerY());
      labelPositions[offset] = new Point(left, top);
    }
    canvas.drawText(elements[offset].name, labelPositions[offset].x, labelPositions[offset].y, paint);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      int elemWidth = (int) (getWidth() / elements.length);
      selected = (int) (event.getX() / elemWidth);
      Log.i("selected now {0}", selected);
      invalidate();
      return true;
    }
    return false;
  }

  public Element getElement() {
    if (elements == null) {
      return null;
    }
    return elements[selected % elements.length];
  }
}
