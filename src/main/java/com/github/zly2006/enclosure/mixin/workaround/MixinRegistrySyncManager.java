package com.github.zly2006.enclosure.mixin.workaround;

import com.github.zly2006.enclosure.gui.EnclosureScreenHandler;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.fabric.impl.registry.sync.packet.DirectRegistryPacketHandler;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
@Mixin(value = DirectRegistryPacketHandler.class, remap = false)
public class MixinRegistrySyncManager {
    @ModifyVariable(
            method = "sendPacket",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Map<Identifier, Object2IntMap<Identifier>> modifyIds(Map<Identifier, Object2IntMap<Identifier>> ids) {
        Object2IntMap<Identifier> map = ids.get(Registries.SCREEN_HANDLER.getKey().getValue());
        map.removeInt(EnclosureScreenHandler.ENCLOSURE_SCREEN_ID);
        return ids;
    }
}
