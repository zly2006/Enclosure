package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.ServerMain;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.ContainerLock;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.github.zly2006.enclosure.utils.Permission.CONTAINER;

@Mixin(LockableContainerBlockEntity.class)
public class MixinLockableContainerBlockEntity extends BlockEntity {
    @Shadow private ContainerLock lock;

    public MixinLockableContainerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(at = @At("HEAD"), method = "checkUnlocked(Lnet/minecraft/entity/player/PlayerEntity;)Z", cancellable = true)
    private void checkUnlocked(PlayerEntity p, CallbackInfoReturnable<Boolean> cir) {
        if (p instanceof ServerPlayerEntity player) {
            if (player.interactionManager.getGameMode() == GameMode.SPECTATOR) {
                return;
            }
            EnclosureArea area = ServerMain.INSTANCE.getAllEnclosures((ServerWorld) getWorld()).getArea(getPos());

            if (area != null && !area.areaOf(getPos()).hasPerm(player, CONTAINER)) {
                player.sendMessage(CONTAINER.getNoPermissionMsg(player));
                cir.setReturnValue(false);
            }
        }
    }
}
