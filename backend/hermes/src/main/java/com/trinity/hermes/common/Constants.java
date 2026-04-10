package com.trinity.hermes.common;

public final class Constants {

  // Live data freshness window in minutes — data older than this is considered stale
  public static final int LIVE_DATA_WINDOW_MINUTES = 5;

  // Dublin county geographic bounding box
  public static final double DUBLIN_LAT_MIN = 53.191981;
  public static final double DUBLIN_LAT_MAX = 53.640914;
  public static final double DUBLIN_LON_MIN = -6.594926;
  public static final double DUBLIN_LON_MAX = -6.106449;

  private Constants() {}
}
