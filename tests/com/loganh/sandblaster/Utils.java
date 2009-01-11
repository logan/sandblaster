package com.loganh.sandblaster;

import java.io.*;

abstract public class Utils {

  static public Element copy(Element element) throws IOException {
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    element.write(new DataOutputStream(outStream));
    ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
    return Element.read(new DataInputStream(inStream));
  }

  static public ElementTable copy(ElementTable elementTable) throws IOException {
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    elementTable.write(new DataOutputStream(outStream));
    ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
    return ElementTable.read(new DataInputStream(inStream));
  }

  static public SandBox copy(SandBox sandbox) throws IOException {
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    sandbox.write(new DataOutputStream(outStream));
    ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
    return SandBox.read(new DataInputStream(inStream));
  }

  static public Element.ProductSet copy(Element.ProductSet productSet, ElementTable elementTable) throws IOException {
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    productSet.write(new DataOutputStream(outStream));
    ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
    return Element.ProductSet.read(new DataInputStream(inStream), elementTable);
  }

  static public Element.Transmutation copy(Element.Transmutation transmutation, ElementTable elementTable) throws IOException {
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    transmutation.write(new DataOutputStream(outStream));
    ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
    return Element.Transmutation.read(new DataInputStream(inStream), elementTable);
  }

  static public ElementTable getTestElementTable() {
    Element wall = new Element("Wall", 'W', 0xeeeeee, true, false, 0);
    Element water = new Element("Water", 'A', 0x0000ff, true, true, 0.8f);
    Element fire = new Element("Fire", 'F', 0xff0000, true, true, -1, 0.5f, 3);
    Element smoke = new Element("Smoke", 'S', 0xaaaaaa, false, true, -0.5f, 0.1f, 10);
    Element plant = new Element("Plant", 'P', 0x00ff00, true, false, 0, 0.02f, 20);
    ElementTable elementTable = new ElementTable(new Element[]{wall, water, fire, smoke, plant});
    fire.decayProducts = new Element.ProductSet(new Element[]{smoke, null}, new float[]{1, 5});
    plant.decayProducts = new Element.ProductSet(new Element[]{fire, null}, new float[]{1, 99});
    Element.ProductSet plantBurn = new Element.ProductSet(new Element[]{smoke}, new float[]{1});
    Element.ProductSet plantGrow = new Element.ProductSet(new Element[]{plant}, new float[]{1});
    elementTable.addTransmutation(fire, new Element.Transmutation(plant, 0.8f, plantBurn));
    elementTable.addTransmutation(plant, new Element.Transmutation(water, 0.3f, plantGrow));
    return elementTable;
  }

}
