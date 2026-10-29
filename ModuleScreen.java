package com.premium.client.gui;

import com.premium.client.PremiumClient;
import com.premium.client.core.Module;
import com.premium.client.core.Settings;
import com.premium.client.scan.BaseDB;
import com.premium.client.scan.BaseInfo;
import com.premium.client.scan.ChunkScanner;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class ModuleScreen extends Screen {
    public ModuleScreen() { super(Component.literal("Premium Client")); }

    @Override
    protected void init() {
        int left = this.width / 2 - 205, right = this.width / 2 + 5, top = 40;

        // ---- modules (left column)
        int y = top;
        for (Module m : PremiumClient.modules.all()) {
            addRenderableWidget(Button.builder(label(m), b -> { m.toggle(); b.setMessage(label(m)); ChunkScanner.requestRescan(); })
                    .bounds(left, y, 200, 20).build());
            y += 24;
        }

        // ---- settings (right column)
        int sy = top;
        stepper(right, sy, () -> "Base threshold: " + Settings.baseThreshold,
                d -> Settings.baseThreshold = Settings.clamp(Settings.baseThreshold + d, 3, 40));
        sy += 24;
        stepper(right, sy, () -> "Stash containers: " + Settings.stashThreshold,
                d -> Settings.stashThreshold = Settings.clamp(Settings.stashThreshold + d, 2, 60));
        sy += 24;
        stepper(right, sy, () -> "Radar range: " + Settings.radarRange + "c",
                d -> Settings.radarRange = Settings.clamp(Settings.radarRange + d * 8, 16, 512));
        sy += 24;

        addRenderableWidget(Button.builder(Component.literal("Ignore villages: " + onOff(Settings.ignoreVillages)), b -> {
            Settings.ignoreVillages = !Settings.ignoreVillages;
            b.setMessage(Component.literal("Ignore villages: " + onOff(Settings.ignoreVillages)));
            ChunkScanner.requestRescan();
        }).bounds(right, sy, 200, 20).build());
        sy += 24;

        addRenderableWidget(Button.builder(Component.literal("Chat alerts: " + onOff(Settings.chatAlerts)), b -> {
            Settings.chatAlerts = !Settings.chatAlerts;
            b.setMessage(Component.literal("Chat alerts: " + onOff(Settings.chatAlerts)));
        }).bounds(right, sy, 200, 20).build());
        sy += 24;

        addRenderableWidget(Button.builder(Component.literal("Copy nearest base coords"), b -> {
            List<BaseInfo> n = BaseDB.nearest();
            if (!n.isEmpty() && this.minecraft != null)
                this.minecraft.keyboardHandler.setClipboard((int) n.get(0).x() + " " + (int) n.get(0).z());
        }).bounds(right, sy, 200, 20).build());
        sy += 24;

        addRenderableWidget(Button.builder(Component.literal("Clear saved bases (this dimension)"), b -> BaseDB.clearCurrent())
                .bounds(right, sy, 200, 20).build());
    }

    private void stepper(int x, int y, Supplier<String> text, IntConsumer step) {
        Button lbl = Button.builder(Component.literal(text.get()), b -> {}).bounds(x, y, 140, 20).build();
        lbl.active = false;
        addRenderableWidget(lbl);
        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            step.accept(-1); lbl.setMessage(Component.literal(text.get())); ChunkScanner.requestRescan();
        }).bounds(x + 144, y, 26, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            step.accept(1); lbl.setMessage(Component.literal(text.get())); ChunkScanner.requestRescan();
        }).bounds(x + 174, y, 26, 20).build());
    }

    private static String onOff(boolean v) { return v ? "§aON" : "§cOFF"; }
    private static Component label(Module m) { return Component.literal(m.name + ": " + onOff(m.isEnabled())); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        g.drawCenteredString(this.font, "Premium Client", this.width / 2, 16, 0xFFB26BFF);
        g.drawCenteredString(this.font, "Right Shift opens this menu", this.width / 2, 27, 0xFF9999AA);
    }
}
