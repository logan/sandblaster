package com.loganh.sandblaster;

abstract public class Demo {
  static public void install(SandBox sandbox) {
    synchronized(sandbox) {
      sandbox.clear();
      sandbox.line(Element.WALL, pX(0.1f, sandbox), pY(0.1f, sandbox), pX(0.9f, sandbox), pY(0.1f, sandbox));
      sandbox.addSource(Element.SAND1, pX(0.2f, sandbox), (int) sandbox.getHeight() - 1);
      sandbox.addSource(Element.WATER, pX(0.5f, sandbox), (int) sandbox.getHeight() - 1);
      sandbox.addSource(Element.SAND3, pX(0.8f, sandbox), (int) sandbox.getHeight() - 1);
      sandbox.addSource(Element.PLANT, pX(0.33f, sandbox), pY(0.22f, sandbox));
      sandbox.addSource(Element.FIRE, pX(0.67f, sandbox), pY(0.22f, sandbox));
    }
  }

  static private int pX(float perc, SandBox sandbox) {
    return Math.round(perc * sandbox.getWidth());
  }

  static private int pY(float perc, SandBox sandbox) {
    return Math.round(perc * sandbox.getHeight());
  }
}
