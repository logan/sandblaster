package com.loganh.sandblaster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;

public class SandBox {

  private final static int SLIDE_STICKINESS = 1;

  private int canvasWidth;
  private int canvasHeight;
  private float scale;
  private int width;
  private int height;
  public Element[][] elements;
  public int[][] ages;
  public int[][] leftNeighbors;
  public int[][] rightNeighbors;
  public Map<Point, Element> sources;
  private Random random;
  private boolean[] slideDirection;
  private int[] slideRemaining;
  private int[][] lastSet;
  private int[][] lastFloated;
  private int iteration;

  public SandBox(int canvasWidth, int canvasHeight, float scale) {
    this.canvasWidth = canvasWidth;
    this.canvasHeight = canvasHeight;
    this.scale = scale;
    width = (int) (canvasWidth * scale);
    height = (int) (canvasHeight * scale);
    random = new Random();
    clear();
  }

  synchronized public void clear() {
    sources = new HashMap();
    elements = new Element[width][height];
    ages = new int[width][height];
    slideDirection = new boolean[height];
    slideRemaining = new int[height];
    leftNeighbors = new int[width][height];
    rightNeighbors = new int[width][height];
    lastSet = new int[width][height];
    lastFloated = new int[width][height];
    iteration = 0;
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        leftNeighbors[x][y] = -1;
        rightNeighbors[x][y] = width;
      }
    }
  }

  public int toCanvasX(float x) {
    return Math.round(x / scale);
  }

  public int toCanvasY(float y) {
    y = height - y - 1;
    return Math.round(y / scale);
  }

  public int fromCanvasX(float x) {
    return Math.round(x * scale);
  }

  public int fromCanvasY(float y) {
    return height - Math.round(y * scale) - 1;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public float getScale() {
    return scale;
  }

  synchronized public void addSource(Element element, int x, int y) {
    if (x >= 0 && y >= 0 && x < width && y < height) {
      sources.put(new Point(x, y), element);
    }
  }

  synchronized public void removeSource(int x, int y) {
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

  class Source {
    public int x;
    public int y;
    public Element element;

    public Source(int x, int y, Element element) {
      this.x = x;
      this.y = y;
      this.element = element;
    }
  }

  private void setParticle(int x, int y, Element element) {
    if (x >= 0 && y >= 0 && x < width && y < height) {
      lastSet[x][y] = iteration;
      if (element != elements[x][y]) {
        elements[x][y] = element;
        ages[x][y] = 0;
        for (int nx = x - 1; nx > leftNeighbors[x][y]; nx--) {
          if (element == null) {
            rightNeighbors[nx][y] = rightNeighbors[x][y];
          } else {
            rightNeighbors[nx][y] = x;
          }
        }
        for (int nx = x + 1; nx < rightNeighbors[x][y]; nx++) {
          if (element == null) {
            leftNeighbors[nx][y] = leftNeighbors[x][y];
          } else {
            leftNeighbors[nx][y] = x;
          }
        }
        if (element != null) {
          if (leftNeighbors[x][y] >= 0) {
            rightNeighbors[leftNeighbors[x][y]][y] = x;
          }
          if (rightNeighbors[x][y] < width) {
            leftNeighbors[rightNeighbors[x][y]][y] = x;
          }
        }
      }
    }
  }

  synchronized public void line(Element element, int x1, int y1, int x2, int y2) {
    int dx = x1 - x2;
    int dy = y1 - y2;
    int d = Math.max(Math.abs(dx), Math.abs(dy));
    for (int i = 0; i <= d; i++) {
      float x = x2 + ((float) i / d) * dx;
      float y = y2 + ((float) i / d) * dy;
      setParticle(Math.round(x), Math.round(y), element);
    }
  }

  synchronized public void update() {
    for (Point pt : sources.keySet()) {
      setParticle(pt.x, pt.y, sources.get(pt));
    }

    ++iteration;

    for (int y = 0; y < height; y++) {
      int[][] neighbors = rightNeighbors;
      int xoffs = -1;
      int start = 0;
      if (slideRemaining[y]-- < 0) {
        slideRemaining[y] = (int) (random.nextDouble() * SLIDE_STICKINESS);
        slideDirection[y] = random.nextBoolean();
      }
      if (slideDirection[y]) {
        neighbors = leftNeighbors;
        xoffs = 1;
        start = width - 1;
      }
      int x = -2;
      while (true) {
        if (x == -2) {
          x = start;
        } else {
          x = neighbors[x][y];
        }
        if (x < 0 || x >= width) {
          break;
        }
        Element e = elements[x][y];
        if (e == null) {
          continue;
        }
        // Transmutations.
        if (e.transmutations != null && lastSet[x][y] != iteration) {
          for (int xo = -1; xo < 2; xo++) {
            for (int yo = -1; yo < 2; yo++) {
              if ((xo != 0 || yo != 0) && (xo == 0 || yo == 0)) {
                if (x + xo >= 0 && x + xo < width && y + yo >= 0 & y + yo < height
                    && elements[x + xo][y + yo] != null && lastSet[x + xo][y + yo] != iteration) {
                  Element o = e.maybeTransmutate(elements[x + xo][y + yo]);
                  if (o != elements[x + xo][y + yo]) {
                    setParticle(x + xo, y + yo, o);
                  }
                }
              }
            }
          }
        }

        // Decay.
        if (lastSet[x][y] != iteration && e.decayProbability > 0 && random.nextFloat() < e.decayProbability) {
          ages[x][y]++;
          if (ages[x][y] > e.lifetime) {
            setParticle(x, y, e.decayProduct);
            continue;
          }
        }

        if (!e.mobile || lastSet[x][y] == iteration) {
          continue;
        }

        // Horizontal movement.
        //int nx = x + xoffs;
        int nx = x + (random.nextBoolean() ? 1 : -1); 
        if (e.density > 0) {
          // Slide only if blocked below.
          if (y - 1 >= 0 && (!isMobile(x, y - 1) || e.density <= effectiveDensity(x, y - 1))) {
            if (isMobile(nx, y) && e.density > effectiveDensity(nx, y)) {
              swap(x, y, nx, y);
            }
          }
        } else if (e.density < 0) {
          // Slide only if blocked above.
          if (y + 1 < height && (!isMobile(x, y + 1) || e.density >= effectiveDensity(x, y + 1))) {
            if (isMobile(nx, y) && e.density < effectiveDensity(nx, y)) {
              swap(x, y, nx, y);
            }
          }
        }

        // Vertical movement.
        if (isMobile(x, y - 1) && e.density > effectiveDensity(x, y - 1)) {
          swap(x, y, x, y - 1);
          lastFloated[x][y] = iteration;
        } else if (lastFloated[x][y] != iteration && isMobile(x, y + 1) && e.density < effectiveDensity(x, y + 1)) {
          swap(x, y, x, y + 1);
          if (y + 1 < height) {
            lastFloated[x][y + 1] = iteration;
          }
        }
      }
    }
  }

  private void swap(int x1, int y1, int x2, int y2) {
    if (x1 < 0 || y1 < 0 || x1 >= width || y1 >= height) {
      setParticle(x2, y2, null);
      return;
    }
    if (x2 < 0 || y2 < 0 || x2 >= width || y2 >= height) {
      setParticle(x1, y1, null);
      return;
    }
    int l1 = ages[x1][y1];
    int l2 = ages[x2][y2];
    Element e = elements[x1][y1];
    setParticle(x1, y1, elements[x2][y2]);
    setParticle(x2, y2, e);
    ages[x1][y1] = l2;
    ages[x2][y2] = l1;
  }

  private double effectiveDensity(int x, int y) {
    if (x < 0 || y < 0 || x >= width || y >= height || elements[x][y] == null) {
      return 0;
    }
    return elements[x][y].density;
  }

  private boolean isMobile(int x, int y) {
    if (x < 0 || y < 0 || x >= width || y >= height || elements[x][y] == null) {
     return true;
    }
    return lastSet[x][y] != iteration && elements[x][y].mobile;
  }
}
