package com.github.zly2006.enclosure.network

import net.minecraft.util.Identifier

object NetworkChannels {
    val ENCLOSURE_INSTALLED: Identifier = Identifier.of("enclosure", "packet.installed")
    val OPEN_REQUEST: Identifier = Identifier.of("enclosure", "packet.request_open_screen")
    val SYNC_SELECTION: Identifier = Identifier.of("enclosure", "packet.sync_selection")
    val SYNC_UUID: Identifier = Identifier.of("enclosure", "packet.uuid")
    val CONFIRM: Identifier = Identifier.of("enclosure", "packet.confirm")
    val SYNC_PERMISSION: Identifier = Identifier.of("enclosure", "packet.sync_permission")
}
