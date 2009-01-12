package com.loganh.sandblaster;

import java.io.*;
import java.util.*;


public class UndoStack implements Recordable {

  public static final int DEFAULT_MAX_BYTES = 150000;

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
    try {
      return push(sandbox.packToBytes());
    } catch (IOException ex) {
      Log.e("failed to serialize sandbox", ex);
      return false;
    }
  }

  public boolean push(byte[] bytes) {
    stack.add(bytes);
    Log.i("adding {0} bytes", bytes.length);
    totalBytes += bytes.length;
    while (totalBytes > maxBytes) {
      Log.i("removing {0} bytes", stack.getFirst().length);
      totalBytes -= stack.removeFirst().length;
    }
    Log.i("stack size: {0} items in {1} bytes", stack.size(), totalBytes);
    return !isEmpty();
  }

  public SandBox pop() {
    if (stack.isEmpty()) {
      return null;
    }
    try {
      return SandBox.unpack(stack.removeLast());
    } catch (IOException ex) {
      Log.e("failed to deserialize sandbox", ex);
      return null;
    } finally {
      Log.i("stack size: {0} items in {1} bytes", stack.size(), totalBytes);
    }
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
    Log.i("stack size: {0}", stackSize);
    while (stackSize-- > 0) {
      byte[] item = new byte[in.readInt()];
      in.read(item);
      stack.push(item);
    }
    return stack;
  }

}
