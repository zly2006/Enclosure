package com.github.zly2006.enclosure.command

import com.github.zly2006.enclosure.*
import com.github.zly2006.enclosure.access.PlayerAccess
import com.github.zly2006.enclosure.gui.EnclosureScreenHandler
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
import net.minecraft.command.argument.DimensionArgumentType
import net.minecraft.command.argument.EntityArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.ClickEvent
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import java.util.*
import java.util.function.Consumer

typealias argT = ArgumentBuilder<ServerCommandSource, *>

@JvmField
val CONSOLE = UUID(0, 0)

fun error(sst: Text, context: CommandContext<ServerCommandSource>): Nothing {
    throw SimpleCommandExceptionType(sst).createWithContext(StringReader(context.input))
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

private val ServerCommandSource.uuid: UUID
    get() = if (player != null) player!!.uuid else CONSOLE

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

private fun createEnclosure(context: CommandContext<ServerCommandSource>) {
    val name = StringArgumentType.getString(context, "name")
    if (ServerMain.getEnclosure(name) != null) {
        error(TrT.of("enclosure.message.name_in_use"), context)
    }
    if (!context.source.hasPermissionLevel(4)) {
        if (name.length > ServerMain.commonConfig.maxEnclosureNameLength) {
            error(TrT.of("enclosure.message.res_name_too_long"), context)
        }
    }
    if (name.chars().anyMatch { c: Int -> !Character.isLetterOrDigit(c) && c != '_'.code }) {
        error(TrT.of("enclosure.message.res_name_invalid"), context)
    }
    val session = sessionOf(context.source)
    checkSession(context)
    val list = ServerMain.getAllEnclosures(session.world)
    val intersectArea = sessionOf(context.source).intersect(list)
    if (intersectArea != null) {
        error(TrT.of("enclosure.message.intersected") + intersectArea.serialize(SerializationSettings.Name, context.source.player), context)
    }
    val enclosure = Enclosure(session, name)
    val limits = getLimits(context)
    if (!context.source.hasPermissionLevel(4)) {
        checkSessionSize(session, context)
        if (context.source.player != null) {
            val count = ServerMain.getAllEnclosuresForSuggestion(context.source.uuid).size.toLong()
            if (count >= limits.maxLands) {
                error(
                    TrT.of("enclosure.message.rcle.self")
                        .append(Text.literal(limits.maxLands.toString())), context
                )
            }
        }
    }
    enclosure.changeWorld(session.world)
    session.reset(session.world)
    list.addArea(enclosure)
    context.source.sendMessage(
        TrT.of("enclosure.message.created")
            .append(enclosure.serialize(SerializationSettings.Name, context.source.player))
    )
    LOGGER.info(context.source.name + " created enclosure " + enclosure.name)
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
        registerConfirmCommand()
        registerAdmin()
        literal("about") {
            permission("enclosure.command.about", BuilderScope.Companion.DefaultPermission.TRUE)
            executes {
                val player = source.player
                source.sendMessage(TrT.of("enclosure.about.author"))
                source.sendMessage(TrT.of("enclosure.about.source").styled {
                    it.withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/zly2006/Enclosure"))
                })
                source.sendMessage(TrT.of("enclosure.about.team_page"))
                source.sendMessage(
                    TrT.of("enclosure.about.version.server").append(MOD_VERSION.friendlyString)
                )
                if (player != null && EnclosureInstalledC2SPacket.isInstalled(player)) {
                    val version = EnclosureInstalledC2SPacket.clientVersion(player)
                    source.sendMessage(TrT.of("enclosure.about.version.client").append(version!!.friendlyString))
                }
                source.sendMessage(TrT.of("enclosure.about.copyright"))
            }
        }
        literal("flags") {
            permission("enclosure.command.flags", BuilderScope.Companion.DefaultPermission.TRUE)
            paged({ "/enclosure flags" }) {
                Permission.PERMISSIONS.values.map {
                    it.serialize(SerializationSettings.Full, null)
                }
            }
        }
        literal("limits") {
            permission("enclosure.command.limits", BuilderScope.Companion.DefaultPermission.TRUE)
            executes {
                val limits = getLimits(this)
                val translatable = TrT.of("enclosure.message.limit.header")
                limits.javaClass.fields.mapNotNull { field ->
                    runCatching {
                        Text.literal("\n")
                            .append(TrT.limit(field).append(": ").gold())
                            .append(field[limits].toString())
                    }.getOrNull()
                }.forEach { text -> translatable.append(text) }
                source.sendMessage(translatable)
            }
        }
        literal("list") {
            permission("enclosure.command.list", BuilderScope.Companion.DefaultPermission.TRUE)
            paged({ "/enclosure list" }) {
                ServerMain.getAllEnclosures().map {
                    it.serialize(SerializationSettings.Summarize, null)
                }
            }
            literal("world") {
                argument("world", DimensionArgumentType.dimension()) {
                    paged({
                        val world = getArgument("world", Identifier::class.java)
                        "/enclosure list world $world"
                    }, {
                        val world = DimensionArgumentType.getDimensionArgument(this, "world")
                        ServerMain.getAllEnclosures(world).areas.map {
                            it.serialize(SerializationSettings.Summarize, null)
                        }
                    })
                }
            }
            literal("user") {
                argument(offlinePlayerArgument()) {
                    executes {
                        val uuid = getOfflineUUID(this)
                        val list = ServerMain.getAllEnclosuresForSuggestion(uuid)
                        val ret = TrT.of("enclosure.message.list.user", Utils.getDisplayNameByUUID(uuid), list.size)
                        list.forEach(Consumer { e: Enclosure ->
                            ret.append("\n").append(
                                e.serialize(
                                    SerializationSettings.Summarize,
                                    source.player
                                )
                            )
                        })
                        source.sendMessage(ret)
                    }
                }
            }
        }
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
        literal("auto") {
            permission("enclosure.command.auto", BuilderScope.Companion.DefaultPermission.TRUE)
            argument("name", StringArgumentType.word()) {
                executes {
                    val pos = BlockPos.ofFloored(source.position)
                    val limits = getLimits(this)
                    val session = sessionOf(source)
                    val expandX = (limits.maxXRange - 1) / 2
                    val expandZ = (limits.maxZRange - 1) / 2
                    session.enabled = true
                    session.world = source.world
                    session.pos1 = BlockPos(pos.x - expandX, limits.minY, pos.z - expandZ)
                    session.pos2 = BlockPos(
                        pos.x + expandX,
                        limits.maxY.coerceAtMost(limits.maxHeight + limits.minY - 1),
                        pos.z + expandZ
                    )
                    session.owner = source.uuid
                    createEnclosure(this)
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
                            if (!isPositionSafe(area.world, area.teleportPos!!)) {
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
                    if (ServerMain.commonConfig.showTeleportWarning && !isPositionSafe(area.world, area.teleportPos!!)) {
                        source.sendMessage(
                            TrT.of("enclosure.message.teleport_warning.on_set").formatted(Formatting.YELLOW)
                        )
                    }
                    source.sendMessage(
                        TrT.of("enclosure.message.change_teleport_position.0")
                            .append(area.serialize(SerializationSettings.Name, source.player))
                            .append(TrT.of("enclosure.message.change_teleport_position.1"))
                            .append(
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
        literal("create") {
            permission("enclosure.command.create", BuilderScope.Companion.DefaultPermission.TRUE)
            argument("name", StringArgumentType.word()) {
                executes {
                    createEnclosure(this)
                }
            }
        }
        literal("subzone") {
            permission("enclosure.command.subzone", BuilderScope.Companion.DefaultPermission.TRUE)
            argument("name", StringArgumentType.string()) {
                executes {
                    val name = StringArgumentType.getString(this, "name")
                    if (name.length > ServerMain.commonConfig.maxEnclosureNameLength) {
                        error(TrT.of("enclosure.message.res_name_too_long"), this)
                    }
                    if (name.chars().anyMatch { c: Int -> !Character.isLetterOrDigit(c) && c != '_'.code }) {
                        error(TrT.of("enclosure.message.res_name_invalid"), this)
                    }
                    val session = sessionOf(source)
                    val list = ServerMain.getAllEnclosures(sessionOf(source).world)
                    val area: EnclosureArea = Enclosure(session, name)
                    val enclosure = list.areas.firstOrNull { res ->
                        res is Enclosure && res.includesArea(area)
                    } as Enclosure? ?: error(TrT.of("enclosure.message.no_father_enclosure"), this)
                    if (enclosure.subEnclosures.areas.any { it.name.equals(name, ignoreCase = true) }) {
                        error(TrT.of("enclosure.message.name_in_use"), this)
                    }
                    if (!enclosure.isOwner(source)) {
                        error(TrT.of("enclosure.message.not_owner"), this)
                    }
                    val intersectArea = sessionOf(source).intersect(enclosure.subEnclosures)
                    if (intersectArea != null) {
                        error(
                            TrT.of("enclosure.message.intersected")
                                .append(intersectArea.serialize(SerializationSettings.Name, source.player)), this
                        )
                    }
                    val limits = getLimits(this)
                    if (!source.hasPermissionLevel(4)) {
                        checkSessionSize(session, this)
                        val count = enclosure.subEnclosures.areas.size.toLong()
                        if (count > limits.maxSubLands) {
                            error(
                                TrT.of("enclosure.message.scle").append(Text.literal(limits.maxSubLands.toString())),
                                this
                            )
                        }
                    }
                    area.changeWorld(session.world)
                    enclosure.addChild(area)
                    source.sendMessage(
                        TrT.of("enclosure.message.created")
                            .append(area.serialize(SerializationSettings.Name, source.player))
                    )
                    LOGGER.info("Created subzone {} by {}", area.fullName, source.name)
                }
            }
        }
        registerRename()
        registerRemove()
        registerPermissions()
        registerMessages(access)
    }
    return dispatcher.register(node.parent)
}

@Suppress("ReplaceSizeCheckWithIsNotEmpty")
fun isPositionSafe(world: ServerWorld, teleportPos: Vec3d): Boolean {
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
