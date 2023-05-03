package com.github.zly2006.enclosure.command

import com.github.zly2006.enclosure.EnclosureArea
import com.github.zly2006.enclosure.EnclosureList
import com.github.zly2006.enclosure.ServerMain
import com.github.zly2006.enclosure.config.LandLimits
import com.github.zly2006.enclosure.minecraftServer
import com.github.zly2006.enclosure.network.EnclosureInstalledC2SPacket
import com.github.zly2006.enclosure.network.NetworkChannels
import com.github.zly2006.enclosure.utils.TrT
import com.github.zly2006.enclosure.utils.Utils
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Direction.*
import kotlin.math.max
import kotlin.math.min

class Session(
    player: ServerPlayerEntity?
): ClientSession() {
    var owner = player?.uuid ?: CONSOLE
    var world: ServerWorld = player?.getWorld() ?: minecraftServer.overworld
    fun trySync() {
        if (owner == CONSOLE || !enabled) {
            return
        }
        val player = world.server.playerManager?.getPlayer(owner) ?: return
        if (EnclosureInstalledC2SPacket.isInstalled(player)) {
            val session = ServerMain.playerSessions[player.uuid]
            if (session != null) {
                val buf = PacketByteBufs.create()
                buf.writeBlockPos(session.pos1)
                buf.writeBlockPos(session.pos2)
                ServerPlayNetworking.send(player, NetworkChannels.SYNC_SELECTION, buf)
            }
        }
    }

    fun reset(serverWorld: ServerWorld) {
        world = serverWorld
        pos1 = serverWorld.spawnPos
        pos2 = serverWorld.spawnPos
    }

    fun syncDimension(player: ServerPlayerEntity) {
        if (world !== player.getWorld()) {
            reset(player.getWorld())
        }
    }

    fun intersect(list2check: EnclosureList): EnclosureArea? {
        val minX = min(pos1.x, pos2.x)
        val minY = min(pos1.y, pos2.y)
        val minZ = min(pos1.z, pos2.z)
        val maxX = max(pos1.x, pos2.x)
        val maxY = max(pos1.y, pos2.y)
        val maxZ = max(pos1.z, pos2.z)
        for (area in list2check.areas) {
            if (area.intersect(minX, minY, minZ, maxX, maxY, maxZ)) {
                return area
            }
        }
        return null
    }

    private fun <T : Comparable<T>> singleCheck(value: T, limit: T, lessThan: Boolean, name: String): Text? {
        val r = value.compareTo(limit)
        if (r < 0 && lessThan) {
            return null
        }
        if (r == 0) {
            return null
        }
        if (r > 0 && !lessThan) {
            return null
        }
        return if (lessThan) {
            TrT.of("enclosure.limit." + Utils.camelCaseToSnakeCase(name))
                .append(TrT.of("enclosure.message.limit_exceeded.0"))
                .append(limit.toString())
                .append(TrT.of("enclosure.message.limit_exceeded.1"))
                .append(Text.literal(value.toString()))
        } else {
            TrT.of("enclosure.limit." + Utils.camelCaseToSnakeCase(name))
                .append(TrT.of("enclosure.message.limit_exceeded.2"))
                .append(limit.toString())
                .append(TrT.of("enclosure.message.limit_exceeded.1"))
                .append(Text.literal(value.toString()))
        }
    }

    fun isValid(limits: LandLimits): Text? {
        val minX = min(pos1.x, pos2.x)
        val minY = min(pos1.y, pos2.y)
        val minZ = min(pos1.z, pos2.z)
        val maxX = max(pos1.x, pos2.x)
        val maxY = max(pos1.y, pos2.y)
        val maxZ = max(pos1.z, pos2.z)
        var text = singleCheck(maxX - minX + 1, limits.maxXRange, true, "maxXRange")
        if (text != null) {
            return text
        }
        text = singleCheck(maxZ - minZ + 1, limits.maxZRange, true, "maxZRange")
        if (text != null) {
            return text
        }
        text = singleCheck(maxY - minY + 1, limits.maxHeight, true, "maxHeight")
        if (text != null) {
            return text
        }
        text = singleCheck(maxX - minX + 1, limits.minXRange, false, "minXRange")
        if (text != null) {
            return text
        }
        text = singleCheck(maxZ - minZ + 1, limits.minZRange, false, "minZRange")
        if (text != null) {
            return text
        }
        text = singleCheck(minY, limits.minY, false, "minY")
        if (text != null) {
            return text
        }
        text = singleCheck(maxY, limits.maxY, true, "maxY")
        return text
    }

    fun shrink(direction: Direction, amount: Int) {
        val p1 = BlockPos(
            min(pos1.x, pos2.x), min(pos1.y, pos2.y), min(
                pos1.z, pos2.z
            )
        )
        val p2 = BlockPos(
            max(pos1.x, pos2.x), max(pos1.y, pos2.y), max(
                pos1.z, pos2.z
            )
        )
        pos1 = p1
        pos2 = p2
        when (direction) {
            NORTH -> pos1 = pos1.add(0, 0, amount)
            SOUTH -> pos2 = pos2.add(0, 0, -amount)
            WEST -> pos1 = pos1.add(amount, 0, 0)
            EAST -> pos2 = pos2.add(-amount, 0, 0)
            UP -> pos2 = pos2.add(0, -amount, 0)
            DOWN -> pos1 = pos1.add(0, amount, 0)
        }
    }

    fun expand(direction: Direction, amount: Int) {
        shrink(direction, -amount)
    }

    fun shift(direction: Direction, amount: Int) {
        pos1 = pos1.offset(direction, amount)
        pos2 = pos2.offset(direction, amount)
    }
}