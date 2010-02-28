package com.loganh.sandblaster;

import java.io.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import android.graphics.Point;

abstract public class BaseSandBox {

  public final static int DEFAULT_WIDTH = 120;
  public final static int DEFAULT_HEIGHT = 160;
  public final static float SERIALIZATION_VERSION = 1.6f;
  public final static float SOURCE_PROBABILITY = 0.4f;

  public final static int[][] NEIGHBORS = {
    { 0, 1 },
    { 1, 1 },
    { 1, 0 },
    { 1, -1 },
    { 0, -1 },
    { -1, -1 },
    { -1, 0 },
    { -1, 1 },
  };

  static class Source {
    public int x;
    public int y;
    public Element element;

    public Source(int x, int y, Element element) {
      this.x = x;
      this.y = y;
      this.element = element;
    }
  }

  // Element of the particle at (x, y).
  Element[][] elements;

  // Exported snapshot of particle colors.
  int[] pixels;

  // Table of elements.
  public ElementTable elementTable;

  // Points where particles are continuously emitted.
  protected HashMap<Point, Element> sources;

  // Iterating.
  public boolean playing;

  // Dimensions.
  protected int width;
  protected int height;

  // Age of the particle at (x, y).
  protected int[][] ages;

  // Keep track of which iteration certain events occurred in for each particle.
  int iteration;
  protected int[][] lastSet;
  protected int[][] lastChange;
  protected int[][] lastFloated;

  public BaseSandBox() {
    this(DEFAULT_WIDTH, DEFAULT_HEIGHT);
  }

  public BaseSandBox(int width, int height) {
    this.width = width;
    this.height = height;
    clear();
  }

  synchronized public void clear() {
    sources = new HashMap<Point, Element>();
    elements = new Element[width][height];
    ages = new int[width][height];
    lastSet = new int[width][height];
    lastChange = new int[width][height];
    lastFloated = new int[width][height];
    iteration = -1;
    pixels = new int[width * height];
    sources = new HashMap<Point, Element>();
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  synchronized public void addSource(Element element, int x, int y) {
    if (element == null) {
      removeSource(x, y);
      return;
    }
    if (x >= 0 && y >= 0 && x < width && y < height) {
      sources.put(new Point(x, y), element);
    }
  }

  synchronized public void removeSource(int x, int y) {
    // TODO: ndk hack!
    if (sources == null) { return; }
    sources.remove(new Point(x, y));
  }

  synchronized public Source[] getSources() {
    Source[] result = new Source[sources.size()];
    int i = 0;
    for (Point pt : sources.keySet()) {
      result[i++] = new Source(pt.x, pt.y, sources.get(pt));
    }
    return result;
  }

  int[] getPixels() {
    return pixels;
  }

  public void setParticle(int x, int y, Element element, int radius) {
    setParticle(x, y, element, radius, 0.4f);
  }

  abstract public void setParticle(int x, int y, Element element, int radius, float prob);
  abstract public void setParticle(int x, int y, Element element);
  abstract public void line(Element element, int radius, int x1, int y1, int x2, int y2);
  abstract public void line(Element element, int x1, int y1, int x2, int y2);
  abstract public void update();
}
