package com.github.zly2006.enclosure.command

import com.github.zly2006.enclosure.EnclosureArea
import com.github.zly2006.enclosure.LOGGER
import com.github.zly2006.enclosure.ServerMain
import com.github.zly2006.enclosure.exceptions.PermissionTargetException
import com.github.zly2006.enclosure.minecraftServer
import com.github.zly2006.enclosure.utils.TrT
import com.github.zly2006.enclosure.utils.checkPermission
import com.github.zly2006.enclosure.utils.hoverText
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.*
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos

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
            } catch (e: Exception) {
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
                    ServerMain.getSmallestEnclosure(source.world, blockPos)
                        ?.let { action(it) }
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
                    ServerMain.getSmallestEnclosure(it.source.world, blockPos)
                        ?.let { area -> action(it, area, t) }
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
                    OP -> source.hasPermissionLevel(
                        if (minecraftServer.isSingleplayer) 2 else 4 // 2 for LAN, 4 for normal
                    )
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
