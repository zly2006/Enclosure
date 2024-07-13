package com.github.zly2006.enclosure.command

import com.github.zly2006.enclosure.MOD_VERSION
import com.github.zly2006.enclosure.network.config.EnclosureInstalledC2SPacket
import com.github.zly2006.enclosure.utils.Permission
import com.github.zly2006.enclosure.utils.Serializable2Text.SerializationSettings
import com.github.zly2006.enclosure.utils.TrT
import com.github.zly2006.enclosure.utils.gold
import net.minecraft.text.ClickEvent
import net.minecraft.text.Text

fun BuilderScope<*>.registerAbout() {
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
}
