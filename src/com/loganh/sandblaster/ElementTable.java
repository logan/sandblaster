package com.loganh.sandblaster;

import java.io.*;

import java.util.*;


public class ElementTable implements Recordable {
  public Element[] elements;
  private Element.Transmutation[][] transmutations;
  private Random random;

  public ElementTable(Element[] elements) {
    this.elements = elements;
    for (int i = 0; i < elements.length; i++) {
      elements[i].ordinal = i;
    }
    transmutations = new Element.Transmutation[elements.length][elements.length];
    random = new Random();
  }

  public Element resolve(String name) {
    for (Element element : elements) {
      if (element.name.toLowerCase().equals(name.toLowerCase())) {
        return element;
      }
    }
    return null;
  }

  public Element resolve(char id) {
    for (Element element : elements) {
      if (element.id == id) {
        return element;
      }
    }
    return null;
  }

  public Element resolve(byte ordinal) {
    for (Element element : elements) {
      if (element.ordinal == ordinal) {
        return element;
      }
    }
    return null;
  }

  public void addTransmutation(Element agent, Element.Transmutation transmutation) {
    transmutations[agent.ordinal][transmutation.target.ordinal] = transmutation;
    agent.transmutationCount++;
  }

  public Element maybeTransmutate(Element agent, Element target) {
    Element.Transmutation transmutation = transmutations[agent.ordinal][target.ordinal];
    if (transmutation != null && random.nextFloat() < transmutation.probability) {
      return transmutation.pickProduct();
    }
    return target;
  }

  public Iterable<Element.Transmutation> getTransmutations(Element agent) {
    List<Element.Transmutation> transmutationList = new ArrayList<Element.Transmutation>();
    for (int i = 0; i < elements.length; i++) {
      if (transmutations[agent.ordinal][i] != null) {
        transmutationList.add(transmutations[agent.ordinal][i]);
      }
    }
    return transmutationList;
  }

  public void write(DataOutputStream out) throws IOException {
    out.writeByte((byte) elements.length);
    for (Element e : elements) {
      e.write(out);
    }
    for (Element e : elements) {
      if (e.decayProducts == null) {
        out.writeByte(0);
      } else {
        e.decayProducts.write(out);
      }
    }
    for (Element e : elements) {
      for (Element f : elements) {
        Element.Transmutation t = transmutations[e.ordinal][f.ordinal];
        if (t != null) {
          out.writeByte(e.ordinal);
          t.write(out);
        }
      }
    }
    out.writeByte(-1);
  }

  static public ElementTable read(DataInputStream in) throws IOException {
    Element[] elements = new Element[in.readByte()];
    for (int i = 0; i < elements.length; i++) {
      elements[i] = Element.read(in);
    }
    ElementTable elementTable = new ElementTable(elements);
    for (int i = 0; i < elements.length; i++) {
      elements[i].decayProducts = Element.ProductSet.read(in, elementTable);
    }
    Element agent = elementTable.resolve(in.readByte());
    while (agent != null) {
      elementTable.addTransmutation(agent, Element.Transmutation.read(in, elementTable));
      agent = elementTable.resolve(in.readByte());
    }
    return elementTable;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof ElementTable)) {
      return false;
    }
    ElementTable other = (ElementTable) object;
    if (elements.length != other.elements.length) {
      return false;
    }
    for (int i = 0; i < elements.length; i++) {
      if (!elements[i].equals(other.elements[i])) {
        return false;
      }
      for (int j = 0; j < elements.length; j++) {
        if ((transmutations[i][j] == null) != (other.transmutations[i][j] == null)) {
          return false;
        } else if (transmutations[i][j] != null && !transmutations[i][j].equals(other.transmutations[i][j])) {
          return false;
        }
      }
    }
    return true;
  }
}
