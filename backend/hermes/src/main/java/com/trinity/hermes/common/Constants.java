package com.trinity.hermes.common;

public final class Constants {

  // Live data freshness window in minutes — data older than this is considered stale
  public static final int LIVE_DATA_WINDOW_MINUTES = 5;

  // Dublin county geographic bounding box
  public static final double DUBLIN_LAT_MIN = 53.15;
  public static final double DUBLIN_LAT_MAX = 53.65;
  public static final double DUBLIN_LON_MIN = -6.55;
  public static final double DUBLIN_LON_MAX = -5.95;

  private Constants() {}
}
