package com.github.zly2006.enclosure.command

import com.github.zly2006.enclosure.ServerMain
import com.github.zly2006.enclosure.access.PlayerAccess
import com.github.zly2006.enclosure.utils.Permission
import com.github.zly2006.enclosure.utils.Serializable2Text.SerializationSettings
import com.github.zly2006.enclosure.utils.TrT
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos

fun BuilderScope<*>.registerTeleport() {
    literal("tp") {
        permission("enclosure.command.tp", BuilderScope.Companion.DefaultPermission.TRUE)
        argument(landArgument()) {
            executes {
                val player = source.player!!
                val lastTeleportTimeSpan =
                    System.currentTimeMillis() - (player as PlayerAccess).permissionDeniedMsgTime
                val cd = ServerMain.commonConfig.teleportCooldown
                val area = getEnclosure(this)

                if (!area.hasPerm(player, Permission.COMMAND_TP)) {
                    player.sendMessage(Permission.COMMAND_TP.getNoPermissionMsg(player))
                    return@executes
                }
                if (!source.hasPermissionLevel(4) && cd > 0 && lastTeleportTimeSpan < cd) {
                    error(
                        TrT.of(
                            "enclosure.message.teleport_too_fast",
                            String.format("%.1f", (cd - lastTeleportTimeSpan) / 1000.0)
                        ), this
                    )
                }
                if (!source.hasPermissionLevel(4) && ServerMain.commonConfig.restrictEnclosureTp) {
                    if (source.player is PlayerAccess) {
                        if (!(source.player as PlayerAccess).visitedEnclosures.contains(area.uuid)) {
                            error(TrT.of("enclosure.message.unvisited_enclosure"), this)
                        }
                    }
                }
                (player as PlayerAccess).permissionDeniedMsgTime = System.currentTimeMillis()
                if (ServerMain.commonConfig.showTeleportWarning) {
                    if (!isPositionSafe(area.world, area.teleportPos!!, player)) {
                        source.sendMessage(
                            TrT.of("enclosure.message.teleport_warning").formatted(Formatting.YELLOW)
                        )
                        ConfirmManager.confirm(null, source.player) { area.teleport(player) }
                    } else {
                        area.teleport(player)
                    }
                } else {
                    area.teleport(player)
                }
            }
        }
    }
    literal("settp") {
        permission("enclosure.command.settp", BuilderScope.Companion.DefaultPermission.TRUE)
        optionalEnclosure { area ->
            if (!area.hasPerm(source.player!!, Permission.ADMIN)) {
                error(Permission.ADMIN.getNoPermissionMsg(source.player), this)
            }
            if (!area.contains(BlockPos.ofFloored(source.position))) {
                error(TrT.of("enclosure.message.res_settp_pos_error"), this)
            }
            area.setTeleportPos(source.position, source.rotation.y, source.rotation.x)
            if (ServerMain.commonConfig.showTeleportWarning && !isPositionSafe(area.world, area.teleportPos!!, source.player!!)) {
                source.sendMessage(
                    TrT.of("enclosure.message.teleport_warning.on_set").formatted(Formatting.YELLOW)
                )
            }
            source.sendMessage(
                TrT.of("enclosure.message.change_teleport_position",
                    area.serialize(SerializationSettings.Name, source.player),
                    String.format(
                        "[%.2f, %.2f, %.2f](yaw: %.2f, pitch: %.2f)",
                        source.position.x,
                        source.position.y,
                        source.position.z,
                        source.rotation.y,
                        source.rotation.x
                    )
                )
            )
        }
    }
}
