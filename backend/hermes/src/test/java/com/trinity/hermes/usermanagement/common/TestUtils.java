package com.trinity.hermes.usermanagement.common;

import java.security.SecureRandom;
import java.util.Base64;

public class TestUtils {

  private static final SecureRandom RNG = new SecureRandom();

  public static String randomPassword() {
    String tail = Base64.getUrlEncoder().withoutPadding().encodeToString(RNG.generateSeed(12));
    return "Aa1!" + tail;
  }
}
