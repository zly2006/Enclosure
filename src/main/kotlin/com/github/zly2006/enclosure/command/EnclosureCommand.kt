package com.github.zly2006.enclosure.command

import com.github.zly2006.enclosure.Enclosure
import com.github.zly2006.enclosure.EnclosureArea
import com.github.zly2006.enclosure.ServerMain
import com.github.zly2006.enclosure.gui.EnclosureScreenHandler
import com.github.zly2006.enclosure.minecraftServer
import com.github.zly2006.enclosure.network.config.EnclosureInstalledC2SPacket
import com.github.zly2006.enclosure.utils.*
import com.github.zly2006.enclosure.utils.Serializable2Text.SerializationSettings
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.CommandSource
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import java.util.*

typealias argT = ArgumentBuilder<ServerCommandSource, *>

@JvmField
val CONSOLE = UUID(0, 0)

fun error(text: Text, context: CommandContext<ServerCommandSource>): Nothing {
    throw SimpleCommandExceptionType(text).createWithContext(StringReader(context.input))
}

fun getEnclosure(context: CommandContext<ServerCommandSource>): EnclosureArea {
    return ServerMain.getEnclosure(StringArgumentType.getString(context, "land"))
        ?: error(TrT.of("enclosure.message.no_enclosure"), context)
}

fun permissionArgument(target: Permission.Target): RequiredArgumentBuilder<ServerCommandSource, String> {
    return CommandManager.argument("permission", StringArgumentType.word())
        .suggests { _, builder ->
            CommandSource.suggestMatching(Permission.suggest(target), builder)
        }
}

fun offlinePlayerArgument(): RequiredArgumentBuilder<ServerCommandSource, String> {
    return CommandManager.argument("player", StringArgumentType.string())
        .suggests { _, builder ->
            CommandSource.suggestMatching(minecraftServer.userCache?.byName?.keys ?: emptySet(), builder)
        }
}

fun sessionOf(source: ServerCommandSource): Session {
    return ServerMain.playerSessions[source.player?.uuid ?: CONSOLE]!!
}

fun landArgument(): RequiredArgumentBuilder<ServerCommandSource, String> {
    return CommandManager.argument("land", StringArgumentType.string())
        .suggests { context, builder: SuggestionsBuilder ->
            val res = builder.remainingLowerCase
            if (res.contains(".")) {
                val enclosure = ServerMain.getEnclosure(res.substringBeforeLast('.'))
                if (enclosure is Enclosure) {
                    val subRes = res.substringAfterLast('.')
                    enclosure.subEnclosures.areas
                        .filter { it.name.lowercase().startsWith(subRes) }
                        .map { it.fullName }
                        .forEach { text -> builder.suggest(text) }
                }
            } else {
                ServerMain.getAllEnclosures()
                    .map { it.fullName }
                    .filter { it.lowercase().startsWith(res) }
                    .forEach { text -> builder.suggest(text) }
            }
            builder.buildFuture()
        }
}

fun optionalBooleanArgument(): RequiredArgumentBuilder<ServerCommandSource, String> {
    return CommandManager.argument("value", StringArgumentType.word())
        .suggests { _, builder: SuggestionsBuilder ->
            builder.suggest("true")
            builder.suggest("false")
            builder.suggest("none")
            builder.buildFuture()
        }
}

val ServerCommandSource.uuid: UUID
    get() = player?.uuid ?: CONSOLE

fun checkSession(context: CommandContext<ServerCommandSource>) {
    val session = sessionOf(context.source)
    if (!session.enabled) {
        error(TrT.of("enclosure.message.null_select_point"), context)
    }
}

fun getLimits(context: CommandContext<ServerCommandSource>) = ServerMain.getLimitsForPlayer(
    context.source.player ?: throw EntityArgumentType.PLAYER_NOT_FOUND_EXCEPTION.create()
) ?: error(TrT.of("enclosure.message.no_limits_available"), context)

fun checkSessionSize(session: Session, context: CommandContext<ServerCommandSource>) {
    if (!context.source.hasPermissionLevel(4)) {
        val text = session.isValid(getLimits(context))
        if (text != null && !context.source.hasPermissionLevel(4)) {
            throw SimpleCommandExceptionType(text).createWithContext(StringReader(context.input))
        }
    }
}

fun getOfflineUUID(context: CommandContext<ServerCommandSource>): UUID {
    try {
        return UUID.fromString(StringArgumentType.getString(context, "player"))
    } catch (_: Exception) { }
    return Utils.getUUIDByName(StringArgumentType.getString(context, "player"))
        ?: error(TrT.of("enclosure.message.player_not_found"), context)
}

fun BuilderScope<*>.registerConfirmCommand() {
    literal("confirm") {
        executes {
            if (!ConfirmManager.execute(source.uuid)) {
                error(TrT.of("enclosure.message.no_task_to_confirm"), this)
            }
        }
    }
}

fun register(dispatcher: CommandDispatcher<ServerCommandSource>, access: CommandRegistryAccess): LiteralCommandNode<ServerCommandSource>? {
    val node = BuilderScope(CommandManager.literal("enclosure")).apply {
        literal("help") {
            permission("enclosure.command.help", BuilderScope.Companion.DefaultPermission.TRUE)
            executes {
                source.sendMessage(TrT.of("enclosure.help.header"))
                dispatcher.getSmartUsage(dispatcher.root.getChild("enclosure"), source).forEach { (name, s) ->
                    source.sendMessage(Text.literal("/enclosure $s").gold()
                        .append(": ")
                        .append(TrT.of("enclosure.help." + name.name).white())
                    )
                }
            }
            argument("subcommand", StringArgumentType.word()) {
                executes {
                    val name = StringArgumentType.getString(this, "subcommand")
                    val node = dispatcher.root.getChild("enclosure").getChild(name)
                    if (node == null) {
                        source.sendMessage(TrT.of("enclosure.help.no_child", name))
                    } else {
                        source.sendMessage(
                            Text.literal("/enclosure $name").gold()
                                .append(": ")
                                .append(TrT.of("enclosure.help.$name").white())
                        )
                    }
                }
            }
        }
        literal("gui") {
            // Note: this register block only runs once
            BuilderScope.map["enclosure.gui"] = BuilderScope.Companion.DefaultPermission.TRUE
            parent.requires {
                it.isExecutedByPlayer &&
                        checkPermission(it, "enclosure.gui")
            }
            optionalEnclosure {
                val player = source.player!!
                if (EnclosureInstalledC2SPacket.isInstalled(player)) {
                    EnclosureScreenHandler.open(player, it)
                }
            }
        }
        literal("info") {
            permission("enclosure.command.info", BuilderScope.Companion.DefaultPermission.TRUE)
            optionalEnclosure { area ->
                val text = area.serialize(SerializationSettings.BarredFull, source.player)
                if (EnclosureInstalledC2SPacket.isInstalled(source.player)) {
                    text.append(
                        Text.literal("(*)").setStyle(
                            Style.EMPTY.withColor(Formatting.AQUA)
                                .hoverText(Text.translatable("enclosure.message.suggest_gui"))
                                .clickRun("/enclosure gui ${area.fullName}")
                        )
                    )
                }
                source.sendMessage(text)
            }
        }
        registerConfirmCommand()
        registerAdmin()
        registerAbout()
        registerList()

        registerCreate()
        registerRename()
        registerRemove()

        registerTeleport()
        registerSelection()
        registerPermissions()
        registerMessages(access)

        literal("experimental") {
            registerMusic()
            registerForceLoad()
        }
    }
    return dispatcher.register(node.parent)
}

@Suppress("ReplaceSizeCheckWithIsNotEmpty")
fun isPositionSafe(world: ServerWorld, teleportPos: Vec3d, player: ServerPlayerEntity): Boolean {
    // load chunks
    val pos = BlockPos.ofFloored(teleportPos)
    world.getChunk(pos)
    val collisionBox = Box(
        teleportPos.x - 0.3,
        teleportPos.y,
        teleportPos.z - 0.3,
        teleportPos.x + 0.3,
        teleportPos.y + 1.8,
        teleportPos.z + 0.3
    )
    val collisions = world.getCollisions(null, collisionBox)
    val hasCollision = collisions.count() != 0
    val supportingBox = Box(
        teleportPos.x - 0.3,
        teleportPos.y - 1e-4,
        teleportPos.z - 0.3,
        teleportPos.x + 0.3,
        teleportPos.y,
        teleportPos.z + 0.3
    )
    val supporting = world.getCollisions(null, supportingBox)
    val hasSupportingBlock = supporting.count() != 0
    val hasFluid = world.containsFluid(collisionBox)
    return hasSupportingBlock && !hasCollision && !hasFluid
}

fun Session.enable() {
    if (!enabled) {
        enabled = true
    }
}
