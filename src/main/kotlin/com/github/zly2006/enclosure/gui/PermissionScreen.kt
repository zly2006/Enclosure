package com.github.zly2006.enclosure.gui

import com.github.zly2006.enclosure.ReadOnlyEnclosureArea
import com.github.zly2006.enclosure.command.CONSOLE
import com.github.zly2006.enclosure.network.UUIDCacheS2CPacket
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.nbt.NbtCompound
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.util.*

class PermissionScreen(
    val area: ReadOnlyEnclosureArea,
    val uuid: UUID,
    val fullName: String,
    private val parent: Screen
): Screen(Text.of("Set permission")) {
    private var permissionWidgetList: PermissionListWidget? = null
    override fun init() {
        super.init()
        permissionWidgetList = PermissionListWidget(
            client, this,
            fullName, area, uuid, width, height, 20, height
        )
        addDrawableChild(permissionWidgetList)
        focused = permissionWidgetList
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (keyCode == 256 && shouldCloseOnEsc()) {
            close()
            return true
        }
        return if (focused == null) {
            false
        } else focused!!.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun close() {
        super.close()
        assert(client != null)
        client!!.setScreen(parent)
    }

    override fun render(drawContext: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(drawContext)
        super.render(drawContext, mouseX, mouseY, delta)
        val title = Text.translatable("enclosure.widget.set_permission").append(" ")
        if (CONSOLE == uuid) {
            title.append(Text.translatable("enclosure.widget.global"))
        } else {
            title.append(Text.translatable("enclosure.widget.player"))
                .append(" ")
                .append(UUIDCacheS2CPacket.getName(uuid))
        }
        title.append(" ")
            .append(Text.translatable("enclosure.widget.in_enclosure"))
            .append(" ")
            .append(fullName)
        drawContext.drawText(textRenderer, title, 10, 10, 0xffffff, true)
    }

    fun requestConfirm(readString: Text?) {
        assert(client != null)
        client!!.execute {
            client!!.setScreen(ConfirmScreen(this, readString!!) {
                assert(client!!.player != null)
                client!!.player!!.networkHandler.sendCommand("enclosure confirm")
            })
        }
    }

    fun syncPermission(permission: NbtCompound) {
        val perms: MutableMap<String, Boolean> = HashMap()
        for (key in permission.keys) {
            perms[key] = permission.getBoolean(key)
        }
        area.permissionsMap[uuid] = perms
    }

    companion object {
        val PERMISSION_SCREEN_ID = Identifier("enclosure", "screen.permission")
    }
}
