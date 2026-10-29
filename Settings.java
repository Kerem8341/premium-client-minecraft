package com.premium.client.core;

public final class Settings {
    public static int baseThreshold = 7;     // score needed to flag a chunk as a base
    public static int stashThreshold = 6;    // containers per chunk to flag a stash
    public static int radarRange = 64;       // chunks shown on the radar
    public static boolean ignoreVillages = true;
    public static boolean chatAlerts = true;

    public static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
