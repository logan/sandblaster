package com.loganh.sandblaster;

public class NativeSandBox extends SandBox {
  public NativeSandBox(int w, int h) {
    super(w, h);
  }

  native synchronized int[] getPixels();
  native synchronized public void clear();
  native synchronized public void setParticle(int x, int y, Element element, int radius);
  native synchronized public void setParticle(int x, int y, Element element, int radius, float prob);
  native synchronized public void setParticle(int x, int y, Element element);
  native synchronized public void line(Element element, int radius, int x1, int y1, int x2, int y2);
  native synchronized public void line(Element element, int x1, int y1, int x2, int y2);
  native synchronized public void update();

  static {
    System.loadLibrary("sandblaster");
  }
}
