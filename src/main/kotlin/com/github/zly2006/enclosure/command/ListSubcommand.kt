package com.github.zly2006.enclosure.command

import com.github.zly2006.enclosure.ServerMain
import com.github.zly2006.enclosure.utils.Serializable2Text.SerializationSettings
import com.github.zly2006.enclosure.utils.TrT
import com.github.zly2006.enclosure.utils.Utils
import net.minecraft.command.argument.DimensionArgumentType
import net.minecraft.util.Identifier

fun BuilderScope<*>.registerList() {
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
                    list.forEach {
                        ret.append("\n").append(
                            it.serialize(
                                SerializationSettings.Summarize,
                                source.player
                            )
                        )
                    }
                    source.sendMessage(ret)
                }
            }
        }
    }
}
