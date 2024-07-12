package com.github.zly2006.enclosure.command

import com.github.zly2006.enclosure.ServerMain
import com.github.zly2006.enclosure.access.PlayerAccess
import com.github.zly2006.enclosure.network.config.EnclosureInstalledC2SPacket
import com.github.zly2006.enclosure.utils.Permission
import com.github.zly2006.enclosure.utils.Serializable2Text.SerializationSettings
import com.github.zly2006.enclosure.utils.TrT
import com.github.zly2006.enclosure.utils.Utils
import com.github.zly2006.enclosure.utils.plus
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.text.ClickEvent
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos

fun BuilderScope<*>.registerAdmin() {
    literal("admin") {
        literal("reload") {
            permission("enclosure.command.admin.reload", BuilderScope.Companion.DefaultPermission.OP)
            literal("all") {
                executes {
                    ServerMain.reloadCommon()
                    ServerMain.reloadLimits()
                    source.sendMessage(Text.literal("Reloaded, some config may not affect until restart"))
                }
            }
            literal("common") {
                executes {
                    ServerMain.reloadCommon()
                    source.sendMessage(Text.literal("Reloaded, some config may not affect until restart"))
                }
            }
            literal("limits") {
                executes {
                    ServerMain.reloadLimits()
                    source.sendMessage(Text.literal("Reloaded, some config may not affect until restart"))
                }
            }
        }
        literal("limit_exceeded") {
            permission("enclosure.command.admin.limit_exceeded", BuilderScope.Companion.DefaultPermission.OP)
            literal("size") {
                executes {
                    ServerMain.getAllEnclosures().flatMap {
                        it.subEnclosures.areas.toList() + it
                    }.map {
                        val session = Session(null)
                        session.pos1 = BlockPos(it.minX, it.minY, it.minZ)
                        session.pos2 = BlockPos(it.maxX, it.maxY, it.maxZ)
                        it to session.isValid(getLimits(this))
                    }.filter { it.second != null }.forEach { (area, text) ->
                        source.sendMessage(
                            Text.literal("Enclosure ")
                                .append(area.serialize(SerializationSettings.Name, source.player))
                                .append(" is too large: ")
                                .append(text)
                        )
                    }
                }
            }
            literal("count") {
                executes {
                    ServerMain.getAllEnclosures().groupBy {
                        it.owner
                    }.forEach { (owner, enclosures) ->
                        if (enclosures.size > getLimits(this).maxLands) {
                            source.sendMessage(
                                Text.literal("Player ") +
                                        Utils.getDisplayNameByUUID(owner) +
                                        " has too many enclosures: " +
                                        enclosures.size.toString()
                            )
                        }
                    }
                }
            }
        }
        literal("visited") {
            permission("enclosure.command.admin.visited", BuilderScope.Companion.DefaultPermission.OP)
            argument("player", EntityArgumentType.player()) {
                literal("get") {
                    executes {
                        val player = EntityArgumentType.getPlayer(this, "player")
                        val visited = ServerMain.getAllEnclosures()
                            .filter { it.uuid in (player as PlayerAccess).visitedEnclosures }
                        val text = player.name.copy().append(" visited: ")
                        visited.forEach { e ->
                            text.append(e.serialize(SerializationSettings.Summarize, player))
                                .append(" ")
                        }
                        source.sendMessage(text)
                    }
                }
                literal("clear") {
                    executes {
                        val player = EntityArgumentType.getPlayer(this, "player")
                        (player as PlayerAccess).visitedEnclosures.clear()
                        source.sendMessage(Text.literal("Cleared"))
                    }
                }
            }
        }
        literal("closest") {
            permission("enclosure.command.admin.closest", BuilderScope.Companion.DefaultPermission.OP)
            executes {
                val enclosure = ServerMain.getAllEnclosures(source.world).areas
                    .minByOrNull {
                        it.distanceTo(source.position).horizontalLength()
                    }
                if (enclosure == null) {
                    source.sendMessage(Text.literal("No enclosure found"))
                } else {
                    source.sendMessage(
                        Text.literal("Closest enclosure: " + enclosure.fullName + ", click to show info")
                            .styled {
                                it.withClickEvent(
                                    ClickEvent(
                                        net.minecraft.text.ClickEvent.Action.RUN_COMMAND,
                                        "/enclosure info " + enclosure.fullName
                                    )
                                )
                            })
                }
            }
        }
        literal("perm-info") {
            permission("enclosure.command.admin.perm_info", BuilderScope.Companion.DefaultPermission.OP)
            argument(permissionArgument(com.github.zly2006.enclosure.utils.Permission.Target.Both)) {
                executes {
                    val permission = Permission.getValue(StringArgumentType.getString(this, "permission"))
                        ?: error(TrT.of("enclosure.message.invalid_permission"), this)
                    source.sendMessage(
                        Text.literal("Name: ${permission.name} Target: ${permission.target}\nDescription: ") + permission.description + Text.literal(
                            "\nDefault: ${permission.defaultValue}\nComponents: ${permission.permissions.joinToString()}"
                        )
                    )
                }
            }
        }
        literal("clients") {
            permission("enclosure.command.admin.clients", BuilderScope.Companion.DefaultPermission.OP)
            executes {
                EnclosureInstalledC2SPacket.installedClientMod.forEach {
                    source.sendMessage(
                        source.server.playerManager.getPlayer(it.key)!!.name.copy()
                            .append(Text.literal(": " + it.value.friendlyString))
                    )
                }
            }
        }
    }
}
