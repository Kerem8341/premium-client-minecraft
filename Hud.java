package com.premium.client;

import com.premium.client.core.Module;
import com.premium.client.core.Settings;
import com.premium.client.modules.BaseFinder;
import com.premium.client.modules.Radar;
import com.premium.client.scan.BaseDB;
import com.premium.client.scan.BaseInfo;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Comparator;
import java.util.List;

public final class Hud {
    private static final String[] DIRS = {"E", "SE", "S", "SW", "W", "NW", "N", "NE"};

    public static void render(GuiGraphics g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        try {
            drawModuleList(g, mc);
            if (PremiumClient.modules.isOn(Radar.class)) drawRadar(g, mc);
        } catch (Throwable t) {
            PremiumClient.LOGGER.error("HUD error", t);
        }
    }

    private static void drawModuleList(GuiGraphics g, Minecraft mc) {
        g.drawString(mc.font, "Premium §7v2.0", 4, 4, 0xFFB26BFF);
        List<Module> on = PremiumClient.modules.all().stream()
                .filter(Module::isEnabled)
                .sorted(Comparator.comparingInt((Module m) -> mc.font.width(m.display())).reversed())
                .toList();
        int w = g.guiWidth(), y = 4;
        for (Module m : on) {
            String s = m.display();
            int x = w - mc.font.width(s) - 5;
            g.fill(x - 3, y - 1, w, y + 10, 0x90000000);
            g.fill(w - 1, y - 1, w, y + 10, 0xFFB26BFF);
            g.drawString(mc.font, s, x, y + 1, 0xFFD9B8FF);
            y += 11;
        }
    }

    private static void drawRadar(GuiGraphics g, Minecraft mc) {
        int size = 100, x0 = 6, y0 = 18, cxp = x0 + size / 2, cyp = y0 + size / 2;
        g.fill(x0, y0, x0 + size, y0 + size, 0xA0101018);
        int border = 0xFFB26BFF;
        g.fill(x0, y0, x0 + size, y0 + 1, border);
        g.fill(x0, y0 + size - 1, x0 + size, y0 + size, border);
        g.fill(x0, y0, x0 + 1, y0 + size, border);
        g.fill(x0 + size - 1, y0, x0 + size, y0 + size, border);
        g.fill(cxp, y0 + 1, cxp + 1, y0 + size - 1, 0x30FFFFFF);
        g.fill(x0 + 1, cyp, x0 + size - 1, cyp + 1, 0x30FFFFFF);

        double px = mc.player.getX(), pz = mc.player.getZ();
        double lim = size / 2.0 - 3;
        double scale = lim / (Settings.radarRange * 16.0);

        for (BaseInfo b : BaseDB.nearest()) {
            double dx = (b.x() - px) * scale, dz = (b.z() - pz) * scale;
            double d = Math.max(Math.abs(dx), Math.abs(dz));
            if (d > lim) { dx *= lim / d; dz *= lim / d; }      // clamp to edge = "that way"
            int sx = cxp + (int) Math.round(dx), sy = cyp + (int) Math.round(dz);
            int col;
            if (b.stash) col = 0xFFFF40FF;
            else {
                float[] c = BaseFinder.color(b.score);
                col = 0xFF000000 | ((int) (c[0] * 255) << 16) | ((int) (c[1] * 255) << 8) | (int) (c[2] * 255);
            }
            g.fill(sx - 1, sy - 1, sx + 2, sy + 2, col);
        }
        g.fill(cxp - 1, cyp - 1, cxp + 2, cyp + 2, 0xFF55FF55);
        g.drawString(mc.font, "N", cxp - 2, y0 + 2, 0xFFFFFFFF);
        g.drawString(mc.font, Settings.radarRange + "c", x0 + 3, y0 + size - 11, 0xFF9999AA);

        // nearest list
        int y = y0 + size + 4, shown = 0;
        for (BaseInfo b : BaseDB.nearest()) {
            if (shown++ >= 6) break;
            double dx = b.x() - px, dz = b.z() - pz;
            int i = (int) Math.round(Math.atan2(dz, dx) / (Math.PI / 4));
            String dir = DIRS[((i % 8) + 8) % 8];
            String line = (b.stash ? "§d" : "§e") + (int) b.x() + ", " + (int) b.z() + " §7" + (int) b.dist + "m " + dir + " [" + b.score + "]";
            g.drawString(mc.font, line, x0, y, 0xFFFFFFFF);
            y += 10;
        }
    }
}
