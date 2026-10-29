package com.premium.client.core;

import com.premium.client.PremiumClient;
import com.premium.client.modules.BaseFinder;
import com.premium.client.modules.Radar;
import com.premium.client.modules.StashFinder;
import com.premium.client.scan.BaseDB;
import com.premium.client.scan.ChunkScanner;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    private int ticks;

    public void add(Module m) { modules.add(m); }
    public List<Module> all() { return modules; }

    @SuppressWarnings("unchecked")
    public <T extends Module> T get(Class<T> c) {
        for (Module m : modules) if (c.isInstance(m)) return (T) m;
        return null;
    }

    public boolean isOn(Class<? extends Module> c) {
        for (Module m : modules) if (c.isInstance(m)) return m.isEnabled();
        return false;
    }

    public boolean needsScan() { return isOn(BaseFinder.class) || isOn(StashFinder.class) || isOn(Radar.class); }

    public void tick(Minecraft mc) {
        try {
            if (needsScan()) {
                ChunkScanner.tick(mc);
                if (++ticks % 10 == 0) BaseDB.refreshNearest(mc);
            }
        } catch (Throwable t) { PremiumClient.LOGGER.error("Scanner error", t); }

        for (Module m : modules) {
            if (!m.isEnabled()) continue;
            try { m.onTick(mc); } catch (Throwable t) { PremiumClient.LOGGER.error("Module " + m.name + " tick error", t); }
        }
    }

    public void render(Minecraft mc, float delta) {
        for (Module m : modules) {
            if (!m.isEnabled()) continue;
            try { m.onRender(mc, delta); } catch (Throwable t) { PremiumClient.LOGGER.error("Module " + m.name + " render error", t); }
        }
    }
}
