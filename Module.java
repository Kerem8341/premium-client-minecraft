package com.premium.client.core;

import net.minecraft.client.Minecraft;

public abstract class Module {
    public final String name, description;
    private boolean enabled;

    protected Module(String name, String description) { this.name = name; this.description = description; }

    public boolean isEnabled() { return enabled; }
    public void toggle() { setEnabled(!enabled); }
    public void setEnabled(boolean v) {
        if (v == enabled) return;
        enabled = v;
        if (v) onEnable(); else onDisable();
    }
    public String display() { return name; }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick(Minecraft mc) {}
    public void onRender(Minecraft mc, float delta) {}
}
