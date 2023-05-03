package com.github.zly2006.enclosure.listeners

import com.github.zly2006.enclosure.ServerMain
import com.github.zly2006.enclosure.command.Session
import com.github.zly2006.enclosure.command.enable
import com.github.zly2006.enclosure.utils.TrT
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents.AfterPlayerChange
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayNetworkHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World
import java.util.*

class SessionListener private constructor() : ServerPlayConnectionEvents.Join,
    ServerPlayConnectionEvents.Disconnect, AttackBlockCallback, UseBlockCallback, AfterPlayerChange {
    override fun afterChangeWorld(player: ServerPlayerEntity, origin: ServerWorld, destination: ServerWorld) {
        getSession(player)?.reset(destination)
    }

    override fun interact(player: PlayerEntity, world: World, hand: Hand, pos: BlockPos, direction: Direction): ActionResult {
        if (player.getStackInHand(hand).item === ServerMain.operationItem) {
            if (getSession(player)!!.pos1 != pos) {
                val session = getSession(player)
                session!!.syncDimension(player as ServerPlayerEntity)
                session.pos1 = pos
                session.enable()
                session.trySync()
                player.sendMessage(TrT.of("enclosure.message.set_pos_1").append(pos.toShortString()))
            }
            return ActionResult.SUCCESS
        }
        return ActionResult.PASS
    }

    override fun interact(player: PlayerEntity, world: World, hand: Hand, hitResult: BlockHitResult): ActionResult {
        if (player.mainHandStack.item === ServerMain.operationItem && hand == Hand.MAIN_HAND || player.offHandStack.item === ServerMain.operationItem && hand == Hand.OFF_HAND) {
            if (getSession(player)!!.pos2 != hitResult.blockPos) {
                val session = getSession(player)
                session!!.syncDimension(player as ServerPlayerEntity)
                session.pos2 = hitResult.blockPos
                session.enable()
                session.trySync()
                player.sendMessage(TrT.of("enclosure.message.set_pos_2").append(hitResult.blockPos.toShortString()))
            }
            return ActionResult.FAIL
        }
        return ActionResult.PASS
    }

    override fun onPlayDisconnect(handler: ServerPlayNetworkHandler, server: MinecraftServer) {
        ServerMain.playerSessions.remove(handler.player.uuid)
    }

    override fun onPlayReady(handler: ServerPlayNetworkHandler, sender: PacketSender, server: MinecraftServer) {
        ServerMain.playerSessions[handler.player.gameProfile.id] = Session(handler.player)
    }

    companion object {
        @JvmStatic
        fun register() {
            val listener = SessionListener()
            ServerPlayConnectionEvents.JOIN.register(listener)
            ServerPlayConnectionEvents.DISCONNECT.register(listener)
            AttackBlockCallback.EVENT.register(listener)
            UseBlockCallback.EVENT.register(listener)
            ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(listener)
        }

        private fun getSession(player: UUID): Session? {
            return ServerMain.playerSessions[player]
        }

        private fun getSession(player: PlayerEntity): Session? {
            return getSession(player.uuid)
        }
    }
}