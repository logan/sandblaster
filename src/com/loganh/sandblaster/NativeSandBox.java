package com.loganh.sandblaster;

public class NativeSandBox extends SandBox {
  public NativeSandBox(int w, int h) {
    super(w, h);
  }

  native int[] getPixels();
  native public void clear();
  native public void setParticle(int x, int y, Element element, int radius);
  native public void setParticle(int x, int y, Element element, int radius, float prob);
  native public void setParticle(int x, int y, Element element);
  native public void line(Element element, int radius, int x1, int y1, int x2, int y2);
  native public void line(Element element, int x1, int y1, int x2, int y2);
  native public void update();

  static {
    System.loadLibrary("sandblaster");
  }
}
