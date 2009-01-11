package com.loganh.sandblaster;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface Recordable {
  public void write(DataOutputStream out) throws IOException;
}
