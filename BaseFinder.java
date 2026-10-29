package com.premium.client.modules;

import com.premium.client.PremiumClient;
import com.premium.client.render.Renderer;
import com.premium.client.scan.BaseDB;
import com.premium.client.scan.BaseInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Tall beams + tracers on every remembered base, visible through walls from far away. */
public class BaseFinder extends com.premium.client.core.Module {
    public BaseFinder() { super("BaseFinder", "Beams and tracers on detected bases"); }

    @Override public String display() { return name + " §7[" + BaseDB.map().size() + "]"; }

    /** yellow (weak) -> red (strong) */
    public static float[] color(int score) {
        float t = Math.min(1f, Math.max(0f, (score - 6) / 20f));
        return new float[]{1f, 1f - 0.8f * t, 0.15f};
    }

    @Override
    public void onRender(Minecraft mc, float delta) {
        boolean stashOn = PremiumClient.modules.isOn(StashFinder.class);
        Vec3 start = Renderer.cam().add(mc.player.getViewVector(delta).scale(2.0));
        double py = mc.player.getY() + 1;

        for (BaseInfo b : BaseDB.nearest()) {
            if (b.stash && stashOn) continue;       // StashFinder draws those
            float[] c = color(b.score);
            double w = 0.35 + b.dist * 0.0035;       // beam gets wider with distance so it stays visible
            Renderer.box(b.x() - w, -64, b.z() - w, b.x() + w, 320, b.z() + w, c[0], c[1], c[2], 0.55f);
            Renderer.line(start, new Vec3(b.x(), py, b.z()), 0.03 + b.dist * 0.0006, c[0], c[1], c[2], 0.9f);
        }
    }
}
