package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.access.MiningStatusAccess;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerActionResponseS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class MixinServerPlayNetworkHandler {
    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onPlayerAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerInteractionManager;processBlockBreakingAction(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/network/packet/c2s/play/PlayerActionC2SPacket$Action;Lnet/minecraft/util/math/Direction;I)V", shift = At.Shift.AFTER))
    private void onPlayerAction(PlayerActionC2SPacket packet, CallbackInfo ci) {
        switch (packet.getAction()) {
            case STOP_DESTROY_BLOCK -> {
                if (!((MiningStatusAccess) player.interactionManager).success()) {
                    BlockEntity entity = player.getServerWorld().getBlockEntity(packet.getPos());
                    if (entity != null) {
                        Packet<ClientPlayPacketListener> syncPacket = entity.toUpdatePacket();
                        if (syncPacket != null) {
                            // finally, sync the block entity to the client
                            player.networkHandler.sendPacket(syncPacket);
                        }
                    }
                }
            }
        }
    }
}
