package com.loganh.sandblaster;

import java.io.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import android.graphics.Point;

public class SandBox implements Recordable {

  int[] pixels;

  public final static int DEFAULT_WIDTH = 120;
  public final static int DEFAULT_HEIGHT = 160;
  public final static float SERIALIZATION_VERSION = 1.6f;

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

  // Particles.

  // Table of elements.
  public ElementTable elementTable;

  // Element of the particle at (x, y).
  public Element[][] elements;

  // Neighbors of the particle at (x, y). See the NEIGHBORS offset table above.

  // Age of the particle at (x, y).
  public int[][] ages;

  // Points where particles are continuously emitted.
  public HashMap<Point, Element> sources;

  // Iterating.
  public boolean playing;

  // Keep track of which iteration certain events occurred in for each particle.
  int iteration;
  int[][] lastSet;
  int[][] lastChange;
  int[][] lastFloated;

  // Dimensions.
  private int width;
  private int height;

  final static class RNG {
    final static private int SIZE = 1024;
    final static float[] floats = new float[SIZE];
    final static boolean[] bools = new boolean[SIZE];
    static int fptr;
    static int bptr;

    static {
      Random r = new Random();
      for (int i = 0; i < SIZE; i++) {
        floats[i] = r.nextFloat();
        bools[i] = floats[i] < 0.5f;
      }
      bptr = -1;
      fptr = -1;
    }

    final static boolean nextBoolean() {
      if (++bptr >= SIZE) {
        bptr = 0;
      }
      return bools[bptr];
    }

    final static float nextFloat() {
      if (++fptr >= SIZE) {
        fptr = 0;
      }
      return floats[fptr];
    }
  }

  public SandBox() {
    this(DEFAULT_WIDTH, DEFAULT_HEIGHT);
  }

  public SandBox(int width, int height) {
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

  public void setParticle(int x, int y, Element element, int radius) {
    setParticle(x, y, element, radius, 0.4f);
  }

  public void setParticle(int x, int y, Element element, int radius, float prob) {
    int r2 = radius * radius;
    for (int i = -radius; i <= radius; i++) {
      for (int j = -radius; j <= radius; j++) {
        if (i * i + j * j <= r2 && (element == null || !element.mobile || RNG.nextFloat() < prob)) {
          setParticle(x + i, y + j, element);
        }
      }
    }
  }

  public void setParticle(int x, int y, Element element) {
    if (x >= 0 && y >= 0 && x < width && y < height) {
      lastSet[x][y] = iteration;
      if (element != elements[x][y]) {
        elements[x][y] = element;
        ages[x][y] = 0;
        lastChange[x][y] = iteration;
        int ty = height - y - 1;
        pixels[ty * width + x] = element == null ? 0 : element.color;
      }
    }
  }

  synchronized public void line(Element element, int radius, int x1, int y1, int x2, int y2) {
    int dx = x1 - x2;
    int dy = y1 - y2;
    int d = Math.max(Math.abs(dx), Math.abs(dy));
    for (int i = 0; i <= d; i++) {
      int x = Math.round(x2 + ((float) i / d) * dx);
      int y = Math.round(y2 + ((float) i / d) * dy);
      setParticle(x, y, element, radius, 0.1f);
    }
  }

  public void line(Element element, int x1, int y1, int x2, int y2) {
    line(element, 0, x1, y1, x2, y2);
  }

  synchronized public void update() {
    for (Point pt : sources.keySet()) {
      setParticle(pt.x, pt.y, sources.get(pt));
    }

    ++iteration;

    for (int y = 0; y < height; y++) {
      int start = 0;
      int last = width;
      int dir = 1;
      if (RNG.nextBoolean()) {
        start = width - 1;
        last = -1;
        dir = -1;
      }
      for (int x = start; x != last; x += dir) {
        Element e = elements[x][y];
        if (e == null) {
          continue;
        }

        // Vertical movement.
        if (y == 0 && e.density > 0) {
          // Drop off the screen.
          setParticle(x, y, null);
          continue;
        }
        if (y == height - 1 && e.density < 0) {
          // Float off the screen.
          setParticle(x, y, null);
        }

        int curLastSet = lastSet[x][y];

        // Transmutations.
        if (e.transmutationCount > 0 && curLastSet != iteration) {
          for (int i = 0; i < NEIGHBORS.length; i++) {
            int nx = x + NEIGHBORS[i][0];
            int ny = y + NEIGHBORS[i][1];
            if (nx >= 0 && nx < width && ny >= 0 & ny < height && lastSet[nx][ny] != iteration) {
              Element t = elements[nx][ny];
              if (t != null) {
                Element o = elementTable.maybeTransmutate(e, t);
                if (o != elements[nx][ny]) {
                  setParticle(nx, ny, o);
                }
              }
            }
          }
        }

        // Decay.
        if (curLastSet != iteration && e.decayProbability > 0 && RNG.nextFloat() < e.decayProbability) {
          ages[x][y]++;
          if (ages[x][y] > e.lifetime) {
            setParticle(x, y, e.decayProducts == null ? null : e.decayProducts.pickProduct());
            continue;
          }
        }

        if (!e.mobile || curLastSet == iteration) {
          continue;
        }

        // Horizontal movement.
        if (RNG.nextFloat() < e.viscosity) {
          int nx = x + (RNG.nextBoolean() ? 1 : -1); 
          if (e.density > 0) {
            // Slide only if blocked below.

            if (y - 1 >= 0) {
              Element o = elements[x][y - 1];
              if (o != null && (!o.mobile || e.density <= o.density)) {
                Element p = (nx < 0 || nx >= width) ? null : elements[nx][y];
                if (p == null || (p.mobile && e.density > p.density && RNG.nextFloat() < e.density - p.density)) {
                  swap(x, y, nx, y);
                }
              }
            }
          } else if (e.density < 0) {
            // Slide only if blocked above.
            if (y + 1 < height) {
              Element o = elements[x][y + 1];
              if (o != null && (!o.mobile || e.density >= o.density)) {
                Element p = (nx < 0 || nx >= width) ? null : elements[nx][y];
                if (p == null || (p.mobile && e.density < p.density && RNG.nextFloat() < p.density - e.density)) {
                  swap(x, y, nx, y);
                }
              }
            }
          }
        }

        Element o = elements[x][y - 1];
        if ((o == null && e.density > 0) || (o != null && o.mobile && e.density > o.density)) {
          if (o == null || o.density == 0 || RNG.nextFloat() < e.density - o.density) {
            swap(x, y, x, y - 1);
            lastFloated[x][y] = iteration;
          }
          continue;
        }
        if (lastFloated[x][y] != iteration) {
          o = elements[x][y + 1];
          if ((o == null && e.density < 0) || (o != null && o.mobile && e.density < o.density)) {
            if (o == null || o.density == 0 || RNG.nextFloat() < o.density - e.density) {
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

  public byte[] packToBytes() throws IOException {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    write(new DataOutputStream(stream));
    return stream.toByteArray();
  }

  public String pack() throws IOException {
    return Base64.encode(packToBytes());
  }

  static public SandBox unpack(String packData) throws IOException {
    return unpack(Base64.decode(packData));
  }

  static public SandBox unpack(byte[] bytes) throws IOException {
    ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
    return read(new DataInputStream(stream));
  }

  synchronized public void write(DataOutputStream stream) throws IOException {
    stream.writeFloat(SERIALIZATION_VERSION);
    elementTable.write(stream);
    stream.writeShort(width);
    stream.writeShort(height);
    stream.writeInt(iteration);

    stream.writeInt(sources.size());
    for (Source source : getSources()) {
      if (source.element != null) {
        stream.writeShort(source.x);
        stream.writeShort(source.y);
        stream.writeByte(source.element.ordinal);
      }
    }

    for (int y = 0; y < height; y++) {
      int start;
      for (start = 0; start < width && elements[start][y] == null; start++);
      stream.writeShort(start);
      for (int x = start; x < width; ) {
        if (elements[x][y] == null) {
          stream.writeByte((byte) -1);
        } else {
          stream.writeByte((byte) elements[x][y].ordinal);
          stream.writeShort((short) ages[x][y]);
          stream.writeShort((short) (lastSet[x][y] - iteration));
          stream.writeShort((short) (lastChange[x][y] - iteration));
          stream.writeShort((short) (lastFloated[x][y] - iteration));
        }
        do { ++x; } while (x < width && elements[x][y] == null);
        if (x < width) {
          stream.writeShort((short) x);
        } else {
          stream.writeShort(-1);
        }
      }
    }
  }

  static public SandBox read(DataInputStream stream) throws IOException {
    if (stream.readFloat() != SERIALIZATION_VERSION) {
      throw new IOException();
    }

    ElementTable elementTable = ElementTable.read(stream);
    int width = stream.readShort();
    int height = stream.readShort();
    SandBox sandbox = new SandBox(width, height);
    sandbox.elementTable = elementTable;
    sandbox.iteration = stream.readInt();

    int nsources = stream.readInt();
    for (int i = 0; i < nsources; i++) {
      int x = stream.readShort();
      int y = stream.readShort();
      Element element = elementTable.resolve(stream.readByte());
      sandbox.addSource(element, x, y);
    }

    for (int y = 0; y < height; y++) {
      int x = stream.readShort();
      while (x >= 0 && x < width) {
        Element e = elementTable.resolve(stream.readByte());
        if (e != null) {
          sandbox.setParticle(x, y, e);
          sandbox.ages[x][y] = stream.readShort();
          sandbox.lastSet[x][y] = sandbox.iteration + stream.readShort();
          sandbox.lastChange[x][y] = sandbox.iteration + stream.readShort();
          sandbox.lastFloated[x][y] = sandbox.iteration + stream.readShort();
        }
        x = stream.readShort();
      }
    }
    return sandbox;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof SandBox)) {
      return false;
    }
    SandBox other = (SandBox) object;
    if (width != other.width || height != other.height || !elementTable.equals(other.elementTable)
        || iteration != other.iteration || sources.size() != other.sources.size()) {
      Log.i("details mismatch");
      return false;
    }
    if (!sources.equals(other.sources)) {
      Log.i("sources mismatch");
      return false;
    }
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (ages[x][y] != other.ages[x][y] || lastSet[x][y] != other.lastSet[x][y]
            || lastChange[x][y] != other.lastChange[x][y]
            || lastFloated[x][y] != other.lastFloated[x][y]) {
          Log.i("particle state mismatch at {0}, {1}", x, y);
          Log.i(" elements: {0} vs. {1}", elements[x][y], other.elements[x][y]);
          Log.i(" ages: {0} vs. {1}", ages[x][y], other.ages[x][y]);
          Log.i(" lastSet: {0} vs. {1}", lastSet[x][y], other.lastSet[x][y]);
          Log.i(" lastChange: {0} vs. {1}", lastChange[x][y], other.lastChange[x][y]);
          Log.i(" lastFloated: {0} vs. {1}", lastFloated[x][y], other.lastFloated[x][y]);
          return false;
        }
        if ((elements[x][y] == null) != (other.elements[x][y] == null)) {
          Log.i("particle presence mismatch at {0}, {1}", x, y);
          return false;
        }
        if (elements[x][y] != null && !elements[x][y].equals(other.elements[x][y])) {
          Log.i("particle element mismatch at {0}, {1}", x, y);
          return false;
        }
      }
    }
    return true;
  }
}
