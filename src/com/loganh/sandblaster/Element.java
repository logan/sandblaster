package com.loganh.sandblaster;

import java.util.*;

import android.graphics.Color;

public enum Element {
  WALL (Color.rgb(0xff, 0xff, 0xff), false, 1.0, 0, 0),
  SAND1 (Color.rgb(0xff, 0xff, 0x00), true, 0.5, 0, 0),
  SAND2 (Color.rgb(0xcc, 0xcc, 0x33), true, 0.5, 0, 0),
  SAND3 (Color.rgb(0xaa, 0xaa, 0xaa), true, 0.5, 0, 0),
  WATER (Color.rgb(0x00, 0x00, 0xff), true, 0.4, 0, 0),
  PLANT (Color.rgb(0x00, 0xff, 0x00), false, Double.MAX_VALUE, 0, 0),
  FIRE (Color.rgb(0xff, 0x00, 0x00), true, 0.1, 0.5, 2);

  // Set up transmutations.
  static {
    // Plant elements turn water elements into plants.
    PLANT.addTransmutation(WATER, PLANT, 0.5);

    // Fire elements turn plant elements into fire.
    FIRE.addTransmutation(PLANT, FIRE, 0.85);
  }

  static private Random random = new Random();

  public int color;
  public boolean mobile;
  public double density;
  public double decayProbability;
  public int lifetime;

  //private List<Transmutation> transmutations;
  public Map<Element, Transmutation> transmutations;

  Element(int color, boolean mobile, double density, double decayProbability, int lifetime) {
    this.color = color;
    this.mobile = mobile;
    this.density = density;
    this.decayProbability = decayProbability;
    this.lifetime = lifetime;
    //transmutations = new ArrayList();
  }

  public void addTransmutation(Element target, Element output, double probability) {
    //transmutations.add(new Transmutation(target, output, probability));
    if (transmutations == null) {
      transmutations = new EnumMap(getClass());
    }
    transmutations.put(target, new Transmutation(target, output, probability));
  }

  public Element maybeTransmutate(Element target) {
    /*
    for (Transmutation transmutation : transmutations) {
      if (transmutation.target == target && random.nextDouble() < transmutation.probability) {
        return transmutation.output;
      }
    }
    */
    if (transmutations != null) {
      Transmutation transmutation = transmutations.get(target);
      if (transmutation != null && random.nextDouble() < transmutation.probability) {
        return transmutation.output;
      }
    }
    return target;
  }

  public class Transmutation {
    public Element target;
    public Element output;
    double probability;

    public Transmutation(Element target, Element output, double probability) {
      this.target = target;
      this.output = output;
      this.probability = probability;
    }
  }
}
