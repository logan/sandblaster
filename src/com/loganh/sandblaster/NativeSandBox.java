package com.loganh.sandblaster;

public class NativeSandBox extends SandBox {
  public NativeSandBox(int w, int h) {
    super(w, h);
  }

  public static NativeSandBox read(byte[] data) {
    Log.i("constructing native sandbox");
    NativeSandBox sandbox = new NativeSandBox(200, 200);
    Log.i("telling native sandbox to read from data");
    sandbox.readFromBytes(data);
    return sandbox;
  }

  native synchronized int[] getPixels();
  native synchronized public void clear();
  native synchronized public void setParticle(int x, int y, Element element, int radius);
  native synchronized public void setParticle(int x, int y, Element element, int radius, float prob);
  native synchronized public void setParticle(int x, int y, Element element);
  native synchronized public void line(Element element, int radius, int x1, int y1, int x2, int y2);
  native synchronized public void line(Element element, int x1, int y1, int x2, int y2);
  native synchronized public void update();
  native synchronized public void readFromBytes(byte[] data);

  static {
    System.loadLibrary("sandblaster");
  }
}
