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
    private static void create(CallbackInfoReturnable<Language> cir, ImmutableMap.Builder<String, String> builder, BiConsumer<String, String> biConsumer, String string) {
        if (ServerMain.INSTANCE.getCommonConfig().injectServerLanguage) {
            ServerMain.INSTANCE.getTranslation().entrySet().forEach(entry ->
                builder.put(entry.getKey(), entry.getValue().getAsString()));
        }
    }
}
