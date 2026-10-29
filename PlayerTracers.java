package com.premium.client.modules;

import com.premium.client.core.Module;
import com.premium.client.render.Renderer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PlayerTracers extends Module {
    public PlayerTracers() { super("PlayerTracers", "Boxes + lines to other players"); }

    @Override
    public void onRender(Minecraft mc, float delta) {
        Vec3 start = Renderer.cam().add(mc.player.getViewVector(delta).scale(2.0));
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof Player) || e == mc.player) continue;
            double x = Mth.lerp(delta, e.xo, e.getX());
            double y = Mth.lerp(delta, e.yo, e.getY());
            double z = Mth.lerp(delta, e.zo, e.getZ());
            double h = e.getBbHeight();
            AABB bb = new AABB(x - 0.3, y, z - 0.3, x + 0.3, y + h, z + 0.3);
            Renderer.outline(bb, 0.02, 0.3f, 0.8f, 1f, 1f);
            Renderer.line(start, new Vec3(x, y + h / 2, z), 0.02 + Math.sqrt(e.distanceToSqr(mc.player)) * 0.0006, 0.3f, 0.8f, 1f, 0.9f);
        }
    }
}
