package com.loganh.sandblaster;

import java.util.*;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;


public class ElementTable {
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
    List<Element.Transmutation> transmutationList = new ArrayList();
    for (int i = 0; i < elements.length; i++) {
      if (transmutations[agent.ordinal][i] != null) {
        transmutationList.add(transmutations[agent.ordinal][i]);
      }
    }
    return transmutationList;
  }
}
