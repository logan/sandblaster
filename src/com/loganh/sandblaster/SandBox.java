package com.loganh.sandblaster;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

import android.graphics.Canvas;
import android.graphics.Color;

public class SandBox implements Iterable<SandBox.Particle> {

  static public Element[] ELEMENTS = new Element[]{
      new Element("wall", Color.WHITE, false, 1.0, 0, 0),
      new Element("sand1", Color.YELLOW, true, 0.5, 0, 0),
      new Element("sand2", 0xffcccc33, true, 0.5, 0, 0),
      new Element("sand3", 0xffaaaaaa, true, 0.5, 0, 0),
      new Element("water", Color.BLUE, true, 0.4, 0, 0),
      new Element("plant", Color.GREEN, false, 1.0, 0, 0),
      new Element("fire", Color.RED, true, -1.0, 1.0, 3),
  };

  static {
    addTransmutation("plant", "water", "plant", 0.5);
    addTransmutation("fire", "plant", "fire", 0.75);
  }

  static private Element lookupElement(String name) {
    for (Element element : ELEMENTS) {
      if (element.name.equals(name)) {
        return element;
      }
    }
    return null;
  }

  static private void addTransmutation(String source, String target, String output, double probability) {
    Element sourceElement = lookupElement(source);
    Element targetElement = lookupElement(target);
    Element outputElement = lookupElement(output);
    assert(sourceElement != null);
    assert(targetElement != null);
    assert(outputElement != null);
    sourceElement.addTransmutation(target, outputElement, probability);
  }

  private int width;
  private int height;
  private Particle[][] particles;
  private Map<Integer, Set<Integer> > activeCache;
  private Random random;

  public SandBox(int width, int height) {
    this.width = width;
    this.height = height;
    random = new Random();
    clear();
  }

  synchronized public void clear() {
    particles = new Particle[(int) width][(int) height];
    activeCache = new HashMap();
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        particles[x][y] = new Particle(x, y);
      }
    }
  }

  public int toCanvasX(float x, float canvasWidth) {
    return Math.round(x * canvasWidth / width);
  }

  public int toCanvasY(float y, float canvasHeight) {
    y = height - y - 1;
    return Math.round(y * canvasHeight / height);
  }

  public int fromCanvasX(float x, float canvasWidth) {
    return Math.round(x * width / canvasWidth);
  }

  public int fromCanvasY(float y, float canvasHeight) {
    return (int) height - Math.round(y * height / canvasHeight) - 1;
  }

  public float getWidth() {
    return width;
  }

  public float getHeight() {
    return height;
  }

  synchronized public void setParticle(int x, int y, Element element) {
    if (x >= 0 && y >= 0 && x < width && y < height) {
      particles[x][y].setElement(element);
    }
  }

  private Set<Integer> getRowCache(int y) {
    if (!activeCache.containsKey(y)) {
      activeCache.put(y, new HashSet());
    }
    return activeCache.get(y);
  }

  private void removeActiveParticle(int x, int y) {
    getRowCache(y).remove(x);
  }

  private void addActiveParticle(int x, int y) {
    getRowCache(y).add(x);
  }

  class Particle {
    public int x;
    public int y;
    public int lifetime;
    public Element element;

    public Particle(int x, int y) {
      this.x = x;
      this.y = y;
    }

    public void setElement(Element element) {
      if (this.element != null && element == null) {
        SandBox.this.removeActiveParticle(x, y);
      } else if (this.element == null && element != null) {
        SandBox.this.addActiveParticle(x, y);
      }
      if (this.element != element) {
        this.element = element;
        lifetime = 0;
      }
    }
  }

  class ActiveParticleIterator implements Iterator<Particle> {

    private int y;
    private Iterator<Integer> rowIterator;

    public ActiveParticleIterator() {
      y = -1;
      rowIterator = null;
    }

    public boolean hasNext() {
      while (y < height && (rowIterator == null || !rowIterator.hasNext())) {
        y++;
        rowIterator = SandBox.this.getRowCache(y).iterator();
      }
      return y < height;
    }

    public Particle next() {
      if (hasNext()) {
        int x = rowIterator.next();
        return particles[x][y];
      } else {
        throw new NoSuchElementException();
      }
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  public Iterator<Particle> iterator() {
    return new ActiveParticleIterator();
  }

  synchronized public void update() {
    for (int y = 0; y < height; y++) {
      Integer[] xs = new Integer[0];
      int xoffs = -1;
      xs = getRowCache(y).toArray(xs);
      if (random.nextBoolean()) {
        xoffs = 1;
        for (int i = 0; i < xs.length / 2; i++) {
          int x = xs[i];
          xs[i] = xs[xs.length - i - 1];
          xs[xs.length - i - 1] = x;
        }
      }
      for (int x : xs) {
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
