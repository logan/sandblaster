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

public class SandBox implements Iterable<SandBox.Particle> {

  private int canvasWidth;
  private int canvasHeight;
  private float scale;
  private int width;
  private int height;
  private Particle[][] particles;
  private Map<Point, Element> sources;
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
    particles = new Particle[(int) width][(int) height];
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        particles[x][y] = new Particle(x, y);
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

  public float getWidth() {
    return width;
  }

  public float getHeight() {
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
      particles[x][y].setElement(element);
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

  class Particle {
    public int x;
    public int y;
    public int canvasX;
    public int canvasY;
    public int lifetime;
    public Element element;

    public Particle(int x, int y) {
      this.x = x;
      this.y = y;
      canvasX = toCanvasX(x);
      canvasY = toCanvasY(y);
    }

    public void setElement(Element element) {
      if (this.element != element) {
        this.element = element;
        lifetime = 0;
      }
    }
  }

  synchronized public Iterator<Particle> iterator() {
    List result = new ArrayList();
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (particles[x][y].element != null) {
          result.add(particles[x][y]);
        }
      }
    }
    return result.iterator();
  }

  synchronized public void update() {
    for (Point pt : sources.keySet()) {
      particles[pt.x][pt.y].setElement(sources.get(pt));
    }

    for (int y = 0; y < height; y++) {
      int[] xs = new int[width];
      int nxs = 0;
      int xoffs = -1;
      for (int x = 0; x < width; x++) {
        if (particles[x][y].element != null) {
          xs[nxs++] = x;
        }
      }
      if (random.nextBoolean()) {
        xoffs = 1;
        for (int i = 0; i < nxs / 2; i++) {
          int x = xs[i];
          xs[i] = xs[nxs - i - 1];
          xs[nxs - i - 1] = x;
        }
      }
      for (int i = 0; i < nxs; i++) {
        int x = xs[i];
        Particle p = particles[x][y];

        // Transmutations.
        for (int xo = -1; xo < 2; xo++) {
          for (int yo = -1; yo < 2; yo++) {
            if ((xo != 0 || yo != 0) && (xo == 0 || yo == 0)) {
              if (x + xo >= 0 && x + xo < width && y + yo >= 0 & y + yo < height
                  && particles[x + xo][y + yo].element != null) {
                Element element = p.element.maybeTransmutate(particles[x + xo][y + yo].element);
                particles[x + xo][y + yo].setElement(element);
              }
            }
          }
        }

        if (p.element.mobile) {

          // Decay.
          if (random.nextFloat() < p.element.decayProbability) {
            p.lifetime++;
            if (p.lifetime > p.element.lifetime) {
              p.setElement(null);
              continue;
            }
          }

          // Special case: at bottom of screen, particles will always drop off
          if (y == 0) {
            p.setElement(null);
            continue;
          }
          if (y > 0 && particles[x][y - 1].element == null) {
            particles[x][y - 1].setElement(p.element);
            particles[x][y - 1].lifetime = p.lifetime;
            p.setElement(null);
            continue;
          }
          if (y < height - 1 && particles[x][y + 1].element != null
              && particles[x][y + 1].element.mobile) {
            int nx = x + xoffs;
            // Special case: at edge of screen, particles will always drop off
            if (nx < 0 || nx >= width) {
              p.setElement(null);
              continue;
            } else if (particles[nx][y].element == null) {
              particles[nx][y].setElement(p.element);
              particles[nx][y].lifetime = p.lifetime;
              p.setElement(null);
              continue;
            }
          }
          if (y > 0 && particles[x][y - 1].element != null
              && particles[x][y - 1].element.density < p.element.density) {
            // TODO: maintain particle properties
            Element e = p.element;
            int lifetime = p.lifetime;
            p.setElement(particles[x][y - 1].element);
            p.lifetime = particles[x][y - 1].lifetime;
            particles[x][y - 1].setElement(e);
            particles[x][y - 1].lifetime = lifetime;
            continue;
          }
        }
      }
    }
  }
}
