package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMain;
import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.util.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Language.class)
public class MixinLanguage {
    @Inject(method = "create", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Language;load(Ljava/util/function/BiConsumer;Ljava/lang/String;)V"))
    private static void create(CallbackInfoReturnable<Language> cir, @Local ImmutableMap.Builder<String, String> builder) {
        if (ServerMain.INSTANCE.getCommonConfig().injectServerLanguage) {
            ServerMain.INSTANCE.getTranslation().entrySet().forEach(entry ->
                builder.put(entry.getKey(), entry.getValue().getAsString()));
        }
    }
}
