package com.github.zly2006.enclosure.rei;

import com.github.zly2006.enclosure.gui.EnclosureScreen;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.OverlayDecider;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ActionResult;

public class ReiMain implements REIClientPlugin {
    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerDecider(new OverlayDecider() {
            @Override
            public <R extends Screen> boolean isHandingScreen(Class<R> screen) {
                return EnclosureScreen.class.isAssignableFrom(screen);
            }

            @Override
            public <R extends Screen> ActionResult shouldScreenBeOverlaid(R screen) {
                if (screen instanceof EnclosureScreen) {
                    return ActionResult.FAIL;
                }
                return ActionResult.PASS;
            }
        });
    }
}
