package com.loganh.sandblaster;

import java.io.IOException;

import junit.framework.TestCase;

public class ElementTableTest extends TestCase {

  public void testPacking() throws IOException {
    ElementTable elementTable = Utils.getTestElementTable();
    assertEquals(elementTable, Utils.copy(elementTable));
  }

}
