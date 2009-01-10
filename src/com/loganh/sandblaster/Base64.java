package com.loganh.sandblaster;

abstract public class Base64 {

  protected static String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
  protected static int DEFAULT_LINE_LENGTH = 76;

  static public String encode(byte[] data, int lineLength) {
    StringBuffer buffer = new StringBuffer(data.length * 4 / 3 + data.length / 57);
    int colCounter = 0;
    int word = 0;
    for (int i = 0; i < data.length; i++) {
      word = (word << 8) | (data[i] & 0xff);
      if (i % 3 == 2) {
        if (colCounter + 4 > lineLength) {
          buffer.append('\n');
          colCounter = 0;
        }
        buffer.append(ALPHABET.charAt((word >> 18) & 0x3f))
            .append(ALPHABET.charAt((word >> 12) & 0x3f))
            .append(ALPHABET.charAt((word >> 6) & 0x3f))
            .append(ALPHABET.charAt(word & 0x3f));
        colCounter += 4;
        word = 0;
      }
    }
    if (data.length % 3 != 0) {
      if (colCounter + 4 > lineLength) {
        buffer.append('\n');
      }
      word <<= 8 * (3 - data.length % 3);
      buffer.append(ALPHABET.charAt((word >> 18) & 0x3f))
          .append(ALPHABET.charAt((word >> 12) & 0x3f));
      if (data.length % 3 == 2) {
        buffer.append(ALPHABET.charAt((word >> 6) & 0x3f)).append("=");
      } else {
        buffer.append("==");
      }
    }
    return buffer.toString();
  }

  static public String encode(byte[] data) {
    return encode(data, DEFAULT_LINE_LENGTH);
  }

  static public String encode(String data, int lineLength) {
    return encode(data.getBytes(), lineLength);
  }

  static public String encode(String data) {
    return encode(data, DEFAULT_LINE_LENGTH);
  }

  static public byte[] decode(String encodedData) {
    byte[] data = new byte[encodedData.length() * 3 / 4];
    int dc = 0;
    int[] buffer = new int[4];
    int bc = 0;
    for (byte c : encodedData.getBytes()) {
      if (c == '=') {
        buffer[bc++] = -1;
      } else {
        int i = ALPHABET.indexOf(c);
        if (i >= 0) {
          buffer[bc++] = i;
        }
      }
      if (bc == 4) {
        if (buffer[0] != -1 && buffer[1] != -1) {
          data[dc++] = (byte) ((buffer[0] << 2) | (buffer[1] >> 4));
          if (buffer[2] != -1) {
            data[dc++] = (byte) (((buffer[1] << 4) & 0xf0) | (buffer[2] >> 2));
            if (buffer[3] != -1) {
              data[dc++] = (byte) (((buffer[2] << 6) & 0xc0) | buffer[3]);
            }
          }
        }
        bc = 0;
      }
    }
    if (dc != data.length) {
      byte[] tmp = new byte[dc];
      System.arraycopy(data, 0, tmp, 0, dc);
      data = tmp;
    }
    return data;
  }

  static public String decodeToString(String encodedData) {
    return new String(decode(encodedData));
  }
}
