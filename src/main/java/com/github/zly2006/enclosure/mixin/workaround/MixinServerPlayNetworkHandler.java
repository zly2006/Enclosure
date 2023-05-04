package com.github.zly2006.enclosure.mixin.workaround;

import com.github.zly2006.enclosure.access.MiningStatusAccess;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
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

/**
 * This mixin is a work-around for the Minecraft mining system.
 */
@Mixin(ServerPlayNetworkHandler.class)
public class MixinServerPlayNetworkHandler {
    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onPlayerAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;updateSequence(I)V", shift = At.Shift.AFTER))
    private void onPlayerAction(PlayerActionC2SPacket packet, CallbackInfo ci) {
        switch (packet.getAction()) {
            case START_DESTROY_BLOCK, STOP_DESTROY_BLOCK, ABORT_DESTROY_BLOCK -> {
                if (!((MiningStatusAccess) player.interactionManager).success()) {
                    // firstly, respond to the client's mining request
                    // Note: the client will roll back the block changes when receiving this packet,
                    //       but it will **NOT** roll back the block entity.
                    if (player.networkHandler.sequence > -1) {
                        player.networkHandler.sendPacket(new PlayerActionResponseS2CPacket(player.networkHandler.sequence));
                        player.networkHandler.sequence = -1;
                    }
                    // then, sync the block to the client
                    player.networkHandler.sendPacket(new BlockUpdateS2CPacket(player.getWorld(), packet.getPos()));
                    BlockEntity entity = player.getWorld().getBlockEntity(packet.getPos());
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
