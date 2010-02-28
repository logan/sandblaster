package com.loganh.sandblaster;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;


public class PixelRenderer extends AbsRenderer {

  private static final int VOID_COLOR = Color.rgb(0, 0, 0);
  private static final int PADDING_COLOR = Color.rgb(0x11, 0x11, 0x11);
  private Bitmap bitmap;

  @Override
  protected void draw(Canvas canvas) {
    // Set canvas background.
    Point topLeft = camera.objectToView(new Point(0, sandbox.getHeight() - 1));
    Point bottomRight = camera.objectToView(new Point(sandbox.getWidth(), 0));
    Rect src = new Rect(0, 0, sandbox.getWidth(), sandbox.getHeight());
    Rect dest = new Rect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y);
    Paint paint = new Paint();
    paint.setColor(PADDING_COLOR);
    canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
    paint.setColor(VOID_COLOR);
    canvas.drawRect(dest, paint);

    // Render to bitmap, then project onto canvas.
    setPixels(sandbox);
    canvas.drawBitmap(bitmap, src, dest, paint);
  }

  private void setPixels(SandBox sandbox) {
    synchronized (sandbox) {
      int w = sandbox.getWidth();
      int h = sandbox.getHeight();
      if (bitmap == null || bitmap.getWidth() != w || bitmap.getHeight() != h) {
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
      }
      bitmap.setPixels(sandbox.getPixels(), 0, w, 0, 0, w, h);
    }
  }

  public Bitmap getBitmap(SandBox sandbox) {
    setPixels(sandbox);
    return bitmap;
  }

}
