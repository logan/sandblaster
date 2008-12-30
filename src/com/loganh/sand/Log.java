package com.loganh.sand;

import java.text.MessageFormat;

abstract public class Log {
  static public String TAG = "com.loganh.sand";

  static public String format(String msg, Object[] params) {
    return MessageFormat.format(msg, params);
  }

  static public void i(String msg, Object... params) {
    android.util.Log.i(TAG, format(msg, params));
  }

  static public void e(String msg, Object... params) {
    android.util.Log.e(TAG, format(msg, params));
  }

  static public void e(String msg, Throwable throwable) {
    android.util.Log.e(TAG, msg, throwable);
  }
}
