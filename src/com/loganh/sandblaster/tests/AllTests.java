package com.loganh.sandblaster;

import junit.framework.Test;
import junit.framework.TestSuite;

import android.test.suitebuilder.TestSuiteBuilder;


public class AllTests {

  public static Test suite() {
    return new TestSuiteBuilder(AllTests.class).includeAllPackagesUnderHere().build();
  }

}
