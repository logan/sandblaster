package com.loganh.sandblaster;

import junit.framework.TestSuite;

import android.test.suitebuilder.TestSuiteBuilder;


public class AllTests {

  public static TestSuite suite() {
    return new TestSuiteBuilder(AllTests.class).includeAllPackagesUnderHere().build();
  }

}
