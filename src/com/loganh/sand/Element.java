package com.loganh.sand;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Element {
  static private Random random = new Random();

  public String name;
  public int color;
  public boolean mobile;
  public double density;
  public double decayProbability;
  public int lifetime;

  private Map<String, Transmutation> transmutations;

  public Element(String name, int color, boolean mobile, double density, double decayProbability, int lifetime) {
    this.name = name;
    this.color = color;
    this.mobile = mobile;
    this.density = density;
    this.decayProbability = decayProbability;
    this.lifetime = lifetime;
    transmutations = new HashMap();
  }

  public void addTransmutation(String target, Element output, double probability) {
    transmutations.put(target, new Transmutation(output, probability));
  }

  public Element maybeTransmutate(Element target) {
    Transmutation transmutation = transmutations.get(target.name);
    if (transmutation != null && random.nextFloat() < transmutation.probability) {
      return transmutation.output;
    }
    return target;
  }

  public class Transmutation {
    public Element output;
    double probability;

    public Transmutation(Element output, double probability) {
      this.output = output;
      this.probability = probability;
    }
  }
}
