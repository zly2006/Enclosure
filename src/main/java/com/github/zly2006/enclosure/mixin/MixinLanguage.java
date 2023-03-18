package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMain;
import com.google.common.collect.ImmutableMap;
import net.minecraft.util.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.function.BiConsumer;

@Mixin(Language.class)
public class MixinLanguage {
    @Inject(method = "create", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Ljava/lang/Class;getResourceAsStream(Ljava/lang/String;)Ljava/io/InputStream;"))
    private static void create(CallbackInfoReturnable<Language> cir, ImmutableMap.Builder builder, BiConsumer biConsumer) {
        ServerMain.load();
        if (ServerMain.commonConfig.injectServerLanguage) {
            if (ServerMain.translation != null) {
                ServerMain.translation.entrySet().forEach(entry ->
                        builder.put(entry.getKey(), entry.getValue().getAsString()));
            }
            else {
                ServerMain.LOGGER.error("Failed to inject enclosure translations!");
            }
        }
    }
}
