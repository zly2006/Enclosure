package com.github.zly2006.enclosure.access

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text

@JvmDefaultWithCompatibility
interface PlayerAccess {
    interface MessageProvider {
        fun get(player: ServerPlayerEntity?): Text?
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("enclosure\$getLastTeleportTime")
    @set:JvmName("enclosure\$setLastTeleportTime")
    var lastTeleportTime: Long

    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("enclosure\$getPermissionDeniedMsgTime")
    @set:JvmName("enclosure\$setPermissionDeniedMsgTime")
    var permissionDeniedMsgTime: Long

    fun sendMessageWithCD(text: Text?) {
        if (permissionDeniedMsgTime + 1000 < System.currentTimeMillis()) {
            permissionDeniedMsgTime = System.currentTimeMillis()
            (this as PlayerEntity).sendMessage(text, false)
        }
    }

    fun sendMessageWithCD(provider: MessageProvider) {
        if (this is ServerPlayerEntity) {
            sendMessageWithCD(provider.get(this as ServerPlayerEntity))
        } else {
            sendMessageWithCD(provider.get(null))
        }
    }
}
