package com.loganh.sandblaster;

import java.io.*;
import java.util.*;


public class UndoStack implements Recordable {

  public static final int DEFAULT_MAX_BYTES = 500000;

  private int maxBytes;
  private LinkedList<byte[]> stack;
  int totalBytes;

  public UndoStack() {
    this(DEFAULT_MAX_BYTES);
  }

  public UndoStack(int maxBytes) {
    this.maxBytes = maxBytes;
    stack = new LinkedList<byte[]>();
    totalBytes = 0;
  }

  public boolean push(SandBox sandbox) {
    return true;
    /*
    try {
      return push(sandbox.packToBytes());
    } catch (IOException ex) {
      Log.e("failed to serialize sandbox", ex);
      return false;
    }
    */
  }

  public boolean push(byte[] bytes) {
    return true;
    /*
    stack.add(bytes);
    totalBytes += bytes.length;
    while (totalBytes > maxBytes) {
      totalBytes -= stack.removeFirst().length;
    }
    Log.i("stack size: {0} items in {1} bytes", stack.size(), totalBytes);
    return !isEmpty();
    */
  }

  public SandBox pop() {
    return null;
    /*
    if (stack.isEmpty()) {
      return null;
    }
    try {
      byte[] toRemove = stack.removeLast();
      totalBytes -= toRemove.length;
      return SandBox.unpack(toRemove);
    } catch (IOException ex) {
      Log.e("failed to deserialize sandbox", ex);
      return null;
    } finally {
      Log.i("stack size: {0} items in {1} bytes", stack.size(), totalBytes);
    }
    */
  }

  public void clear() {
    totalBytes = 0;
    stack.clear();
  }

  public boolean isEmpty() {
    return stack.isEmpty();
  }

  public void write(DataOutputStream out) throws IOException {
    out.writeByte((byte) stack.size());
    for (byte[] item : stack) {
      out.writeInt(item.length);
      out.write(item);
    }
  }

  static public UndoStack read(DataInputStream in) throws IOException {
    UndoStack stack = new UndoStack();
    byte stackSize = in.readByte();
    while (stackSize-- > 0) {
      byte[] item = new byte[in.readInt()];
      in.read(item);
      stack.push(item);
    }
    return stack;
  }

}
