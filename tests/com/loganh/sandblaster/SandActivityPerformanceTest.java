package com.loganh.sandblaster;

import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase;

public class SandActivityPerformanceTest extends ActivityInstrumentationTestCase<SandActivity> {

  public SandActivityPerformanceTest() {
    this("com.loganh.sandblaster", SandActivity.class);
  }

  public SandActivityPerformanceTest(String pkg, Class<SandActivity> activityClass) {
    super(pkg, activityClass);
  }

  public void testPhysicsFrameRate() {
    int W = 256;
    int H = 256;
    int D = 4;
    int N = 300;
    int T = N * 250;  // goal = 200ms / iteration

    SandBox sandbox = new SandBox(W, H);
    sandbox.elementTable = Utils.getTestElementTable();
    Element wall = sandbox.elementTable.resolve("Wall");
    Element fire = sandbox.elementTable.resolve("Fire");
    Element plant = sandbox.elementTable.resolve("Plant");
    Element water = sandbox.elementTable.resolve("Water");
    sandbox.line(wall, 0, H / 2, W / 2, 0);
    sandbox.line(wall, W / 2, 0, W - 1, H / 2);
    sandbox.addSource(plant, W / 2 - 5, 5);
    sandbox.addSource(fire, W / 2 + 5, 4);
    sandbox.addSource(water, W / 2, H - 1);
    for (int i = 0; i < D; i++) {
      sandbox.line(water, i, 3 * H / 4 - i, W - i - 1, 3 * H / 4 - i);
    }

    long total = 0;
    long worst = 0;
    long best = Long.MAX_VALUE;
    long last = SystemClock.uptimeMillis();
    while (sandbox.iteration < N) {
      sandbox.update();
      long now = SystemClock.uptimeMillis();
      long diff = now - last;
      total += diff;
      worst = Math.max(worst, diff);
      best = Math.min(best, diff);
      last = now;
      if (sandbox.iteration % 10 == 0) {
        Log.i("Finished {0}/{1}", sandbox.iteration, N);
      }
    }

    Log.i("{0} ms", total);
    Log.i("average: {0}ms / iteration", total / (double) N);
    Log.i("best: {0}ms / iteration", best);
    Log.i("worst: {0}ms / iteration", worst);
    assertTrue(total < T);
  }

}
