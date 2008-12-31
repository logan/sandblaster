package com.loganh.sandblaster;

import java.util.EnumSet;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class PaletteView extends View {

  public static Element[] ELEMENTS = EnumSet.range(Element.WALL, Element.FIRE).toArray(new Element[]{});

  public int selected;

  public PaletteView(Context context, AttributeSet attrs) {
    super(context, attrs);
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
      offset++;
    }
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
    return ELEMENTS[selected];
  }
}
