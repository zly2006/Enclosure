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
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Inject(method = "create", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Language;load(Ljava/util/function/BiConsumer;Ljava/lang/String;)V"))
    private static void create(CallbackInfoReturnable<Language> cir, ImmutableMap.Builder builder, BiConsumer biConsumer) {
        if (ServerMain.INSTANCE.getCommonConfig().injectServerLanguage) {
            ServerMain.INSTANCE.getTranslation().entrySet().forEach(entry ->
                    builder.put(entry.getKey(), entry.getValue().getAsString()));
        }
    }
}
