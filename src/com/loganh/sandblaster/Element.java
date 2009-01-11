package com.loganh.sandblaster;

import java.io.*;
import java.util.*;


public class Element implements Recordable {

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
        String name = (products[i] == null) ? "nothing" : products[i].name;
        result.append(" <" + name + " w=" + weights[i] + ">");
      }
      return result.append("]").toString();
    }

    public void write(DataOutputStream out) throws IOException {
      out.writeByte(products.length);
      for (Element e : products) {
        if (e == null) {
          out.writeByte(-1);
        } else {
          out.writeByte(e.ordinal);
        }
      }
      for (float weight : weights) {
        out.writeFloat(weight);
      }
    }

    static public ProductSet read(DataInputStream in, ElementTable elementTable) throws IOException {
      int nProducts = in.readByte();
      if (nProducts == 0) {
        return null;
      }
      Element[] products = new Element[nProducts];
      float[] weights = new float[products.length];
      for (int i = 0; i < nProducts; i++) {
        products[i] = elementTable.resolve(in.readByte());
      }
      for (int i = 0; i < nProducts; i++) {
        weights[i] = in.readFloat();
      }
      return new ProductSet(products, weights);
    }

    @Override
    public boolean equals(Object object) {
      if (!(object instanceof ProductSet)) {
        return false;
      }
      ProductSet other = (ProductSet) object;
      if (products.length != other.products.length || weights.length != other.weights.length) {
        return false;
      }
      for (int i = 0; i < products.length; i++) {
        if ((products[i] == null) != (other.products[i] == null)) {
          return false;
        }
        if (products[i] != null && !products[i].equals(other.products[i])) {
          return false;
        }
        if (weights[i] != other.weights[i]) {
          return false;
        }
      }
      return true;
    }
  }

  static public class Transmutation implements Recordable {
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

    public void write(DataOutputStream out) throws IOException {
      out.writeByte(target.ordinal);
      out.writeFloat(probability);
      product.write(out);
    }

    static public Transmutation read(DataInputStream in, ElementTable elementTable) throws IOException {
      Element target = elementTable.resolve(in.readByte());
      float probability = in.readFloat();
      ProductSet product = ProductSet.read(in, elementTable);
      return new Transmutation(target, probability, product);
    }

    @Override
    public boolean equals(Object object) {
      if (!(object instanceof Transmutation)) {
        return false;
      }
      Transmutation other = (Transmutation) object;
      if (!target.equals(other.target)) {
        return false;
      }
      if ((product == null) != (other.product == null)) {
        return false;
      }
      if (product != null && !product.equals(other.product)) {
        return false;
      }
      return true;
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
    this(name, id, color, drawable, mobile, density, 0, 0);
  }

  public Element(String name, char id, int color, boolean drawable, boolean mobile, float density,
                 float decayProbability, int lifetime) {
    this.name = name;
    this.id = id;
    this.color = color;
    this.drawable = drawable;
    this.mobile = mobile;
    this.density = density;
    this.decayProbability = decayProbability;
    this.lifetime = lifetime;
  }

  public void write(DataOutputStream out) throws IOException {
    out.writeUTF(name);
    out.writeChar(id);
    out.writeInt(color);
    out.writeBoolean(drawable);
    out.writeBoolean(mobile);
    out.writeFloat(density);
    out.writeFloat(decayProbability);
    out.writeInt(lifetime);
  }

  static public Element read(DataInputStream in) throws IOException {
    String name = in.readUTF();
    char id = in.readChar();
    int color = in.readInt();
    boolean drawable = in.readBoolean();
    boolean mobile = in.readBoolean();
    float density = in.readFloat();
    float decayProbability = in.readFloat();
    int lifetime = in.readInt();
    return new Element(name, id, color, drawable, mobile, density, decayProbability, lifetime);
  }

  @Override
  public boolean equals(Object object) {
    Element other = (Element) object;
    if (!name.equals(other.name) || id != other.id || color != other.color
        || drawable != other.drawable || mobile != other.mobile
        || density != other.density || decayProbability != other.decayProbability
        || lifetime != other.lifetime) {
      return false;
    }
    if ((decayProducts == null) != (other.decayProducts == null)) {
      return false;
    }
    if (decayProducts != null && !decayProducts.equals(other.decayProducts)) {
      return false;
    }
    return true;
  }
}
