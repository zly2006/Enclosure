package com.github.zly2006.enclosure.command

import com.github.zly2006.enclosure.gui.EnclosureScreenHandler
import com.github.zly2006.enclosure.minecraftServer
import com.github.zly2006.enclosure.network.ConfirmRequestS2CPacket
import com.github.zly2006.enclosure.utils.TrT
import com.github.zly2006.enclosure.utils.hoverText
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.ClickEvent
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.*

object ConfirmManager {
    init {
        ServerPlayNetworking.registerGlobalReceiver(ConfirmRequestS2CPacket.ID) { _, context ->
            val uuid = context.player().uuid
            val time = pendingMap[uuid]
            if (time != null) {
                pendingMap -= uuid
            }
        }
        ServerTickEvents.START_SERVER_TICK.register {
            tick()
        }
    }
    data class Entry(
        val message: Text?,
        val runnable: () -> Unit,
        val enforceCLI: Boolean
    )
    val runnableMap: MutableMap<UUID, Entry> = HashMap()
    private val pendingMap: MutableMap<UUID, Long> = HashMap() // pending confirm request packets
    private const val TIMEOUT = 10000L

    /**
     * @param message  the message to send to the player.
     *
     * If null, a default message with translation key "enclosure.message.dangerous" will be sent.
     *
     * @param player  the player to send the message to.
     *
     * If null, the message will be sent to the server console.
     *
     * @param enforceCLI  whether to enforce the player to use the command line interface.
     *
     * If false, we will send a packet instead a chat message to the player.
     *
     * @param runnable  the code to run when the player confirms.
     */
    fun confirm(message: Text?, player: ServerPlayerEntity?, enforceCLI: Boolean = false, runnable: () -> Unit) {
        val source = player?.commandSource ?: minecraftServer.commandSource
        val text = TrT.of("enclosure.message.dangerous")
        text.style = Style.EMPTY
                .hoverText(TrT.of("enclosure.message.confirm_event"))
                .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/enclosure confirm"))
                .withColor(Formatting.YELLOW)
        val entry = Entry(message, runnable, enforceCLI)
        runnableMap[player?.uuid ?: CONSOLE] = entry
        if (!enforceCLI && player != null && player.currentScreenHandler is EnclosureScreenHandler) {
            ServerPlayNetworking.send(source.player, ConfirmRequestS2CPacket(message ?: text))
            pendingMap[player.uuid] = System.currentTimeMillis()
        } else message?.let { source.sendMessage(it) }
        source.sendMessage(text)
    }

    private fun tick() {
        pendingMap.forEach { (uuid, time) ->
            if (System.currentTimeMillis() - time > TIMEOUT) {
                pendingMap.remove(uuid)
                runnableMap[uuid]?.let {
                    minecraftServer.playerManager.getPlayer(uuid)?.cliRetry(it)
                }
            }
        }
    }

    private fun ServerPlayerEntity.cliRetry(entry: Entry) {
        confirm(entry.message, this, true, entry.runnable)
    }
}

