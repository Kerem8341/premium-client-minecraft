package com.premium.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.premium.client.core.ModuleManager;
import com.premium.client.gui.ModuleScreen;
import com.premium.client.modules.*;
import com.premium.client.render.Renderer;
import com.premium.client.scan.BaseDB;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PremiumClient implements ClientModInitializer {
    public static final String ID = "premiumclient";
    public static final Logger LOGGER = LoggerFactory.getLogger("PremiumClient");
    public static ModuleManager modules;

    @Override
    public void onInitializeClient() {
        modules = new ModuleManager();
        modules.add(new BaseFinder());
        modules.add(new StashFinder());
        modules.add(new Radar());
        modules.add(new StorageESP());
        modules.add(new EntityESP());
        modules.add(new PlayerTracers());

        // On by default so it works right away
        modules.get(BaseFinder.class).setEnabled(true);
        modules.get(Radar.class).setEnabled(true);

        BaseDB.load();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(ID, "main"));
        KeyMapping menuKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.premiumclient.menu", InputConstants.Type.KEYSYM, InputConstants.KEY_RSHIFT, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menuKey.consumeClick()) mc.setScreen(new ModuleScreen());
            if (mc.player == null || mc.level == null) return;
            modules.tick(mc);
        });

        WorldRenderEvents.BEFORE_TRANSLUCENT.register(Renderer::render);

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(ID, "hud"), Hud::render);

        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
            BaseDB.save();
            Renderer.close();
        });
    }
}
