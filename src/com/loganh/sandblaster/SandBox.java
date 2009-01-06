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

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;

public class SandBox {

  public final static int DEFAULT_WIDTH = 120;
  public final static int DEFAULT_HEIGHT = 160;

  // Particles.

  // Table of elements.
  public ElementTable elementTable;

  // Element of the particle at (x, y).
  public Element[][] elements;

  // Age of the particle at (x, y).
  public int[][] ages;

  // A doubly-linked list of particles for each row. The nearest neighbor to
  // the left of (x, y) is at (leftNeighbors[x][y], y), if there is one.
  public int[][] leftNeighbors;
  public int[][] rightNeighbors;

  // Points where particles are continuously emitted.
  public Map<Point, Element> sources;

  // Rendering.

  // TODO: move to renderer
  public Bitmap bitmap;

  // Circular buffer of particle coordinates that need to be redrawn.
  public int[] dirtyPixels;

  // Position in dirtyPixels to the right of the last updated particle.
  public int lastCleanIndex;

  // Position in dirtyPixels to insert the next updated particle.
  public int lastDirtyIndex;

  // Iterating.
  public boolean playing;

  // Keep track of which iteration certain events occurred in for each particle.
  private int iteration;
  private int[][] lastSet;
  private int[][] lastChange;
  private int[][] lastFloated;

  private Random random;

  // Dimensions.
  private int width;
  private int height;

  public SandBox() {
    this(DEFAULT_WIDTH, DEFAULT_HEIGHT);
  }

  public SandBox(int width, int height) {
    this.width = width;
    this.height = height;
    random = new Random();
    clear();
  }

  synchronized public void clear() {
    sources = new HashMap();
    elements = new Element[width][height];
    ages = new int[width][height];
    leftNeighbors = new int[width][height];
    rightNeighbors = new int[width][height];
    lastSet = new int[width][height];
    lastChange = new int[width][height];
    lastFloated = new int[width][height];
    iteration = -1;
    dirtyPixels = new int[width * height];
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        leftNeighbors[x][y] = -1;
        rightNeighbors[x][y] = width;
      }
    }
    if (bitmap != null) {
      bitmap.eraseColor(Color.BLACK);
    } else {
      bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
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

  public void setParticle(int x, int y, Element element) {
    if (x >= 0 && y >= 0 && x < width && y < height) {
      lastSet[x][y] = iteration;
      if (element != elements[x][y]) {
        if (lastChange[x][y] != iteration) {
          dirtyPixels[lastDirtyIndex] = y * width + x;
          lastDirtyIndex = (lastDirtyIndex + 1) % dirtyPixels.length;
        }
        elements[x][y] = element;
        ages[x][y] = 0;
        lastChange[x][y] = iteration;
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
      int x = Math.round(x2 + ((float) i / d) * dx);
      int y = Math.round(y2 + ((float) i / d) * dy);
      setParticle(x, y, element);
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
      if (random.nextBoolean()) {
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
        if (e.transmutationCount > 0 && lastSet[x][y] != iteration) {
          for (int xo = -1; xo < 2; xo++) {
            for (int yo = -1; yo < 2; yo++) {
              //if ((xo != 0 || yo != 0) && (xo == 0 || yo == 0)) {
              if (xo != 0 || yo != 0) {
                if (x + xo >= 0 && x + xo < width && y + yo >= 0 & y + yo < height
                    && elements[x + xo][y + yo] != null && lastSet[x + xo][y + yo] != iteration) {
                  Element o = elementTable.maybeTransmutate(e, elements[x + xo][y + yo]);
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
            setParticle(x, y, e.decayProducts == null ? null : e.decayProducts.pickProduct());
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
            float nd = effectiveDensity(nx, y);
            if (isMobile(nx, y) && e.density > nd) {
              if (nd == 0 || random.nextFloat() < e.density - nd) {
                swap(x, y, nx, y);
              }
            }
          }
        } else if (e.density < 0) {
          // Slide only if blocked above.
          if (y + 1 < height && (!isMobile(x, y + 1) || e.density >= effectiveDensity(x, y + 1))) {
            float nd = effectiveDensity(nx, y);
            if (isMobile(nx, y) && e.density < nd) {
              if (nd == 0 || random.nextFloat() < nd - e.density) {
                swap(x, y, nx, y);
              }
            }
          }
        }

        // Vertical movement.
        float nd = effectiveDensity(x, y - 1);
        if (isMobile(x, y - 1) && e.density > nd) {
          if (nd == 0 || random.nextFloat() < e.density - nd) {
            swap(x, y, x, y - 1);
            lastFloated[x][y] = iteration;
          }
        } else {
          nd = effectiveDensity(x, y + 1);
          if (lastFloated[x][y] != iteration && isMobile(x, y + 1) && e.density < nd) {
            if (nd == 0 || random.nextFloat() < nd - e.density) {
              swap(x, y, x, y + 1);
              if (y + 1 < height) {
                lastFloated[x][y + 1] = iteration;
              }
            }
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

  private float effectiveDensity(int x, int y) {
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
