package com.github.zly2006.enclosure.command

import com.github.zly2006.enclosure.Enclosure
import com.github.zly2006.enclosure.LOGGER
import com.github.zly2006.enclosure.ServerMain
import com.github.zly2006.enclosure.utils.TrT

fun BuilderScope<*>.registerRemove() {
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
}
