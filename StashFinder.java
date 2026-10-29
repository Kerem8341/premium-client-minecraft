package com.premium.client.modules;

import com.premium.client.core.Module;
import com.premium.client.render.Renderer;
import com.premium.client.scan.BaseDB;
import com.premium.client.scan.BaseInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Magenta beams on chunks with many containers (chests / barrels / shulkers). */
public class StashFinder extends Module {
    public StashFinder() { super("StashFinder", "Beams on chunks packed with containers"); }

    @Override
    public void onRender(Minecraft mc, float delta) {
        Vec3 start = Renderer.cam().add(mc.player.getViewVector(delta).scale(2.0));
        double py = mc.player.getY() + 1;
        for (BaseInfo b : BaseDB.nearest()) {
            if (!b.stash) continue;
            double w = 0.5 + b.dist * 0.005;
            Renderer.box(b.x() - w, -64, b.z() - w, b.x() + w, 320, b.z() + w, 1f, 0.2f, 1f, 0.6f);
            Renderer.line(start, new Vec3(b.x(), py, b.z()), 0.04 + b.dist * 0.0008, 1f, 0.2f, 1f, 0.9f);
        }
    }
}
