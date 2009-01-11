package com.loganh.sandblaster;

import java.io.IOException;

import junit.framework.TestCase;

public class ElementTest extends TestCase {

  public void testPacking() throws IOException {
    Element element = new Element("Test", 'T', 0x334455, false, false, 1, 2);
    assertEquals(element, Utils.copy(element));

    element.drawable = true;
    element.mobile = true;
    assertEquals(element, Utils.copy(element));

    element.decayProbability = 0.5f;
    element.lifetime = 2;
    assertEquals(element, Utils.copy(element));
  }

  public void testProductSetPacking() throws IOException {
    ElementTable elementTable = Utils.getTestElementTable();
    Element fire = elementTable.resolve("Fire");
    Element plant = elementTable.resolve("Plant");
    assertEquals(fire.decayProducts, Utils.copy(fire.decayProducts, elementTable));
    assertEquals(plant.decayProducts, Utils.copy(plant.decayProducts, elementTable));
  }

  public void testTransmutationPacking() throws IOException {
    ElementTable elementTable = Utils.getTestElementTable();
    Element fire = elementTable.resolve("Fire");
    for (Element.Transmutation transmutation : elementTable.getTransmutations(fire)) {
      assertEquals(transmutation, Utils.copy(transmutation, elementTable));
    }
  }

}
