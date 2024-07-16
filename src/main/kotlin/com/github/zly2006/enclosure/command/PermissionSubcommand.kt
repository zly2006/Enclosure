package com.github.zly2006.enclosure.command

import com.github.zly2006.enclosure.EnclosureArea
import com.github.zly2006.enclosure.ServerMain
import com.github.zly2006.enclosure.minecraftServer
import com.github.zly2006.enclosure.network.config.EnclosureInstalledC2SPacket
import com.github.zly2006.enclosure.network.play.SyncPermissionS2CPacket
import com.github.zly2006.enclosure.toNbt
import com.github.zly2006.enclosure.utils.Permission
import com.github.zly2006.enclosure.utils.Serializable2Text.SerializationSettings
import com.github.zly2006.enclosure.utils.TrT
import com.github.zly2006.enclosure.utils.Utils
import com.github.zly2006.enclosure.utils.gold
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.command.argument.UuidArgumentType
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.*

fun BuilderScope<*>.registerPermissions() {
    // type uuid permission area
    fun <T : argT> BuilderScope<T>.tupa(
        appender: (argT, Command<ServerCommandSource>) -> Unit = { n, c -> n.executes(c) },
        action: CommandContext<ServerCommandSource>.(EnclosureArea, UUID, Permission) -> Unit
    ) {
        optionalEnclosure(listOf("user", "global", "uuid"), { l, c ->
            when (l) {
                "user" -> literal(l) {
                    argument(offlinePlayerArgument()) {
                        argument(permissionArgument(Permission.Target.Player)) {
                            appender(parent, c)
                        }
                    }
                }

                "global" -> literal(l) {
                    argument(permissionArgument(Permission.Target.Enclosure)) {
                        appender(parent, c)
                    }
                }

                "uuid" -> literal(l) {
                    argument("uuid", UuidArgumentType.uuid()) {
                        argument(permissionArgument(Permission.Target.Player)) {
                            appender(parent, c)
                        }
                    }
                }
            }
        }) { a, l ->
            val permission = Permission.getValue(StringArgumentType.getString(this, "permission"))
                ?: error(TrT.of("enclosure.message.invalid_permission"), this)
            when (l) {
                "user" -> {
                    val uuid = getOfflineUUID(this)
                    action(a, uuid, permission)
                }

                "global" -> {
                    val uuid = CONSOLE
                    action(a, uuid, permission)
                }

                "uuid" -> {
                    val uuid = UuidArgumentType.getUuid(this, "uuid")
                    action(a, uuid, permission)
                }
            }
        }
    }
    literal("set") {
        permission("enclosure.command.set", BuilderScope.Companion.DefaultPermission.TRUE)
        tupa({ n, c -> n.then(optionalBooleanArgument().executes(c)) }) { area, uuid, permission ->
            source.player?.let {
                if (!area.hasPerm(it, Permission.ADMIN)) {
                    error(Permission.ADMIN.getNoPermissionMsg(it), this)
                }
            }
            val value: Boolean? = when (getArgument("value", String::class.java)) {
                "true" -> true
                "false" -> false
                else -> null
            }
            val action = {
                area.setPermission(source, uuid, permission, value)
                area.markDirty()
                source.sendFeedback(
                    {
                        TrT.of(
                            "enclosure.message.set_permission",
                            Utils.getDisplayNameByUUID(uuid),
                            permission.serialize(SerializationSettings.Summarize, source.player),
                            value?.toString() ?: "none",
                            area.fullName
                        ).formatted(Formatting.YELLOW)
                    }, true
                )
            }
            val warning = if (permission === Permission.ADMIN) {
                TrT.of("enclosure.message.setting_admin").formatted(Formatting.RED)
            } else if (permission.permissions.size > 1) {
                TrT.of("enclosure.message.setting_multiple").formatted(Formatting.RED)
            } else null
            if (warning != null) {
                ConfirmManager.confirm(warning, source.player, false, action)
                if (EnclosureInstalledC2SPacket.isInstalled(source.player)) {
                    // update client
                    ServerPlayNetworking.send(
                        source.player,
                        SyncPermissionS2CPacket(uuid, area.permissionsMap[uuid].toNbt())
                    )
                }
            } else {
                action()
            }
        }
    }
    literal("check") {
        permission("enclosure.command.check", BuilderScope.Companion.DefaultPermission.TRUE)
        tupa { area, uuid, permission ->
            val textTrue = Text.literal("true").formatted(Formatting.GREEN)
            val textFalse = Text.literal("false").formatted(Formatting.RED)
            area.hasPerm(uuid, permission)
            source.sendMessage(
                TrT.of(
                    "enclosure.message.check_permission",
                    Utils.getDisplayNameByUUID(uuid).gold(),
                    permission.serialize(SerializationSettings.Summarize, source.player).gold(),
                    area.serialize(SerializationSettings.Name, source.player).gold(),
                    if (area.hasPerm(uuid, permission)) textTrue else textFalse
                )
            )
        }
    }

    literal("trust") {
        permission("enclosure.command.trust", BuilderScope.Companion.DefaultPermission.TRUE)
        argument(offlinePlayerArgument()) {
            optionalEnclosure { area ->
                val uuid = getOfflineUUID(this)
                if (area.hasPerm(source.player!!, Permission.ADMIN)) {
                    area.setPermission(source, uuid, Permission.TRUSTED, true)
                    source.sendFeedback(
                        { TrT.of("enclosure.message.added_user", Utils.getDisplayNameByUUID(uuid)) },
                        true
                    )
                } else {
                    error(Permission.ADMIN.getNoPermissionMsg(source.player), this)
                }
            }
        }
    }
    literal("give") {
        permission("enclosure.command.give", BuilderScope.Companion.DefaultPermission.TRUE)
        argument(landArgument()) {
            fun CommandContext<ServerCommandSource>.execute(area: EnclosureArea, uuid: UUID) {
                val target = minecraftServer.playerManager.getPlayer(uuid)
                if (!area.isOwner(source)) {
                    error(TrT.of("enclosure.message.not_owner"), this)
                }
                ConfirmManager.confirm(null, source.player) {
                    val limitsOfReceiver = getLimits(this)
                    if (!source.hasPermissionLevel(4)) {
                        val count = ServerMain.getAllEnclosuresForSuggestion(uuid).size.toLong()
                        if (count > limitsOfReceiver.maxLands) {
                            error(
                                TrT.of("enclosure.message.rcle.receiver")
                                    .append(limitsOfReceiver.maxLands.toString()), this
                            )
                        }
                    }
                    area.setPermission(source, area.owner, Permission.ALL, null)
                    area.owner = uuid
                    area.setPermission(source, uuid, Permission.ALL, true)
                    source.sendFeedback({
                        TrT.of("enclosure.message.given.1")
                            .append(area.serialize(SerializationSettings.Name, source.player))
                            .append(TrT.of("enclosure.message.given.2"))
                            .append(Utils.getDisplayNameByUUID(uuid))
                    }, true)
                    target?.sendMessage(
                        TrT.of("enclosure.message.received.1")
                            .append(area.serialize(SerializationSettings.Name, source.player))
                            .append(TrT.of("enclosure.message.received.2"))
                            .append(source.displayName)
                    )
                }
            }
            argument(offlinePlayerArgument()) {
                executes {
                    val res = getEnclosure(this)
                    val uuid = getOfflineUUID(this)
                    execute(res, uuid)
                }
            }
            argument("uuid", UuidArgumentType.uuid()) {
                executes {
                    val res = getEnclosure(this)
                    val uuid = UuidArgumentType.getUuid(this, "uuid")
                    execute(res, uuid)
                }
            }
        }
    }
}
