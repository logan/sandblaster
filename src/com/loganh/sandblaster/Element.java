package com.loganh.sandblaster;

import java.util.*;


public class Element {

  static public class ProductSet {
    Element[] products;
    float[] weights;
    float totalWeight;
    Random random;

    public ProductSet(Element[] products, float[] weights) {
      this.products = products;
      this.weights = weights;
      for (float weight : weights) {
        totalWeight += weight;
      }
      random = new Random();
    }

    public Element pickProduct() {
      if (products.length == 0) {
        return null;
      } else if (products.length == 1) {
        return products[0];
      }
      float w = random.nextFloat() * totalWeight;
      for (int i = 0; i < products.length; i++) {
        if (w <= weights[i]) {
          return products[i];
        }
        w -= weights[i];
      }
      return products[0];
    }

    public String toString() {
      StringBuffer result = new StringBuffer("[ProductSet");
      for (int i = 0; i < products.length; i++) {
        result.append(" <" + products[i].name + " w=" + weights[i] + ">");
      }
      return result.append("]").toString();
    }
  }

  static public class Transmutation {
    public Element target;
    float probability;
    public ProductSet product;

    public Transmutation(Element target, float probability, ProductSet product) {
      this.target = target;
      this.probability = probability;
      this.product = product;
    }

    public Element pickProduct() {
      if (product == null) {
        return null;
      }
      return product.pickProduct();
    }

    public String toString() {
      return "[Transmutation " + target + " -> " + product + " at p=" + probability + "]";
    }
  }

  public String name;
  public char id;
  public int color;
  public boolean drawable;
  public boolean mobile;
  public float density;
  public float decayProbability;
  public int lifetime;
  public ProductSet decayProducts;
  public int ordinal;
  public int transmutationCount;

  public Element(String name, char id, int color, boolean drawable, boolean mobile, float density) {
    this.name = name;
    this.id = id;
    this.color = color;
    this.drawable = drawable;
    this.mobile = mobile;
    this.density = density;
  }
}
