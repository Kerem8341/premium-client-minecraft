package com.premium.client.scan;

public final class BaseInfo {
    public final int cx, cz, score, storage;
    public final boolean stash;
    public double dist; // refreshed every few ticks

    public BaseInfo(int cx, int cz, int score, int storage, boolean stash) {
        this.cx = cx; this.cz = cz; this.score = score; this.storage = storage; this.stash = stash;
    }

    public double x() { return cx * 16 + 8.0; }
    public double z() { return cz * 16 + 8.0; }
}
