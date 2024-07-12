package com.github.zly2006.enclosure.command

import com.github.zly2006.enclosure.EnclosureArea
import com.github.zly2006.enclosure.ServerMain
import com.github.zly2006.enclosure.utils.Permission
import com.github.zly2006.enclosure.utils.TrT
import com.github.zly2006.enclosure.utils.hoverText
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.argument.TextArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.ClickEvent
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty

fun BuilderScope<*>.registerMessages(access: CommandRegistryAccess) {
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
                n.then(
                    CommandManager.argument("message", StringArgumentType.greedyString())
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
                n.then(CommandManager.argument("message", TextArgumentType.text(access)).executes(c))
            }) { area, l ->
                if (!area.hasPerm(source.player!!, Permission.ADMIN)) {
                    error(Permission.ADMIN.getNoPermissionMsg(source.player), this)
                }
                var str by delegate(area, l)
                val message = Text.Serialization.toJsonString(TextArgumentType.getTextArgument(this, "message"), area.world.registryManager)
                str = "#rich:$message"
                source.sendMessage(TrT.of("enclosure.message.set_message", l))
            }
        }
    }
}
