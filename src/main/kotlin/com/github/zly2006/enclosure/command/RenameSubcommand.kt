package com.github.zly2006.enclosure.command

import com.github.zly2006.enclosure.Enclosure
import com.github.zly2006.enclosure.ServerMain
import com.github.zly2006.enclosure.utils.TrT
import com.mojang.brigadier.arguments.StringArgumentType

fun BuilderScope<*>.registerRename() {
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
}
