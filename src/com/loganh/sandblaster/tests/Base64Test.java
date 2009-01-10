package com.loganh.sandblaster;

import junit.framework.TestCase;


public class Base64Test extends TestCase {

  public void testPadding() {
    assertEquals("", Base64.encode(""));
    assertEquals("ag==", Base64.encode("j"));
    assertEquals("ajA=", Base64.encode("j0"));
    assertEquals("ajAh", Base64.encode("j0!"));
    assertEquals("ajAhIQ==", Base64.encode("j0!!"));
  }

  public void testWrapping() {
    assertEquals("ajAhIQ==", Base64.encode("j0!!", 8));
    assertEquals("ajAhIWow\nISE=", Base64.encode("j0!!j0!!", 8));
    assertEquals("ajAhIWow\nISFqMCEh", Base64.encode("j0!!j0!!j0!!", 8));
    assertEquals("ajAhIWow\nISFqMCEh\najAhIQ==", Base64.encode("j0!!j0!!j0!!j0!!", 8));
  }

  public void testDecoding() {
    assertEquals("j0!!j0!!j0!!j0!!", Base64.decodeToString("ajAhIWow\nISFqMCEh\najAhIQ=="));
  }
}
