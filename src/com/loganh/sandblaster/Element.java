package com.loganh.sandblaster;

import java.util.*;

import android.graphics.Color;

public enum Element {
  // Palette elements:

  WALL (Color.rgb(0xaa, 0xaa, 0xaa), false, 1.0),
  SAND (Color.rgb(0xff, 0xff, 0x00), true, 0.5),
  SALT (Color.rgb(0xee, 0xee, 0xee), true, 0.5),
  WATER (Color.rgb(0x00, 0x00, 0xff), true, 0.4),
  OIL (Color.rgb(0xcc, 0x33, 0x00), true, 0.3),
  PLANT (Color.rgb(0x00, 0xff, 0x00), false, Double.MAX_VALUE),
  FIRE (Color.rgb(0xff, 0x00, 0x00), true, -0.1),

  // Other elements:
  SALTWATER (Color.rgb(0x88, 0x88, 0xff), true, 0.45),
  DEADPLANT (Color.rgb(0xaa, 0x88, 0x00), true, 0.2);

  // Set up transmutations and decay products.
  static {
    PLANT.addTransmutation(WATER, PLANT, 0.5);
    PLANT.decayInto(DEADPLANT, 0.05, 20);

    FIRE.addTransmutation(PLANT, FIRE, 0.4);
    FIRE.addTransmutation(DEADPLANT, FIRE, 0.8);
    FIRE.addTransmutation(OIL, FIRE, 1.0);
    FIRE.decayInto(null, 0.5, 2);

    WATER.addTransmutation(SALT, SALTWATER, 0.9);
    WATER.addTransmutation(DEADPLANT, PLANT, 0.25);
    WATER.decayInto(SALTWATER, 0.01, 20);

    SALTWATER.addTransmutation(PLANT, DEADPLANT, 0.02);
    SALTWATER.decayInto(SALT, 0.01, 20);

    SALT.addTransmutation(PLANT, DEADPLANT, 0.05);
  }

  static private Random random = new Random();

  public int color;
  public boolean mobile;
  public double density;
  public double decayProbability;
  public Element decayProduct;
  public int lifetime;

  public Map<Element, Transmutation> transmutations;

  Element(int color, boolean mobile, double density) {
    this.color = color;
    this.mobile = mobile;
    this.density = density;
  }

  public void addTransmutation(Element target, Element output, double probability) {
    if (transmutations == null) {
      transmutations = new EnumMap(getClass());
    }
    transmutations.put(target, new Transmutation(target, output, probability));
  }

  public Element maybeTransmutate(Element target) {
    if (transmutations != null) {
      Transmutation transmutation = transmutations.get(target);
      if (transmutation != null && random.nextDouble() < transmutation.probability) {
        return transmutation.output;
      }
    }
    return target;
  }

  public void decayInto(Element decayProduct, double decayProbability, int lifetime) {
    this.decayProduct = decayProduct;
    this.decayProbability = decayProbability;
    this.lifetime = lifetime;
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
