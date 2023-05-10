package com.github.zly2006.enclosure.client.mixin;

import com.github.zly2006.enclosure.access.ServerMetadataAccess;
import net.fabricmc.loader.api.Version;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerInfo.class)
public class MixinServerInfo implements ServerMetadataAccess {
    String modName;
    Version modVersion;

    @Override
    public String getModName() {
        return modName;
    }

    @Override
    public Version getModVersion() {
        return modVersion;
    }

    @Override
    public void setModVersion(Version modVersion) {
        this.modVersion = modVersion;
    }

    @Override
    public void setModName(String modName) {
        this.modName = modName;
    }
}
