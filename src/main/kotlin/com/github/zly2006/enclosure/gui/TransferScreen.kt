package com.github.zly2006.enclosure.gui

import com.github.zly2006.enclosure.EnclosureView
import com.github.zly2006.enclosure.network.UUIDCacheS2CPacket
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text

class TransferScreen(
        area: EnclosureView.ReadOnly,
        fullName: String,
        parent: Screen
): Screen(Text.literal("Transfer")), EnclosureGui {
    private val data = area.clone().apply {
        permissionsMap = UUIDCacheS2CPacket.uuid2name.keys.associateWith { mutableMapOf<String, Boolean>() }.toMutableMap()
    }
    private val permissionTargetListWidget = PermissionTargetListWidget<ButtonWidget>(client, data, fullName, parent, width, height, 35, height - 35) { uuid, _ ->
        ButtonWidget.builder(Text.translatable("enclosure.widget.transfer")) {
            client!!.execute {
                client!!.setScreen(ConfirmScreen(this, Text.translatable("enclosure.widget.transfer_confirm")) {
                    assert(client!!.player != null)
                    client!!.player!!.networkHandler.sendCommand("enclosure give $fullName $uuid")
                })
            }
        }.size(40, 20).build()
    }

    init {
        addDrawableChild(permissionTargetListWidget)
        permissionTargetListWidget.showPlayers()
    }
}
