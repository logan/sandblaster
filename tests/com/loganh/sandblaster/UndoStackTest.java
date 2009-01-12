package com.loganh.sandblaster;

import junit.framework.TestCase;


public class UndoStackTest extends TestCase {

  public void testOverflow() {
    UndoStack stack = new UndoStack(10);
    assertTrue(stack.push(new byte[10]));
    assertEquals(10, stack.totalBytes);
    assertFalse(stack.isEmpty());

    assertFalse(stack.push(new byte[11]));
    assertEquals(0, stack.totalBytes);
    assertTrue(stack.isEmpty());

    assertTrue(stack.push(new byte[2]));
    assertTrue(stack.push(new byte[2]));
    assertTrue(stack.push(new byte[2]));
    assertEquals(6, stack.totalBytes);
    assertTrue(stack.push(new byte[7]));
    assertEquals(9, stack.totalBytes);
  }

  public void testUnderflow() {
    UndoStack stack = new UndoStack();
    SandBox sandbox = new SandBox(1, 1);
    sandbox.elementTable = Utils.getTestElementTable();
    assertNull(stack.pop());
    assertTrue(stack.push(sandbox));
    assertEquals(sandbox, stack.pop());
    assertNull(stack.pop());
  }

}
