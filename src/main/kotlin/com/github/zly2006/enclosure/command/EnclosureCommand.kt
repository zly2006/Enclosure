package com.github.zly2006.enclosure.command

import com.github.zly2006.enclosure.*
import com.github.zly2006.enclosure.access.PlayerAccess
import com.github.zly2006.enclosure.exceptions.PermissionTargetException
import com.github.zly2006.enclosure.gui.EnclosureScreenHandler
import com.github.zly2006.enclosure.network.EnclosureInstalledC2SPacket
import com.github.zly2006.enclosure.network.NetworkChannels
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
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.command.CommandSource
import net.minecraft.command.argument.BlockPosArgumentType
import net.minecraft.command.argument.DimensionArgumentType
import net.minecraft.command.argument.TextArgumentType
import net.minecraft.command.argument.UuidArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.*
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import java.util.*
import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.min
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

    fun paged(command: CommandContext<ServerCommandSource>.() -> String? = { null }, listSupplier: CommandContext<ServerCommandSource>.() -> List<Text>) {
        val size = 5
        val action: CommandContext<ServerCommandSource>.(Int) -> Unit = { p ->
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
}

object ConfirmManager {
    val runnableMap: MutableMap<UUID, () -> Unit> = HashMap()
    fun confirm(player: ServerPlayerEntity?, runnable: () -> Unit) {
        val text = TrT.of("enclosure.message.dangerous")
        text.style = Style.EMPTY
            .hoverText(TrT.of("enclosure.message.confirm_event"))
            .withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/enclosure confirm"))
            .withColor(Formatting.YELLOW)
        if (player == null) {
            runnableMap[CONSOLE] = runnable
            minecraftServer.commandSource.sendMessage(TrT.of("enclosure.message.operation_confirm"))
        } else {
            runnableMap[player.uuid] = runnable
            player.sendMessage(text)
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
            CommandSource.suggestMatching(byUuid.values, builder)
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

private fun checkSessionSize(session: Session, context: CommandContext<ServerCommandSource>) {
    if (!context.source.hasPermissionLevel(4)) {
        val text = session.isValid(ServerMain.limits)
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
        if (name.chars().anyMatch { c: Int -> !Character.isLetterOrDigit(c) && c != '_'.code }) {
            error(TrT.of("enclosure.message.res_name_invalid"), context)
        }
    }
    val session = sessionOf(context.source)
    checkSession(context)
    val list = ServerMain.getAllEnclosures(session.world)
    val intersectArea = sessionOf(context.source).intersect(list)
    if (intersectArea != null) {
        error(TrT.of("enclosure.message.intersected") + intersectArea.serialize(SerializationSettings.Name, context.source.player), context)
    }
    val enclosure = Enclosure(session, name)
    val limits = ServerMain.limits
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

private fun Session.ordered(action: Session.(Int, Int, Int, Int, Int, Int)-> Unit) {
    val x1 = min(pos1.x, pos2.x)
    val y1 = min(pos1.y, pos2.y)
    val z1 = min(pos1.z, pos2.z)
    val x2 = max(pos1.x, pos2.x)
    val y2 = max(pos1.y, pos2.y)
    val z2 = max(pos1.z, pos2.z)
    action(x1, y1, z1, x2, y2, z2)
}

private fun getOfflineUUID(context: CommandContext<ServerCommandSource>): UUID {
    return Utils.getUUIDByName(StringArgumentType.getString(context, "player"))
        ?: error(TrT.of("enclosure.message.player_not_found"), context)
}

fun BuilderScope<*>.registerConfirmCommand() {
    literal("confirm") {
        executes {
            ConfirmManager.runnableMap.remove(source.uuid)?.run { invoke() }
                ?: error(TrT.of("enclosure.message.no_task_to_confirm"), this)
        }
    }
}

fun register(dispatcher: CommandDispatcher<ServerCommandSource>): LiteralCommandNode<ServerCommandSource>? {
    val node = BuilderScope(CommandManager.literal("enclosure")).apply {
        registerConfirmCommand()
        literal("about") {
            executes {
                val player = source.player
                source.sendMessage(TrT.of("enclosure.about.author"))
                source.sendMessage(TrT.of("enclosure.about.translator"))
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
                literal("all") {
                    executes {
                        ServerMain.reloadCommon()
                        ServerMain.reloadLimits()
                        source.sendMessage(Text.literal("Reloaded"))
                    }
                }
                literal("common") {
                    executes {
                        ServerMain.reloadCommon()
                        source.sendMessage(Text.literal("Reloaded"))
                    }
                }
                literal("limits") {
                    executes {
                        ServerMain.reloadLimits()
                        source.sendMessage(Text.literal("Reloaded"))
                    }
                }
            }
            literal("limit_exceeded") {
                literal("size") {
                    executes {
                        ServerMain.getAllEnclosures().flatMap {
                            it.subEnclosures.areas.toList() + it
                        }.map {
                            val session = Session(null)
                            session.pos1 = BlockPos(it.minX, it.minY, it.minZ)
                            session.pos2 = BlockPos(it.maxX, it.maxY, it.maxZ)
                            it to session.isValid(ServerMain.limits)
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
                            if (enclosures.size > ServerMain.limits.maxLands) {
                                source.sendMessage(
                                    Text.literal("Player ")
                                        .append(Utils.getDisplayNameByUUID(owner))
                                        .append(" has too many enclosures: ")
                                        .append(enclosures.size.toString())
                                )
                            }
                        }
                    }
                }
                literal("closest") {
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
                    argument(permissionArgument(Permission.Target.Both)) {
                        executes {
                            val permission = Permission.getValue(StringArgumentType.getString(this, "permission"))
                                ?: error(TrT.of("enclosure.message.invalid_permission"), this)
                            source.sendMessage(
                                Text.empty()
                                    .append(Text.literal("Name: "))
                                    .append(permission.name)
                                    .append(Text.literal(" Target: "))
                                    .append(permission.target.toString())
                                    .append(Text.literal("\nDescription: "))
                                    .append(permission.description)
                                    .append(Text.literal("\nDefault: "))
                                    .append(permission.defaultValue.toString())
                                    .append(Text.literal("\nComponents: "))
                                    .append(permission.permissions.joinToString())
                            )
                        }
                    }
                }
                literal("clients") {
                    executes {
                        EnclosureInstalledC2SPacket.installedClientMod.forEach {
                            source.sendMessage(Text.literal(it.key.entityName + ": " + it.value.friendlyString))
                        }
                    }
                }
            }
        }
        literal("flags") {
            paged({ "/enclosure flags" }) {
                Permission.PERMISSIONS.values.map {
                    it.serialize(SerializationSettings.Full, null)
                }
            }
        }
        literal("limits") {
            executes {
                val limits = ServerMain.limits
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
                        val uuid = Utils.getUUIDByName(StringArgumentType.getString(this, "player"))
                        if (uuid == null) {
                            error(TrT.of("enclosure.message.user_not_found"), this)
                        } else {
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
        }
        literal("help") {
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
            argument("name", StringArgumentType.word()) {
                executes {
                    val pos = BlockPos.ofFloored(source.position)
                    val limits = ServerMain.limits
                    val session = sessionOf(source)
                    val expandX = (limits.maxXRange - 1) / 2
                    val expandZ = (limits.maxZRange - 1) / 2
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
            fun <T : argT> BuilderScope<T>.setPos(name: String, action: Session.(BlockPos) -> Unit) {
                literal(name) {
                    argument("position", BlockPosArgumentType.blockPos()) {
                        executes {
                            val pos = BlockPosArgumentType.getBlockPos(this, "position")
                            val session = sessionOf(source)
                            session.world = source.world
                            session.action(pos)
                            source.sendMessage(TrT.of("enclosure.message.$name", pos.x, pos.y, pos.z))
                            session.trySync()
                        }
                    }
                }
            }
            setPos("pos1") { pos1 = it; enable() }
            setPos("pos2") { pos2 = it; enable() }
            literal("world") {
                argument("world", DimensionArgumentType.dimension()) {
                    executes {
                        val world = DimensionArgumentType.getDimensionArgument(this, "world")
                        val session = sessionOf(source)
                        session.world = world
                        source.sendMessage(TrT.of("enclosure.message.world", world.registryKey.value.toString()))
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
                            source.sendFeedback(
                                TrT.of(key).append(amount.toString())
                                    .append(TrT.of("enclosure.message.resized." + direction.getName())), false
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
                    val limits = ServerMain.limits
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
                    source.sendFeedback(TrT.of("enclosure.message.max_height"), false)
                }
            }
            literal("max_square") {
                executes {
                    val limits = ServerMain.limits
                    sessionOf(source).ordered { x1, y1, z1, x2, y2, z2 ->
                        val expandX = (limits.maxXRange - x2 + x1 - 1) / 2
                        val expandZ = (limits.maxZRange - z2 + z1 - 1) / 2
                        pos1 = BlockPos(x1 - expandX, y1, z1 - expandZ)
                        pos2 = BlockPos(x2 + expandX, y2, z2 + expandZ)
                        trySync()
                    }
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
                    val minX = min(session.pos1.x, session.pos2.x)
                    val minY = min(session.pos1.y, session.pos2.y)
                    val minZ = min(session.pos1.z, session.pos2.z)
                    val maxX = max(session.pos1.x, session.pos2.x)
                    val maxY = max(session.pos1.y, session.pos2.y)
                    val maxZ = max(session.pos1.z, session.pos2.z)
                    area.minX = minX
                    area.minY = minY
                    area.minZ = minZ
                    area.maxX = maxX
                    area.maxY = maxY
                    area.maxZ = maxZ
                    source.sendFeedback(
                        TrT.of("enclosure.message.resized")
                            .append(area.serialize(SerializationSettings.Name, source.player)), false
                    )
                }
            }
        }
        literal("gui") {
            parent.requires { it.isExecutedByPlayer }
            optionalEnclosure {
                val player = source.player!!
                if (EnclosureInstalledC2SPacket.isInstalled(player)) {
                    EnclosureScreenHandler.open(player, it)
                }
            }
        }
        literal("info") {
            optionalEnclosure { area ->
                val text = area.serialize(SerializationSettings.BarredFull, source.player)
                if (EnclosureInstalledC2SPacket.isInstalled(source.player)) {
                    text.append(
                        Text.literal("\n(*)").setStyle(
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
            argument(landArgument()) {
                executes {
                    val player = source.player!!
                    val lastTeleportTimeSpan = System.currentTimeMillis() - (player as PlayerAccess).lastTeleportTime
                    val cd = ServerMain.commonConfig.teleportCooldown
                    if (!source.hasPermissionLevel(4) && cd > 0 && lastTeleportTimeSpan < cd) {
                        error(
                            TrT.of(
                                "enclosure.message.teleport_too_fast",
                                String.format("%.1f", (cd - lastTeleportTimeSpan) / 1000.0)
                            ), this
                        )
                    }
                    (player as PlayerAccess).lastTeleportTime = System.currentTimeMillis()
                    val area = getEnclosure(this)
                    if (!area.hasPerm(player, Permission.COMMAND_TP)) {
                        player.sendMessage(Permission.COMMAND_TP.getNoPermissionMsg(player))
                        return@executes
                    }
                    if (ServerMain.commonConfig.showTeleportWarning) {
                        val world = area.world
                        val pos = Utils.toBlockPos(area.teleportPos)
                        val down = world.getBlockState(pos.down())
                        val state = world.getBlockState(pos)
                        val up = world.getBlockState(pos.up())
                        if (!down.material.blocksMovement() || state.material.blocksMovement() && up.material.blocksMovement()) {
                            source.sendMessage(
                                TrT.of("enclosure.message.teleport_warning").formatted(Formatting.YELLOW)
                            )
                            ConfirmManager.confirm(source.player) { area.teleport(player) }
                        } else {
                            area.teleport(player)
                        }
                    } else {
                        area.teleport(player)
                    }
                }
            }
        }
        literal("create") {
            argument("name", StringArgumentType.word()) {
                executes {
                    createEnclosure(this)
                }
            }
        }
        literal("rename") {
            argument(landArgument()) {
                argument("name", StringArgumentType.word()) {
                    executes {
                        val res = getEnclosure(this)
                        val name = StringArgumentType.getString(this, "name")
                        if (!source.hasPermissionLevel(4) && source.player != null && !res.isOwner(source)) {
                            error(TrT.of("enclosure.message.not_owner"), this)
                        }
                        if (ServerMain.getEnclosure(name) != null) {
                            error(TrT.of("enclosure.message.name_in_use"), this)
                        }
                        val list =
                            (res.father as? Enclosure)?.subEnclosures ?: ServerMain.getAllEnclosures(res.world)
                        list.remove(res.name)
                        res.name = name
                        list.addArea(res)
                        res.markDirty()
                    }
                }
            }
        }
        literal("remove") {
            argument(landArgument()) {
                executes {
                    val res = getEnclosure(this)
                    if (!res.isOwner(source)) {
                        error(TrT.of("enclosure.message.not_owner"), this)
                    }
                    ConfirmManager.confirm(source.player) {
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
            optionalEnclosure({ area ->
                val uuid = getOfflineUUID(this)
                if (source.hasPermissionLevel(4) || area.hasPerm(source.player!!, Permission.ADMIN)) {
                    area.setPermission(source, uuid, Permission.TRUSTED, true)
                    source.sendFeedback(TrT.of("enclosure.message.added_user", Utils.getDisplayNameByUUID(uuid)), true)
                } else {
                    error(Permission.ADMIN.getNoPermissionMsg(source.player), this)
                }
            }, { node, command ->
                node.then(offlinePlayerArgument().executes(command))
            })
        }
        literal("give") {
            argument(landArgument()) {
                argument(offlinePlayerArgument()) {
                    executes {
                        val res = getEnclosure(this)
                        val uuid = getOfflineUUID(this)
                        val target = minecraftServer.playerManager.getPlayer(uuid)
                        if (!res.isOwner(source)) {
                            error(TrT.of("enclosure.message.not_owner"), this)
                        }
                        ConfirmManager.confirm(source.player) {
                            val limitsOfReceiver = ServerMain.limits
                            if (!source.hasPermissionLevel(4)) {
                                val count = ServerMain.getAllEnclosures(uuid).size.toLong()
                                if (count > limitsOfReceiver.maxLands) {
                                    error(
                                        TrT.of("enclosure.message.rcle.receiver")
                                            .append(limitsOfReceiver.maxLands.toString()), this
                                    )
                                }
                            }
                            res.setPermission(source, res.owner, Permission.ALL, null)
                            res.owner = uuid
                            res.setPermission(source, uuid, Permission.ALL, true)
                            source.sendFeedback(
                                TrT.of("enclosure.message.given.1")
                                    .append(res.serialize(SerializationSettings.Name, source.player))
                                    .append(TrT.of("enclosure.message.given.2"))
                                    .append(Utils.getDisplayNameByUUID(uuid)), true
                            )
                            target?.sendMessage(
                                TrT.of("enclosure.message.received.1")
                                    .append(res.serialize(SerializationSettings.Name, source.player))
                                    .append(TrT.of("enclosure.message.received.2"))
                                    .append(Utils.getDisplayNameByUUID(res.owner))
                            )
                        }
                    }
                }
            }
        }
        literal("settp") {
            optionalEnclosure { area ->
                if (!source.hasPermissionLevel(4) && !area.hasPerm(source.player!!, Permission.ADMIN)) {
                    error(Permission.ADMIN.getNoPermissionMsg(source.player), this)
                }
                if (!area.isInner(BlockPos.ofFloored(source.position))) {
                    error(TrT.of("enclosure.message.res_settp_pos_error"), this)
                }
                area.setTeleportPos(source.position, source.rotation.y, source.rotation.x)
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
        literal("subzone") {
            executes {
                val name = StringArgumentType.getString(this, "name")
                if (name.length > ServerMain.commonConfig.maxEnclosureNameLength) {
                    error(TrT.of("enclosure.message.res_name_too_long"), this)
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
                val limits = ServerMain.limits
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
                tupa({ n, c -> n.then(optionalBooleanArgument().executes(c)) }) { area, uuid, permission ->
                    source.player?.let {
                        if (!area.hasPerm(it, Permission.ADMIN)) {
                            error(Permission.ADMIN.getNoPermissionMsg(it), this)
                        }
                    }
                    val value: Boolean? = when(getArgument("value", String::class.java)) {
                        "true" -> true
                        "false" -> false
                        else -> null
                    }
                    val action = {
                        area.setPermission(source, uuid, permission, value)
                        area.markDirty()
                        source.sendFeedback(
                            TrT.of(
                                "enclosure.message.set_permission",
                                Utils.getDisplayNameByUUID(uuid),
                                permission.serialize(SerializationSettings.Summarize, source.player),
                                value?.toString() ?: "none",
                                area.fullName
                            ).formatted(Formatting.YELLOW), true
                        )
                    }
                    val warning = if (permission === Permission.ADMIN) {
                        TrT.of("enclosure.message.setting_admin").formatted(Formatting.RED)
                    } else if (permission.permissions.size > 1) {
                        TrT.of("enclosure.message.setting_multiple").formatted(Formatting.RED)
                    } else null
                    if (warning != null) {
                        if (source.player != null && source.player!!.currentScreenHandler is EnclosureScreenHandler) {
                            val buf = PacketByteBufs.create()
                            buf.writeText(warning)
                            ServerPlayNetworking.send(source.player, NetworkChannels.CONFIRM, buf)
                            ConfirmManager.runnableMap[source.player!!.uuid] = action
                        } else {
                            source.sendMessage(warning)
                            ConfirmManager.confirm(source.player, action)
                        }
                    } else {
                        action()
                    }
                }
            }
            literal("check") {
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
            fun <T : argT> BuilderScope<T>.withLeaveEnter(
                builder: (argT, Command<ServerCommandSource>) -> Unit = {n, c -> n.executes(c)},
                action: CommandContext<ServerCommandSource>.(EnclosureArea, String) -> Unit
            ) {
                optionalEnclosure(listOf("enter", "leave"), { l, c ->
                    literal(l) {
                        builder(parent, c)
                    }
                }, action)
            }
            fun delegate(area: EnclosureArea, l: String): ReadWriteProperty<Any?, String> {
                return Delegates.observable(when(l) {
                    "enter" -> area.enterMessage
                    "leave" -> area.leaveMessage
                    else -> error("Unknown arg type")
                }) { _, _, new: String ->
                    when(l) {
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
                    if (!source.hasPermissionLevel(4) && !area.hasPerm(source.player!!, Permission.ADMIN)) {
                        error(TrT.of("enclosure.message.no_permission"), this)
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
                    if (!source.hasPermissionLevel(4) && !area.hasPerm(source.player!!, Permission.ADMIN)) {
                        error(TrT.of("enclosure.message.no_permission"), this)
                    }
                    var str by delegate(area, l)
                    val message = Text.Serializer.toJson(TextArgumentType.getTextArgument(this, "message"))
                    str = "#rich:$message"
                    source.sendMessage(TrT.of("enclosure.message.set_message", l))
                }
            }
        }
        literal("experimental") {
            literal("backup") {
                argument(landArgument()) {
                    executes {
                        val area = getEnclosure(this)
                        if (!source.hasPermissionLevel(4) && !area.hasPerm(source.player!!, Permission.ADMIN)) {
                            error(TrT.of("enclosure.message.no_permission"), this)
                        }
                        if (ServerMain.backupManager.backup(area, source)) {
                            source.sendFeedback(
                                TrT.of("enclosure.message.backup", area.fullName).formatted(Formatting.YELLOW), true
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

fun Session.enable() {
    if (!enabled) {
        enabled = true
    }
}
