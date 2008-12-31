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
    leftNeighbors = new int[width][height];
    rightNeighbors = new int[width][height];
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

  synchronized public void setParticle(int x, int y, Element element) {
    if (x >= 0 && y >= 0 && x < width && y < height) {
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

    for (int y = 0; y < height; y++) {
      int[][] neighbors = rightNeighbors;
      int xoffs = -1;
      int start = 0;
      int x = -2;
      if (random.nextBoolean()) {
        neighbors = leftNeighbors;
        start = width - 1;
        xoffs = 1;
      }
      while (x != -1 && x < width) {
        if (x == -2) {
          x = start;
        } else {
          x = neighbors[x][y];
          if (x == -1 || x >= width) {
            break;
          }
        }
        Element e = elements[x][y];
        if (e == null) {
          continue;
        }

        // Transmutations.
        if (e.transmutations != null) {
          for (int xo = -1; xo < 2; xo++) {
            for (int yo = -1; yo < 2; yo++) {
              if ((xo != 0 || yo != 0) && (xo == 0 || yo == 0)) {
                if (x + xo >= 0 && x + xo < width && y + yo >= 0 & y + yo < height
                    && elements[x + xo][y + yo] != null) {
                  setParticle(x + xo, y + yo, e.maybeTransmutate(elements[x + xo][y + yo]));
                }
              }
            }
          }
        }
          

        if (e.mobile) {

          // Decay.
          if (e.decayProbability > 0 && random.nextFloat() < e.decayProbability) {
            ages[x][y]++;
            if (ages[x][y] > e.lifetime) {
              setParticle(x, y, null);
              continue;
            }
          }

          // Free-fall.
          if (y == 0) {
            // Special case: at bottom of screen, particles will always drop off
            setParticle(x, y, null);
            continue;
          }
          if (y > 0 && elements[x][y - 1] == null) {
            setParticle(x, y - 1, e);
            ages[x][y - 1] = ages[x][y];
            setParticle(x, y, null);
            continue;
          }

          // Sliding due to direct pressure.
          if (y < height - 1 && elements[x][y + 1] != null && elements[x][y + 1].mobile) {
            int nx = x + xoffs;
            if (nx < 0 || nx >= width) {
              // Special case: at edge of screen, particles will always drop off
              setParticle(x, y, null);
              continue;
            } else if (elements[nx][y] == null) {
              setParticle(nx, y, e);
              ages[nx][y] = ages[x][y];
              setParticle(x, y, null);
              continue;
            }
          }

          // Diagonal sliding.
          if (y > 0) {
            int nx = x + xoffs;
            if (nx >= 0 && nx < width && elements[nx][y] == null && elements[nx][y - 1] == null) {
              setParticle(nx, y - 1, e);
              ages[nx][y - 1] = ages[x][y];
              setParticle(x, y, null);
              continue;
            }
          }

          // Sinking.
          if (y > 0 && elements[x][y - 1] != null && elements[x][y - 1].density < e.density) {
            int lifetime = ages[x][y];
            setParticle(x, y, elements[x][y - 1]);
            ages[x][y] = ages[x][y - 1];
            setParticle(x, y - 1, e);
            ages[x][y - 1] = lifetime;
            continue;
          }
        }
      }
    }
  }
}
