package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.access.LecternInventoryAccess;
import com.github.zly2006.enclosure.utils.Permission;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.LecternScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LecternScreenHandler.class)
public abstract class MixinLecternScreenHandler extends ScreenHandler {
    @Shadow @Final private Inventory inventory;

    protected MixinLecternScreenHandler(@Nullable ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    @Inject(method = "onButtonClick", at = @At("HEAD"), cancellable = true)
    private void onButtonClick(PlayerEntity player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayerEntity serverPlayer && inventory instanceof LecternInventoryAccess access) {
            if (!ServerMain.INSTANCE.checkPermission(serverPlayer, Permission.TAKE_BOOK, access.getPos())) {
                if (id == 3) {
                    cir.setReturnValue(false);
                    serverPlayer.closeHandledScreen();
                    serverPlayer.sendMessage(Permission.TAKE_BOOK.getNoPermissionMsg(serverPlayer));
                }
            }
        }
    }
}
