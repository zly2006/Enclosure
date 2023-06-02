package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.access.ServerMetadataAccess;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.server.ServerMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Type;

@Mixin(ServerMetadata.Deserializer.class)
public abstract class MixinServerMetadataCodec {
    @Inject(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/server/ServerMetadata;", at = @At("RETURN"))
    private void deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext, CallbackInfoReturnable<ServerMetadata> cir) {
        ServerMetadataAccess metadata = ((ServerMetadataAccess) cir.getReturnValue());
        if (jsonElement.getAsJsonObject().has("enclosure_name")) {
            metadata.setModName(jsonElement.getAsJsonObject().get("enclosure_name").getAsString());
        }
        if (jsonElement.getAsJsonObject().has("enclosure_version")) {
            String version = jsonElement.getAsJsonObject().get("enclosure_version").getAsString();
            try {
                metadata.setModVersion(net.fabricmc.loader.api.Version.parse(version));
            } catch (VersionParsingException ignored) {
            }
        }
    }
}
