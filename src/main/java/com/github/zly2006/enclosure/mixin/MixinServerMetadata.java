package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMainKt;
import com.github.zly2006.enclosure.access.ServerMetadataAccess;
import net.fabricmc.loader.api.Version;
import net.minecraft.server.ServerMetadata;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerMetadata.class)
public abstract class MixinServerMetadata implements ServerMetadataAccess {
    private String modName = ServerMainKt.MOD_ID;
    private Version modVersion = ServerMainKt.MOD_VERSION;

    @Override
    public String getModName() {
        return modName;
    }

    @Override
    public Version getModVersion() {
        return modVersion;
    }

    public void setModName(String modName) {
        this.modName = modName;
    }

    public void setModVersion(Version modVersion) {
        this.modVersion = modVersion;
    }
}
