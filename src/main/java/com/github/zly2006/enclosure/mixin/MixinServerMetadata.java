package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMainKt;
import com.github.zly2006.enclosure.access.ServerMetadataAccess;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.loader.api.Version;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.server.ServerMetadata;
import net.minecraft.util.dynamic.Codecs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerMetadata.class)
public abstract class MixinServerMetadata implements ServerMetadataAccess {
    @Mutable
    @Shadow
    @Final
    public static Codec<ServerMetadata> CODEC;

    private String modName = ServerMainKt.MOD_ID;
    private Version modVersion = ServerMainKt.MOD_VERSION;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void afterStaticInit(CallbackInfo ci) {
        CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codecs.TEXT.optionalFieldOf("description", ScreenTexts.EMPTY)
                        .forGetter(ServerMetadata::description),
                ServerMetadata.Players.CODEC.optionalFieldOf("players")
                        .forGetter(ServerMetadata::players),
                ServerMetadata.Version.CODEC.optionalFieldOf("version")
                        .forGetter(ServerMetadata::version),
                ServerMetadata.Favicon.CODEC.optionalFieldOf("favicon")
                        .forGetter(ServerMetadata::favicon),
                Codec.BOOL.optionalFieldOf("enforcesSecureChat", false)
                        .forGetter(ServerMetadata::secureChatEnforced),
                Codecs.NON_EMPTY_STRING.optionalFieldOf("enclosure_name", "")
                        .forGetter(serverMetadata -> ((ServerMetadataAccess) serverMetadata).getModName()),
                Codecs.NON_EMPTY_STRING.optionalFieldOf("enclosure_version", "")
                        .forGetter(serverMetadata -> ((ServerMetadataAccess) serverMetadata).getModVersion().getFriendlyString())
        ).apply(instance, ServerMetadataAccess::newMetadata));
    }

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
