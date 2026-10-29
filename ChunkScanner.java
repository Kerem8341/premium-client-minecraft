package com.premium.client.scan;

import com.premium.client.core.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.*;

/**
 * Scores every loaded chunk for "player-made" evidence:
 *  - containers / beds / furnaces / beacons / signs (block entities)
 *  - sections containing artificial blocks (concrete, redstone, pistons, glass, portals ...)
 *  - entities (item frames, armor stands, storage minecarts/boats, named mobs, packed mob pens)
 * Natural structures and villages are down-weighted. Flagged chunks are remembered (BaseDB),
 * so they stay on your radar/ESP even after they unload.
 */
public final class ChunkScanner {
    private static final class Raw { int cx, cz, storage, other, sec, ent; boolean village; }

    private static final Map<Long, Raw> pass = new HashMap<>();
    private static final ArrayDeque<int[]> queue = new ArrayDeque<>();
    private static final Map<Block, Integer> CAT = new HashMap<>();
    private static String passCtx = "";
    private static int timer = 100;

    public static void requestRescan() { timer = 100; }

    public static void tick(Minecraft mc) {
        if (mc.level == null || mc.player == null) return;
        if (queue.isEmpty()) {
            if (++timer < 100) return;      // new pass every ~5s
            timer = 0;
            refill(mc);
            return;
        }
        int budget = 40;                    // chunks per tick, keeps FPS smooth
        while (budget-- > 0 && !queue.isEmpty()) {
            int[] c = queue.poll();
            scanChunk(mc, c[0], c[1]);
        }
        if (queue.isEmpty()) finish(mc);
    }

    private static void refill(Minecraft mc) {
        pass.clear();
        passCtx = BaseDB.context(mc);
        BaseDB.setContext(passCtx);
        int rd = mc.options.renderDistance().get() + 1;
        int px = Mth.floor(mc.player.getX()) >> 4, pz = Mth.floor(mc.player.getZ()) >> 4;
        for (int dx = -rd; dx <= rd; dx++)
            for (int dz = -rd; dz <= rd; dz++)
                queue.add(new int[]{px + dx, pz + dz});
    }

    private static int cat(Block b) { return CAT.computeIfAbsent(b, ChunkScanner::classify); }

    /** 3 = village hint, 2 = strong player block, 1 = weak player block, 0 = ignore */
    private static int classify(Block b) {
        String p = BuiltInRegistries.BLOCK.getKey(b).getPath();
        if (p.equals("bell") || p.equals("composter") || p.equals("hay_block")) return 3;
        if (p.endsWith("concrete") || p.endsWith("concrete_powder") || p.endsWith("glazed_terracotta")
                || p.endsWith("stained_glass") || p.endsWith("stained_glass_pane") || p.equals("tinted_glass")
                || p.equals("redstone_wire") || p.equals("repeater") || p.equals("comparator") || p.equals("observer")
                || p.equals("piston") || p.equals("sticky_piston") || p.equals("slime_block") || p.equals("honey_block")
                || p.equals("nether_portal") || p.equals("bricks") || p.equals("iron_block") || p.equals("diamond_block")
                || p.equals("emerald_block") || p.equals("netherite_block") || p.equals("redstone_block")
                || p.equals("redstone_lamp") || p.equals("daylight_detector") || p.equals("note_block")
                || p.equals("powered_rail") || p.equals("detector_rail") || p.equals("activator_rail")
                || p.equals("lodestone") || p.equals("respawn_anchor") || p.equals("smooth_quartz") || p.equals("quartz_bricks"))
            return 2;
        if (p.endsWith("glass") || p.endsWith("glass_pane") || p.equals("crafting_table") || p.equals("bookshelf")
                || p.endsWith("wool") || p.equals("smooth_stone") || p.equals("stone_bricks") || p.endsWith("fence_gate")
                || p.equals("ladder") || p.equals("scaffolding") || p.equals("campfire") || p.equals("soul_campfire")
                || p.endsWith("_door"))
            return 1;
        return 0;
    }

    private static void scanChunk(Minecraft mc, int cx, int cz) {
        LevelChunk ch = mc.level.getChunkSource().getChunkNow(cx, cz);
        if (ch == null) return;
        Raw r = new Raw();
        r.cx = cx; r.cz = cz;

        for (Map.Entry<BlockPos, BlockEntity> e : ch.getBlockEntities().entrySet()) {
            Block b = e.getValue().getBlockState().getBlock();
            beScore(r, BuiltInRegistries.BLOCK.getKey(b).getPath());
        }
        r.other = Math.min(r.other, 12);

        for (LevelChunkSection s : ch.getSections()) {
            if (s == null || s.hasOnlyAir()) continue;
            if (s.maybeHas(st -> cat(st.getBlock()) == 3)) r.village = true;
            if (s.maybeHas(st -> cat(st.getBlock()) == 2)) r.sec += 2;
            else if (s.maybeHas(st -> cat(st.getBlock()) == 1)) r.sec += 1;
        }
        pass.put(BaseDB.key(cx, cz), r);
    }

    private static void beScore(Raw r, String p) {
        if (p.equals("chest") || p.equals("trapped_chest") || p.equals("barrel") || p.endsWith("shulker_box")) { r.storage++; return; }
        if (p.equals("beacon")) { r.other += 6; return; }
        if (p.equals("ender_chest") || p.contains("furnace") || p.equals("smoker") || p.endsWith("_bed")
                || p.equals("hopper") || p.equals("dropper") || p.equals("dispenser") || p.equals("crafter")
                || p.equals("enchanting_table") || p.equals("brewing_stand") || p.equals("lectern")
                || p.equals("jukebox") || p.equals("decorated_pot") || p.equals("chiseled_bookshelf")
                || p.endsWith("sign") || p.endsWith("banner"))
            r.other += 1;
    }

    private static int entityPoints(Entity e, String p) {
        if (p.equals("item_frame") || p.equals("glow_item_frame") || p.equals("painting")) return 1;
        if (p.equals("armor_stand") || p.equals("end_crystal")) return 2;
        if (p.endsWith("chest_minecart") || p.equals("hopper_minecart") || p.equals("furnace_minecart")
                || p.endsWith("chest_boat") || p.endsWith("chest_raft")) return 2;
        if (e.hasCustomName()) return 1;
        return 0;
    }

    private static boolean nearVillage(Set<Long> villages, int cx, int cz) {
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++)
                if (villages.contains(BaseDB.key(cx + dx, cz + dz))) return true;
        return false;
    }

    private static void finish(Minecraft mc) {
        String ctx = BaseDB.context(mc);
        if (!ctx.equals(passCtx)) { pass.clear(); return; }   // changed dimension mid-scan

        // entity evidence
        Map<Long, Map<String, Integer>> herd = new HashMap<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player || e instanceof Player) continue;
            long k = BaseDB.key(Mth.floor(e.getX()) >> 4, Mth.floor(e.getZ()) >> 4);
            Raw r = pass.get(k);
            if (r == null) continue;
            String p = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
            r.ent += entityPoints(e, p);
            if (e instanceof LivingEntity) herd.computeIfAbsent(k, x -> new HashMap<>()).merge(p, 1, Integer::sum);
        }

        Set<Long> villages = new HashSet<>();
        for (Raw r : pass.values()) if (r.village) villages.add(BaseDB.key(r.cx, r.cz));

        Map<Long, BaseInfo> db = BaseDB.map();
        List<BaseInfo> fresh = new ArrayList<>();
        boolean changed = false;

        for (Raw r : pass.values()) {
            int herdPts = 0;
            Map<String, Integer> h = herd.get(BaseDB.key(r.cx, r.cz));
            if (h != null) for (int c : h.values()) if (c >= 12) { herdPts = 3; break; }   // mob pen / farm

            int total = Math.min(r.storage * 2, 6) + r.other + r.sec + r.ent + herdPts;
            if (Settings.ignoreVillages && nearVillage(villages, r.cx, r.cz)) total = (int) (total * 0.4);

            boolean stash = r.storage >= Settings.stashThreshold;
            long k = BaseDB.key(r.cx, r.cz);
            BaseInfo old = db.get(k);

            if (total >= Settings.baseThreshold || stash) {
                BaseInfo nb = new BaseInfo(r.cx, r.cz, total, r.storage, stash);
                if (old == null) fresh.add(nb);
                if (old == null || old.score != total || old.storage != r.storage || old.stash != stash) {
                    db.put(k, nb);
                    changed = true;
                }
            } else if (old != null && (r.other + r.sec + r.storage * 2) < Settings.baseThreshold / 2) {
                db.remove(k);   // evidence is gone (base was removed)
                changed = true;
            }
        }
        pass.clear();

        if (changed) BaseDB.markDirty();
        BaseDB.saveIfDirty();
        BaseDB.refreshNearest(mc);

        if (Settings.chatAlerts && !fresh.isEmpty() && mc.player != null) {
            fresh.sort(Comparator.comparingDouble(b -> Math.hypot(b.x() - mc.player.getX(), b.z() - mc.player.getZ())));
            int shown = 0;
            for (BaseInfo b : fresh) {
                if (shown++ >= 3) break;
                mc.player.displayClientMessage(Component.literal("§d[Premium] §f" + (b.stash ? "§dSTASH" : "§eBase") + " §fat §bX " + (int) b.x()
                        + " Z " + (int) b.z() + " §7(score " + b.score + ", " + b.storage + " containers)"), false);
            }
            if (fresh.size() > 3)
                mc.player.displayClientMessage(Component.literal("§d[Premium] §7+" + (fresh.size() - 3) + " more found (see radar)"), false);
        }
    }
}
