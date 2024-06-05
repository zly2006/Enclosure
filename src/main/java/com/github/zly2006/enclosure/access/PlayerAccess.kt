package com.github.zly2006.enclosure.access;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public interface PlayerAccess {
    interface MessageProvider {
        Text get(@Nullable ServerPlayerEntity player);
    }
    long getLastTeleportTime();
    void setLastTeleportTime(long time);
    long getPermissionDeniedMsgTime();
    void setPermissionDeniedMsgTime(long time);
    default void sendMessageWithCD(Text text) {
        if (getPermissionDeniedMsgTime() + 1000 < System.currentTimeMillis()) {
            setPermissionDeniedMsgTime(System.currentTimeMillis());
            ((PlayerEntity) this).sendMessage(text, false);
        }
    }
    default void sendMessageWithCD(MessageProvider provider) {
        if (this instanceof ServerPlayerEntity) {
            sendMessageWithCD(provider.get((ServerPlayerEntity) this));
        }
        else {
            sendMessageWithCD(provider.get(null));
        }
    }
}
