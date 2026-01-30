package com.trinity.hermes.common.logging;

public class LogSanitizer {
  public static String sanitizeLog(String input) {
    if (input == null) return null;
    return input.replace('\n', '_').replace('\r', '_');
  }

  public static Object sanitizeLog(Object input) {
    if (input == null) return null;
    return sanitizeLog(input.toString());
  }
}
