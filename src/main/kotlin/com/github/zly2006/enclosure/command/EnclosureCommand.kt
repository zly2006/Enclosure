package com.github.zly2006.enclosure.command

import com.github.zly2006.enclosure.*
import com.github.zly2006.enclosure.access.PlayerAccess
import com.github.zly2006.enclosure.exceptions.PermissionTargetException
import com.github.zly2006.enclosure.gui.EnclosureScreenHandler
import com.github.zly2006.enclosure.network.EnclosureInstalledC2SPacket
import com.github.zly2006.enclosure.utils.*
import com.github.zly2006.enclosure.utils.Serializable2Text.SerializationSettings
import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import net.minecraft.command.CommandSource
import net.minecraft.command.argument.*
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.*
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import java.util.*
import java.util.function.Consumer
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty

typealias argT = ArgumentBuilder<ServerCommandSource, *>

@JvmField
val CONSOLE = UUID(0, 0)

private fun error(sst: Text, context: CommandContext<ServerCommandSource>): Nothing {
    throw SimpleCommandExceptionType(sst).createWithContext(StringReader(context.input))
}

class BuilderScope<T: argT>(var parent: T) {
    fun literal(name: String, action: BuilderScope<*>.() -> Unit) {
        val node = CommandManager.literal(name)
        BuilderScope(node).apply(action)
        parent.then(node)
    }

    fun argument(name: String, type: ArgumentType<*>, action: BuilderScope<*>.() -> Unit) {
        val node = CommandManager.argument(name, type)
        argument(node, action)
    }

    fun argument(node: RequiredArgumentBuilder<ServerCommandSource, *>, action: BuilderScope<*>.() -> Unit) {
        BuilderScope(node).apply(action)
        parent.then(node)
    }

    fun executes(action: CommandContext<ServerCommandSource>.() -> Unit) {
        parent.executes {
            try {
                action(it)
                1
            } catch (e: PermissionTargetException) {
                error(e.text, it)
            } catch (e: CommandSyntaxException) {
                throw e
            } catch (e: java.lang.Exception) {
                LOGGER.error("Error while executing command: " + it.input, e)
                error(TrT.of("enclosure.message.error").append(e.message), it)
            }
        }
    }

    fun paged(commandSupplier: CommandContext<ServerCommandSource>.() -> String? = { null }, listSupplier: CommandContext<ServerCommandSource>.() -> List<Text>) {
        val size = 5
        val action: CommandContext<ServerCommandSource>.(Int) -> Unit = { p ->
            val command = commandSupplier()
            val list = listSupplier()
            val totalPage: Int = (list.size + size - 1) / size
            var page = p
            if (p < 1 || p > totalPage) { // 如果选取页码超过范围限制，则采用第一页
                page = 1
            }
            val firstPage = page == 1
            val lastPage = page >= totalPage

            val ret: MutableText = TrT.of("enclosure.menu.page.0")
                .append(page.toString())
                .append(TrT.of("enclosure.menu.page.1"))
                .append(totalPage.toString())
                .append("\n")

            var i: Int = size * (page - 1)
            while (i < size * page && i < list.size) {
                ret.append(list[i])
                ret.append("\n")
                i++
            }

            if (command != null) {
                ret.append(
                    TrT.of("enclosure.menu.previous").setStyle(
                        if (firstPage) Style.EMPTY.withColor(Formatting.GRAY)
                        else Style.EMPTY.withColor(Formatting.DARK_GREEN)
                            .hoverText(Text.of("Page ${page - 1}"))
                            .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "$command ${page - 1}"))
                    )
                )
                ret.append("    ")
                ret.append(
                    TrT.of("enclosure.menu.next").setStyle(
                        if (lastPage) Style.EMPTY.withColor(Formatting.GRAY)
                        else Style.EMPTY.withColor(Formatting.DARK_GREEN)
                            .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Page ${page + 1}")))
                            .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "$command ${page + 1}"))
                    )
                )
            }
            source.sendMessage(ret)
        }
        executes { action(0) }
        argument("page", IntegerArgumentType.integer(0)) {
            executes {
                action(IntegerArgumentType.getInteger(this, "page"))
            }
        }
    }

    /**
     * @param action 传入参数表示是否包含[node]
     */
    private fun optionalNode(node: argT, action: BuilderScope<*>.(Boolean) -> Unit) {
        BuilderScope(node).apply { action(true) }
        parent.then(node)
        BuilderScope(parent).apply { action(false) }
    }

    fun optionalEnclosure(action: CommandContext<ServerCommandSource>.(EnclosureArea) -> Unit) {
        optionalNode(landArgument()) {
            executes {
                if (it) {
                    val enclosure = getEnclosure(this)
                    action(enclosure)
                } else {
                    val blockPos = BlockPos.ofFloored(source.position)
                    ServerMain.getAllEnclosures(source.world).getArea(
                        blockPos
                    )?.areaOf(blockPos)?.let { action(it) }
                        ?: error(TrT.of("enclosure.message.no_enclosure"), this)
                }
            }
        }
    }

    fun optionalEnclosure(action: CommandContext<ServerCommandSource>.(EnclosureArea) -> Unit, builder: (argT, Command<ServerCommandSource>) -> Unit) {
        optionalEnclosure(listOf(0), { _, c -> builder(parent, c) }) { a, _ -> action(a) }
    }

    fun <T: Any?> optionalEnclosure(list: List<T>, builder: BuilderScope<*>.(T, Command<ServerCommandSource>) -> Unit, action: CommandContext<ServerCommandSource>.(EnclosureArea, T) -> Unit) {
        list.forEach { t ->
            val node = landArgument()
            BuilderScope(node).apply {
                builder(t, Command {
                    val enclosure = getEnclosure(it)
                    it.action(enclosure, t)
                    1
                })
            }
            BuilderScope(parent).apply {
                builder(t, Command {
                    val blockPos = BlockPos.ofFloored(it.source.position)
                    ServerMain.getAllEnclosures(it.source.world).getArea(blockPos)
                        ?.areaOf(blockPos)?.let { area -> action(it, area, t) }
                        ?: error(TrT.of("enclosure.message.no_enclosure"), it)
                    1
                })
            }
            parent.then(node)
        }
    }


    companion object {
        enum class DefaultPermission {
            TRUE, FALSE, OP;

            fun get(source: ServerCommandSource) =
                when (this) {
                    TRUE -> true
                    FALSE -> false
                    OP -> source.hasPermissionLevel(4)
                }
        }

        val map = mutableMapOf<String, DefaultPermission>(
            "enclosure.bypass" to DefaultPermission.OP
        )
    }
    fun permission(s: String, defaultPermission: DefaultPermission) {
        val old = parent.requirement
        if (!map.containsKey(s)) {
            map[s] = defaultPermission
        }
        parent.requires { source ->
            checkPermission(source, s) && old.test(source)
        }
    }
}

private fun getEnclosure(context: CommandContext<ServerCommandSource>): EnclosureArea {
    return ServerMain.getEnclosure(StringArgumentType.getString(context, "land"))
        ?: error(TrT.of("enclosure.message.no_enclosure"), context)
}

private fun permissionArgument(target: Permission.Target): RequiredArgumentBuilder<ServerCommandSource, String> {
    return CommandManager.argument("permission", StringArgumentType.word())
        .suggests { _, builder ->
            CommandSource.suggestMatching(Permission.suggest(target), builder)
        }
}

private fun offlinePlayerArgument(): RequiredArgumentBuilder<ServerCommandSource, String> {
    return CommandManager.argument("player", StringArgumentType.string())
        .suggests { _, builder ->
            CommandSource.suggestMatching(minecraftServer.userCache?.byName?.keys ?: emptySet(), builder)
        }
}

fun sessionOf(source: ServerCommandSource): Session {
    return ServerMain.playerSessions[source.player?.uuid ?: CONSOLE]!!
}

private fun landArgument(): RequiredArgumentBuilder<ServerCommandSource, String> {
    return CommandManager.argument("land", StringArgumentType.string())
        .suggests { _, builder: SuggestionsBuilder ->
            val res = builder.remainingLowerCase
            if (res.contains(".")) {
                val enclosure = ServerMain.getEnclosure(
                    res.substring(
                        0,
                        res.lastIndexOf('.')
                    )
                )
                if (enclosure is Enclosure) {
                    val subRes = res.substring(res.lastIndexOf('.') + 1)
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

private fun checkSession(context: CommandContext<ServerCommandSource>) {
    val session = sessionOf(context.source)
    if (!session.enabled) {
        error(TrT.of("enclosure.message.null_select_point"), context)
    }
}

private fun getLimits(context: CommandContext<ServerCommandSource>) = ServerMain.getLimitsForPlayer(
    context.source.player ?: throw EntityArgumentType.PLAYER_NOT_FOUND_EXCEPTION.create()
) ?: error(TrT.of("enclosure.message.no_limits_available"), context)

private fun checkSessionSize(session: Session, context: CommandContext<ServerCommandSource>) {
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
            val count = ServerMain.getAllEnclosures(context.source.uuid).size.toLong()
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

private fun getOfflineUUID(context: CommandContext<ServerCommandSource>): UUID {
    try {
        return UUID.fromString(StringArgumentType.getString(context, "player"))
    } catch (_: Exception) { }
    return Utils.getUUIDByName(StringArgumentType.getString(context, "player"))
        ?: error(TrT.of("enclosure.message.player_not_found"), context)
}

fun BuilderScope<*>.registerConfirmCommand() {
    literal("confirm") {
        executes {
            ConfirmManager.runnableMap.remove(source.uuid)?.run { runnable() }
                ?: error(TrT.of("enclosure.message.no_task_to_confirm"), this)
        }
    }
}

fun register(dispatcher: CommandDispatcher<ServerCommandSource>): LiteralCommandNode<ServerCommandSource>? {
    val node = BuilderScope(CommandManager.literal("enclosure")).apply {
        registerConfirmCommand()
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
                    source.sendMessage(TrT.of("enclosure.about.version.client").append(version.friendlyString))
                }
                source.sendMessage(TrT.of("enclosure.about.copyright"))
            }
        }
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
                                            ClickEvent.Action.RUN_COMMAND,
                                            "/enclosure info " + enclosure.fullName
                                        )
                                    )
                                })
                    }
                }
            }
            literal("perm-info") {
                permission("enclosure.command.admin.perm_info", BuilderScope.Companion.DefaultPermission.OP)
                argument(permissionArgument(Permission.Target.Both)) {
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
                        source.sendMessage(Text.literal(it.key.nameForScoreboard + ": " + it.value.friendlyString))
                    }
                }
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
                    try {
                        Text.literal("\n")
                            .append(TrT.limit(field).append(": ").gold())
                            .append(field[limits].toString())
                    } catch (ignored: IllegalAccessException) {
                        null
                    }
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
                        val world = DimensionArgumentType.getDimensionArgument(this, "world")
                        "/enclosure list world ${world.registryKey.value}"
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
                        val list = ServerMain.getAllEnclosures(uuid)
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
        literal("select") {
            permission("enclosure.command.select", BuilderScope.Companion.DefaultPermission.TRUE)
            fun <T : argT> BuilderScope<T>.setPos(name: String, action: Session.(BlockPos) -> Unit) {
                literal(name) {
                    argument("position", BlockPosArgumentType.blockPos()) {
                        executes {
                            val pos = BlockPosArgumentType.getBlockPos(this, "position")
                            val session = sessionOf(source)
                            session.world = source.world
                            session.action(pos)
                            session.enable()
                            source.sendMessage(TrT.of("enclosure.message.set_$name").append(pos.toShortString()))
                            session.trySync()
                        }
                    }
                }
            }
            setPos("pos_1") { pos1 = it }
            setPos("pos_2") { pos2 = it }
            literal("world") {
                argument("world", DimensionArgumentType.dimension()) {
                    executes {
                        val world = DimensionArgumentType.getDimensionArgument(this, "world")
                        val session = sessionOf(source)
                        session.world = world
                        source.sendMessage(TrT.of("enclosure.message.selected_world", world.registryKey.value.toString()))
                        session.trySync()
                    }
                }
            }
            literal("view") {
                executes {
                    val session = sessionOf(source)
                    checkSession(this)
                    session.trySync()
                    val intersectArea = session.intersect(ServerMain.getAllEnclosures(session.world))
                    source.sendMessage(
                        TrT.of("enclosure.message.select.from")
                            .append(session.pos1.toShortString())
                            .append(TrT.of("enclosure.message.select.to"))
                            .append(session.pos2.toShortString())
                            .append(TrT.of("enclosure.message.select.world"))
                            .append(session.world.registryKey.value.toString())
                    )
                    source.sendMessage(
                        TrT.of("enclosure.message.total_size")
                            .append(session.size().toString())
                    )
                    if (intersectArea != null) {
                        source.sendMessage(
                            TrT.of("enclosure.message.intersected")
                                .append(intersectArea.serialize(SerializationSettings.Name, source.player))
                        )
                    }
                }
            }
            literal("clear") {
                executes {
                    val session = sessionOf(source)
                    session.pos1 = BlockPos.ORIGIN
                    session.pos2 = BlockPos.ORIGIN
                    session.trySync()
                    session.enabled = false
                    source.sendMessage(TrT.of("enclosure.message.select.clear"))
                }
            }
            fun <T, A : argT> BuilderScope<A>.withDirectionAndAmount(name: String, action: Session.(Direction, Int) -> T, key: String = "enclosure.message.$name") {
                literal(name) {
                    argument("amount", IntegerArgumentType.integer(1)) {
                        executes {
                            val executor = source.player ?: error(TrT.of("enclosure.message.not_player"), this)
                            checkSession(this)
                            val session = sessionOf(source)
                            val amount = IntegerArgumentType.getInteger(this, "amount")
                            val direction = Direction.getEntityFacingOrder(executor)[0]
                            session.action(direction, amount)
                            session.trySync()
                            source.sendMessage(
                                TrT.of(key).append(amount.toString())
                                    .append(TrT.of("enclosure.message.resized." + direction.getName()))
                            )
                        }
                    }
                }
            }
            withDirectionAndAmount("shrink", Session::shrink, "enclosure.message.shrunk")
            withDirectionAndAmount("expand", Session::expand, "enclosure.message.expanded")
            withDirectionAndAmount("shift", Session::shift, "enclosure.message.shifted")
            literal("max_height") {
                executes {
                    val session = sessionOf(source)
                    val limits = getLimits(this)
                    checkSession(this)
                    session.run {
                        pos1 = BlockPos(pos1.x, limits.minY, pos1.z)
                        pos2 = BlockPos(
                            pos2.x,
                            limits.maxY.coerceAtMost(limits.maxHeight + limits.minY - 1),
                            pos2.z
                        )
                    }
                    session.trySync()
                    source.sendMessage(TrT.of("enclosure.message.max_height"))
                }
            }
            literal("max_square") {
                executes {
                    val limits = getLimits(this)
                    val session = sessionOf(source)
                    val (x1, y1, z1, x2, y2, z2) = session.ordered()
                    val expandX = (limits.maxXRange - x2 + x1 - 1) / 2
                    val expandZ = (limits.maxZRange - z2 + z1 - 1) / 2
                    session.pos1 = BlockPos(x1 - expandX, y1, z1 - expandZ)
                    session.pos2 = BlockPos(x2 + expandX, y2, z2 + expandZ)
                    session.trySync()
                    source.sendMessage(TrT.of("enclosure.message.max_square"))
                }
            }
            literal("land") {
                optionalEnclosure {
                    val session = sessionOf(source)
                    session.world = it.world
                    session.pos1 = BlockPos(it.minX, it.minY, it.minZ)
                    session.pos2 = BlockPos(it.maxX, it.maxY, it.maxZ)
                    session.enable()
                    session.trySync()
                    source.sendMessage(TrT.of("enclosure.message.selection_updated"))
                }
            }
            literal("resize") {
                optionalEnclosure { area ->
                    val session = sessionOf(source)
                    checkSession(this)
                    checkSessionSize(session, this)
                    if (!area.isOwner(source)) {
                        error(TrT.of("enclosure.message.not_owner"), this)
                    }
                    val after = EnclosureArea(session, "")
                    if (area is Enclosure) {
                        area.subEnclosures.areas.firstOrNull { sub -> !after.includesArea(sub) }?.let {
                            error(TrT.of("enclosure.message.sub_enclosure_outside", it.fullName), this)
                        }
                    }
                    val (minX, minY, minZ, maxX, maxY, maxZ) = session.ordered()
                    ConfirmManager.confirm(
                        TrT.of(
                            "enclosure.message.resizing",
                            area.fullName,
                            minX - area.minX,
                            minY - area.minY,
                            minZ - area.minZ,
                            maxX - area.maxX,
                            maxY - area.maxY,
                            maxZ - area.maxZ
                        ),
                        source.player
                    ) {
                        area.minX = minX
                        area.minY = minY
                        area.minZ = minZ
                        area.maxX = maxX
                        area.maxY = maxY
                        area.maxZ = maxZ
                        source.sendMessage(
                            TrT.of("enclosure.message.resized")
                                .append(area.serialize(SerializationSettings.Name, source.player))
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
            literal("tp") {
                permission("enclosure.command.tp", BuilderScope.Companion.DefaultPermission.TRUE)
                argument(landArgument()) {
                    executes {
                        val player = source.player!!
                        val lastTeleportTimeSpan =
                            System.currentTimeMillis() - (player as PlayerAccess).lastTeleportTime
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
                        (player as PlayerAccess).lastTeleportTime = System.currentTimeMillis()
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
                    if (!area.isInner(BlockPos.ofFloored(source.position))) {
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
        literal("rename") {
            permission("enclosure.command.rename", BuilderScope.Companion.DefaultPermission.TRUE)
            argument(landArgument()) {
                argument("name", StringArgumentType.word()) {
                    executes {
                        val res = getEnclosure(this)
                        val name = StringArgumentType.getString(this, "name")
                        if (name.length > ServerMain.commonConfig.maxEnclosureNameLength) {
                            error(TrT.of("enclosure.message.res_name_too_long"), this)
                        }
                        if (name.chars().anyMatch { c: Int -> !Character.isLetterOrDigit(c) && c != '_'.code }) {
                            error(TrT.of("enclosure.message.res_name_invalid"), this)
                        }
                        if (!source.hasPermissionLevel(4) && source.player != null && !res.isOwner(source)) {
                            error(TrT.of("enclosure.message.not_owner"), this)
                        }
                        if (ServerMain.getEnclosure(name) != null) {
                            error(TrT.of("enclosure.message.name_in_use"), this)
                        }
                        val list =
                            (res.father as? Enclosure)?.subEnclosures ?: ServerMain.getAllEnclosures(res.world)
                        list.remove(res.name)
                        val oldName = res.name
                        res.name = name
                        list.addArea(res)
                        res.markDirty()
                        source.sendFeedback({ TrT.of("enclosure.message.renamed", oldName, name) }, false)
                    }
                }
            }
        }
        literal("remove") {
            permission("enclosure.command.remove", BuilderScope.Companion.DefaultPermission.TRUE)
            argument(landArgument()) {
                executes {
                    val res = getEnclosure(this)
                    if (!res.isOwner(source)) {
                        error(TrT.of("enclosure.message.not_owner"), this)
                    }
                    ConfirmManager.confirm(null, source.player) {
                        val list =
                            (res.father as? Enclosure)?.subEnclosures ?: ServerMain.getAllEnclosures(res.world)
                        val success = res.father?.let {
                            it.onRemoveChild(res)
                            true
                        } ?: list.remove(res.name)
                        if (success) {
                            source.sendMessage(TrT.of("enclosure.message.deleted").append(res.fullName))
                            LOGGER.info("${source.name} removed ${res.fullName}")
                        } else {
                            error(TrT.of("enclosure.message.no_enclosure"), this)
                        }
                    }
                }
            }
        }
        literal("trust") {
            permission("enclosure.command.trust", BuilderScope.Companion.DefaultPermission.TRUE)
            optionalEnclosure({ area ->
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
            }, { node, command ->
                node.then(offlinePlayerArgument().executes(command))
            })
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
                            val count = ServerMain.getAllEnclosures(uuid).size.toLong()
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
        run {
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
        }
        literal("message") {
            permission("enclosure.command.message", BuilderScope.Companion.DefaultPermission.TRUE)
            fun <T : argT> BuilderScope<T>.withLeaveEnter(
                builder: (argT, Command<ServerCommandSource>) -> Unit = { n, c -> n.executes(c) },
                action: CommandContext<ServerCommandSource>.(EnclosureArea, String) -> Unit
            ) {
                optionalEnclosure(listOf("enter", "leave"), { l, c ->
                    literal(l) {
                        builder(parent, c)
                    }
                }, action)
            }
            fun delegate(area: EnclosureArea, l: String): ReadWriteProperty<Any?, String> {
                return Delegates.observable(
                    when (l) {
                        "enter" -> area.enterMessage
                        "leave" -> area.leaveMessage
                        else -> error("Unknown arg type")
                    }
                ) { _, _, new: String ->
                    when (l) {
                        "enter" -> area.enterMessage = new
                        "leave" -> area.leaveMessage = new
                        else -> error("Unknown arg type")
                    }
                    area.markDirty()
                }
            }
            literal("view") {
                val ctp = TrT.of("enclosure.message.click_to_copy").setStyle(Style.EMPTY.withColor(Formatting.AQUA))
                withLeaveEnter { area, l ->
                    val str by delegate(area, l)
                    val msg = str.replace("&", "&&")
                        .replace("§", "&")
                    if (msg == "#none") {
                        source.sendMessage(TrT.of("enclosure.message.null_message", area.fullName))
                    } else if (msg.isEmpty()) {
                        source.sendMessage(TrT.of("enclosure.message.default_message"))
                    } else {
                        source
                            .sendMessage(
                                Text.literal(msg).append(" ").append(ctp).setStyle(
                                    Style.EMPTY.hoverText(ctp)
                                        .withClickEvent(ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, msg))
                                )
                            )
                    }
                }
            }
            literal("set") {
                withLeaveEnter({ n, c ->
                    n.then(CommandManager.argument("message", StringArgumentType.greedyString())
                        .suggests { _, builder->
                            builder.suggest(ServerMain.commonConfig.defaultEnterMessage.replace("&", "&&").replace("§", "&"))
                                .suggest(ServerMain.commonConfig.defaultLeaveMessage.replace("&", "&&").replace("§", "&"))
                                .suggest("#none")
                                .suggest("#default")
                                .buildFuture()
                        }
                        .executes(c))
                }) { area, l ->
                    if (!area.hasPerm(source.player!!, Permission.ADMIN)) {
                        error(Permission.ADMIN.getNoPermissionMsg(source.player), this)
                    }
                    var str by delegate(area, l)
                    str = StringArgumentType.getString(this, "message").let {
                        if (it == "#default") {
                            ""
                        } else {
                            it.replace("&", "§").replace("§§", "&")
                        }
                    }
                    source.sendMessage(TrT.of("enclosure.message.set_message", l))
                }
            }
            literal("rich") {
                parent.requires { ServerMain.commonConfig.allowRichMessage }
                withLeaveEnter({ n, c ->
                    n.then(CommandManager.argument("message", TextArgumentType.text()).executes(c))
                }) { area, l ->
                    if (!area.hasPerm(source.player!!, Permission.ADMIN)) {
                        error(Permission.ADMIN.getNoPermissionMsg(source.player), this)
                    }
                    var str by delegate(area, l)
                    val message = Text.Serialization.toJsonTree(TextArgumentType.getTextArgument(this, "message"))
                    str = "#rich:$message"
                    source.sendMessage(TrT.of("enclosure.message.set_message", l))
                }
            }
        }
        literal("experimental") {
            permission("enclosure.command.experimental", BuilderScope.Companion.DefaultPermission.OP)
            literal("backup") {
                argument(landArgument()) {
                    executes {
                        val area = getEnclosure(this)
                        if (!area.hasPerm(source.player!!, Permission.ADMIN)) {
                            error(Permission.ADMIN.getNoPermissionMsg(source.player), this)
                        }
                        if (ServerMain.backupManager.backup(area, source)) {
                            source.sendFeedback(
                                { TrT.of("enclosure.message.backup", area.fullName).formatted(Formatting.YELLOW) }, true
                            )
                        } else {
                            error(TrT.of("enclosure.message.backup_failed", area.fullName), this)
                        }
                    }
                }
            }
        }
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
