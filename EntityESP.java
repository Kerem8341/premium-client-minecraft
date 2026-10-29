package com.premium.client.modules;

import com.premium.client.core.Module;
import com.premium.client.render.Renderer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

/** Entities that give bases away: armor stands, item frames, storage minecarts/boats, end crystals, named mobs. */
public class EntityESP extends Module {
    private static final Map<EntityType<?>, Integer> CAT = new HashMap<>();

    public EntityESP() { super("EntityESP", "Highlights armor stands, item frames, storage carts/boats, named mobs"); }

    private static int cat(EntityType<?> t) {
        return CAT.computeIfAbsent(t, k -> {
            String p = BuiltInRegistries.ENTITY_TYPE.getKey(k).getPath();
            if (p.equals("armor_stand")) return 1;
            if (p.equals("item_frame") || p.equals("glow_item_frame")) return 2;
            if (p.endsWith("chest_minecart") || p.equals("hopper_minecart") || p.equals("furnace_minecart")
                    || p.endsWith("chest_boat") || p.endsWith("chest_raft")) return 3;
            if (p.equals("end_crystal")) return 4;
            return 0;
        });
    }

    @Override
    public void onRender(Minecraft mc, float delta) {
        double px = mc.player.getX(), py = mc.player.getY(), pz = mc.player.getZ();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player || e instanceof Player) continue;
            if (e.distanceToSqr(px, py, pz) > 128 * 128) continue;
            int c = cat(e.getType());
            float r, g, b;
            switch (c) {
                case 1 -> { r = 1f; g = 0.85f; b = 0.2f; }
                case 2 -> { r = 0.4f; g = 0.9f; b = 1f; }
                case 3 -> { r = 1f; g = 0.5f; b = 0.1f; }
                case 4 -> { r = 1f; g = 0.3f; b = 1f; }
                default -> {
                    if (!e.hasCustomName()) continue;
                    r = 0.6f; g = 1f; b = 0.6f;
                }
            }
            AABB bb = e.getBoundingBox();
            Renderer.box(bb, r, g, b, 0.3f);
            Renderer.outline(bb, 0.015, r, g, b, 1f);
        }
    }
}
