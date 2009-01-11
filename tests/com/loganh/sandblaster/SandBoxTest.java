package com.loganh.sandblaster;

import java.io.IOException;

import junit.framework.TestCase;


public class SandBoxTest extends TestCase {

  public void testPacking() throws IOException {
    SandBox sandbox = new SandBox(100, 200);
    sandbox.elementTable = Utils.getTestElementTable();
    assertEquals(sandbox, Utils.copy(sandbox));

    // Add some sources.
    Element fire = sandbox.elementTable.resolve("Fire");
    Element plant = sandbox.elementTable.resolve("Plant");
    Element smoke = sandbox.elementTable.resolve("Smoke");
    sandbox.addSource(fire, 10, 15);
    sandbox.addSource(plant, 20, 25);
    sandbox.addSource(null, 30, 35);
    assertEquals(sandbox, Utils.copy(sandbox));

    // Particle state.
    sandbox.iteration = 1000;
    sandbox.line(fire, 0, 0, 99, 99);
    assertEquals(sandbox, Utils.copy(sandbox));

    sandbox.line(plant, 99, 0, 0, 99);
    assertEquals(sandbox, Utils.copy(sandbox));

    sandbox.ages[20][20] = 10;
    sandbox.lastSet[30][30] = 100;
    sandbox.lastChange[40][40] = 200;
    sandbox.lastFloated[50][50] = 300;
    assertEquals(sandbox, Utils.copy(sandbox));
  }

}
