package com.github.zly2006.enclosure.mixin.workaround;

import com.github.zly2006.enclosure.EnclosureListKt;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.*;

@Mixin(PersistentStateManager.class)
public class MixinPersistentStateManager {
    @Inject(
            method = "readNbt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/datafixer/DataFixTypes;update(Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/nbt/NbtCompound;II)Lnet/minecraft/nbt/NbtCompound;"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD,
            cancellable = true
    )
    private void readNbt$Inject$DataFixTypes$Update(String id, DataFixTypes dataFixTypes, int currentSaveVersion, CallbackInfoReturnable<NbtCompound> cir, File file, InputStream inputStream, PushbackInputStream pushbackInputStream, NbtCompound nbtCompound, int i) {
        if (id.equals(EnclosureListKt.ENCLOSURE_LIST_KEY)) {
            try {
                pushbackInputStream.close();
                inputStream.close();
            } catch (IOException ignored) {
            }
            cir.setReturnValue(nbtCompound);
        }
    }
}
