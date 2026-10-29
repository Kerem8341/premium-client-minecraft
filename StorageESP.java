package com.premium.client.modules;

import com.premium.client.core.Module;
import com.premium.client.render.Renderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StorageESP extends Module {
    private record Hit(int x, int y, int z, float r, float g, float b) {}
    private final List<Hit> hits = new ArrayList<>();
    private int timer;

    public StorageESP() { super("StorageESP", "Boxes on chests, barrels, shulkers, hoppers, furnaces"); }

    @Override
    public void onTick(Minecraft mc) {
        if (++timer < 10) return;
        timer = 0;
        hits.clear();
        int rd = Math.min(mc.options.renderDistance().get(), 8);
        int px = Mth.floor(mc.player.getX()) >> 4, pz = Mth.floor(mc.player.getZ()) >> 4;
        for (int dx = -rd; dx <= rd; dx++)
            for (int dz = -rd; dz <= rd; dz++) {
                LevelChunk ch = mc.level.getChunkSource().getChunkNow(px + dx, pz + dz);
                if (ch == null) continue;
                for (Map.Entry<BlockPos, BlockEntity> e : ch.getBlockEntities().entrySet()) {
                    String p = BuiltInRegistries.BLOCK.getKey(e.getValue().getBlockState().getBlock()).getPath();
                    float[] c = color(p);
                    if (c == null) continue;
                    BlockPos pos = e.getKey();
                    hits.add(new Hit(pos.getX(), pos.getY(), pos.getZ(), c[0], c[1], c[2]));
                }
            }
    }

    private static float[] color(String p) {
        if (p.endsWith("shulker_box")) return new float[]{1f, 0.3f, 1f};
        if (p.equals("trapped_chest")) return new float[]{1f, 0.25f, 0.25f};
        if (p.equals("chest") || p.equals("barrel")) return new float[]{1f, 0.67f, 0f};
        if (p.equals("ender_chest")) return new float[]{0.65f, 0.1f, 1f};
        if (p.equals("hopper") || p.equals("dropper") || p.equals("dispenser") || p.equals("crafter")) return new float[]{0.7f, 0.7f, 0.7f};
        if (p.contains("furnace") || p.equals("smoker")) return new float[]{1f, 0.4f, 0.3f};
        if (p.equals("beacon")) return new float[]{0.3f, 1f, 1f};
        return null;
    }

    @Override
    public void onRender(Minecraft mc, float delta) {
        for (Hit h : hits)
            Renderer.box(h.x() - 0.002, h.y() - 0.002, h.z() - 0.002, h.x() + 1.002, h.y() + 1.002, h.z() + 1.002, h.r(), h.g(), h.b(), 0.35f);
    }
}
