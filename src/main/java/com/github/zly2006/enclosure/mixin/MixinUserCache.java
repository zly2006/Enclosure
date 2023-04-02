package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMainKt;
import com.google.gson.JsonArray;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import net.minecraft.util.UserCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import static com.github.zly2006.enclosure.ServerMainKt.GSON;

@Mixin(UserCache.class)
public class MixinUserCache {
    @Inject(at = @At("RETURN"), method = "<init>", locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onAdd(GameProfileRepository profileRepository, File cacheFile, CallbackInfo ci) {
        try {
            GSON.fromJson(Files.newBufferedReader(cacheFile.toPath()), JsonArray.class)
                    .forEach(jsonElement -> ServerMainKt.byUuid.put(
                            UUID.fromString(jsonElement.getAsJsonObject().get("uuid").getAsString()),
                            jsonElement.getAsJsonObject().get("name").getAsString()
                    ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Inject(at = @At("HEAD"), method = "add(Lcom/mojang/authlib/GameProfile;)V")
    private void onAdd(GameProfile profile, CallbackInfo ci) {
        ServerMainKt.byUuid.put(profile.getId(), profile.getName());
    }
}
