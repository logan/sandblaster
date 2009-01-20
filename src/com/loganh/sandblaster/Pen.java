package com.loganh.sandblaster;

import java.util.*;


public class Pen {

  public enum Tool {
    SPRAYER,
    PEN,
    SPOUT,
    LINE,
  }

  public interface Target {
    public void drawAt(Element element, int x, int y);
    public void setLineOverlay(Pen pen, int x1, int x2, int y1, int y2);
    public void clearLineOverlay();
  }

  public interface ChangeListener {
    public void onChange();
  }

  final static public Tool DEFAULT_TOOL = Tool.SPRAYER;
  final static public float MIN_RADIUS = 0.25f;
  final static public float MAX_RADIUS = 2;
  final static public float SPRAY_PROBABILITY = 0.4f;

  final static int[][] circleMap = new int[(int) Math.ceil(MAX_RADIUS)][(int) Math.ceil(MAX_RADIUS)];

  static {
    for (int x = 0; x < circleMap.length; x++) {
      for (int y = 0; y < circleMap.length; y++) {
        circleMap[x][y] = (int) Math.round(Math.sqrt(x * x + y * y));
      }
    }
  }
  
  private Tool selectedTool;
  private Element element;
  private Random random;
  private float radius;
  private Set<ChangeListener> changeListeners;
  int lastX;
  int lastY;
  boolean down;

  public Pen() {
    selectedTool = DEFAULT_TOOL;
    radius = MIN_RADIUS;
    random = new Random();
    changeListeners = new HashSet<ChangeListener>();
  }

  public void addChangeListener(ChangeListener listener) {
    changeListeners.add(listener);
  }

  public boolean removeChangeListener(ChangeListener listener) {
    return changeListeners.remove(listener);
  }

  private void notifyChangeListeners() {
    for (ChangeListener listener : changeListeners) {
      listener.onChange();
    }
  }

  public void selectTool(Tool tool) {
    selectedTool = tool;
    notifyChangeListeners();
  }

  public Tool getSelectedTool() {
    return selectedTool;
  }

  public Element getElement() {
    return element;
  }

  public void setRadius(float radius) {
    this.radius = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, radius));
    notifyChangeListeners();
  }

  public float getRadius() {
    return radius;
  }

  public void setElement(Element element) {
    this.element = element;
    notifyChangeListeners();
  }

  public Element getElement(Element element) {
    return element;
  }

  public void press(Target target, int x, int y) {
    down = true;
    lastX = x;
    lastY = y;
    if (selectedTool != Tool.LINE) {
      drawAt(target, x, y);
    }
  }

  public void drag(Target target, int x, int y) {
    if (selectedTool == Tool.LINE) {
      if (down) {
        target.setLineOverlay(this, lastX, lastY, x, y);
      } else {
        press(target, x, y);
      }
    } else if (down) {
      drawLine(target, lastX, lastY, x, y);
      lastX = x;
      lastY = y;
    } else {
      press(target, x, y);
    }
  }

  public void release(Target target, int x, int y) {
    if (down) {
      down = false;
      if (selectedTool == Tool.LINE) {
        target.clearLineOverlay();
        drawLine(target, lastX, lastY, x, y);
      }
    }
  }

  public void drawAt(Target target, int x, int y) {
    if (selectedTool == null) {
      return;
    }
    float prob = 1;
    if (element != null && element.mobile && selectedTool != Tool.PEN) {
      prob = SPRAY_PROBABILITY;
    }
    drawAround(target, x, y, radius, prob);
  }

  public void drawLine(Target target, int x1, int y1, int x2, int y2) {
    int dx = x1 - x2;
    int dy = y1 - y2;
    int d = Math.max(Math.abs(dx), Math.abs(dy));
    for (int i = 0; i < d; i++) {
      int x = Math.round(x2 + ((float) i / d) * dx);
      int y = Math.round(y2 + ((float) i / d) * dy);
      drawAt(target, x, y);
    }
    drawAt(target, x1, y1);
    drawAt(target, x2, y2);
  }

  private void drawAround(Target target, int x, int y, float r, float prob) {
    int ir = Math.round(r);
    for (int i = 0; i < circleMap.length; i++) {
      for (int j = 0; j < circleMap.length; j++) {
        if (circleMap[i][j] <= ir) {
          if (random.nextFloat() < prob) {
            target.drawAt(element, x + i, y + j);
          }
          if (random.nextFloat() < prob) {
            target.drawAt(element, x - i, y + j);
          }
          if (random.nextFloat() < prob) {
            target.drawAt(element, x + i, y - j);
          }
          if (random.nextFloat() < prob) {
            target.drawAt(element, x - i, y - j);
          }
        }
      }
    }
  }

}
