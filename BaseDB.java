package com.premium.client.scan;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Remembers every flagged base per server+dimension and saves them to .minecraft/premium_bases.tsv */
public final class BaseDB {
    private static final Map<String, Map<Long, BaseInfo>> DATA = new HashMap<>();
    private static List<BaseInfo> nearest = List.of();
    private static String ctx = "none";
    private static boolean dirty;

    public static long key(int cx, int cz) { return ((long) cx << 32) | (cz & 0xFFFFFFFFL); }

    public static String context(Minecraft mc) {
        String server = mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "singleplayer";
        String dim = "none";
        if (mc.level != null) {
            String d = mc.level.dimension().toString();
            int i = d.lastIndexOf('/');
            dim = (i >= 0 ? d.substring(i + 1) : d).replace("]", "").trim();
        }
        return (server + "|" + dim).replace('\t', ' ');
    }

    public static Map<Long, BaseInfo> map() { return DATA.computeIfAbsent(ctx, k -> new HashMap<>()); }
    public static void setContext(String c) { ctx = c; }
    public static List<BaseInfo> nearest() { return nearest; }
    public static void markDirty() { dirty = true; }

    public static void clearCurrent() { map().clear(); nearest = List.of(); dirty = true; save(); }

    public static void refreshNearest(Minecraft mc) {
        ctx = context(mc);
        double px = mc.player.getX(), pz = mc.player.getZ();
        List<BaseInfo> all = new ArrayList<>(map().values());
        for (BaseInfo b : all) b.dist = Math.hypot(b.x() - px, b.z() - pz);
        all.sort(Comparator.comparingDouble(b -> b.dist));
        nearest = all.size() > 200 ? new ArrayList<>(all.subList(0, 200)) : all;
    }

    private static Path file() { return FabricLoader.getInstance().getGameDir().resolve("premium_bases.tsv"); }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            for (String line : Files.readAllLines(f)) {
                String[] p = line.split("\t");
                if (p.length < 6) continue;
                try {
                    int cx = Integer.parseInt(p[1]), cz = Integer.parseInt(p[2]);
                    DATA.computeIfAbsent(p[0], k -> new HashMap<>()).put(key(cx, cz),
                            new BaseInfo(cx, cz, Integer.parseInt(p[3]), Integer.parseInt(p[4]), p[5].equals("1")));
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException ignored) {}
    }

    public static void saveIfDirty() { if (dirty) save(); }

    public static void save() {
        dirty = false;
        try {
            List<String> out = new ArrayList<>();
            for (var e : DATA.entrySet())
                for (BaseInfo b : e.getValue().values())
                    out.add(e.getKey() + "\t" + b.cx + "\t" + b.cz + "\t" + b.score + "\t" + b.storage + "\t" + (b.stash ? 1 : 0));
            Files.write(file(), out);
        } catch (IOException ignored) {}
    }
}
