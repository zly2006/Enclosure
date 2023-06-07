package com.github.zly2006.enclosure.gui

import com.github.zly2006.enclosure.EnclosureView
import com.github.zly2006.enclosure.network.UUIDCacheS2CPacket
import com.github.zly2006.enclosure.utils.formatSelection
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.text.SimpleDateFormat
import java.util.*
import java.util.function.Consumer

private class SetButton(uuid: UUID, widget: PermissionTargetListWidget<SetButton>): ButtonWidget(0, 0, 40, 20, Text.translatable("enclosure.widget.set"),null, { it.get() }) {
    override fun onPress() {
        MinecraftClient.getInstance().setScreen(screenCache)
    }

    private val screenCache: PermissionScreen by lazy {
        PermissionScreen(widget.area, uuid, widget.fullName, widget.parent)
    }
}

class EnclosureScreen(handler: EnclosureScreenHandler, inventory: PlayerInventory?, title: Text?) :
    HandledScreen<EnclosureScreenHandler>(handler, inventory, title), EnclosureGui {
    val area: EnclosureView.ReadOnly = handler.area
    private var permissionTargetListWidget = PermissionTargetListWidget(client, area, handler.fullName, this, width, height, 60, height) { a, b -> SetButton(a, b) }
    private var globalWidget: ButtonWidget = ButtonWidget.builder(Text.translatable("enclosure.widget.global")) {
        assert(client != null)
        client!!.setScreen(PermissionScreen(area, UUID(0, 0), handler.fullName, this))
    }.size(100, 20).position(5, 35).build()
    private var playerWidget: ButtonWidget = ButtonWidget.builder(Text.translatable("enclosure.widget.player")) { button: ButtonWidget ->
        assert(client != null)
        button.active = false
        unlistedWidget.active = true
        permissionTargetListWidget.showPlayers()
    }.size(100, 20).position(110, 35).build()
    private var unlistedWidget: ButtonWidget = ButtonWidget.builder(Text.translatable("enclosure.widget.unspecified_player")) { button: ButtonWidget ->
        assert(client != null)
        button.active = false
        playerWidget.active = true
        permissionTargetListWidget.showUnlistedPlayers()
    }.size(100, 20).position(215, 35).build()
    private var aboutWidget: ButtonWidget = ButtonWidget.builder(Text.translatable("enclosure.widget.about")) {
        assert(client != null)
        client!!.setScreen(AboutScreen(this))
    }.size(50, 20).position(320, 35).build()
    private val transferButton: ButtonWidget = ButtonWidget.builder(Text.translatable("enclosure.widget.transfer")) {
        assert(client != null)
        client!!.setScreen(TransferScreen(area, handler.fullName, this))
    }.size(100, 20).position(5, 5).build()
    private val textWidgets: MutableList<ClickableTextWidget> = ArrayList()
    private val subLandWidgets: MutableList<ClickableTextWidget> = ArrayList()
    private var renderBottom = 5

    override fun init() {
        super.init()
        textWidgets.clear()
        subLandWidgets.clear()
        addDrawableChild(permissionTargetListWidget)
        addDrawableChild(globalWidget)
        addDrawableChild(playerWidget)
        addDrawableChild(unlistedWidget)
        addDrawableChild(aboutWidget)
        val owner = UUIDCacheS2CPacket.getName(area.owner)
        assert(client != null)
        if (handler!!.fatherFullName.isNotEmpty()) {
            textWidgets.add(ClickableTextWidget(
                    client!!, this, Text.literal("<<< ")
                    .styled { style: Style -> style.withColor(Formatting.DARK_GREEN) }
                    .append(Text.literal(handler!!.fatherFullName).formatted(Formatting.GOLD)),
                    Text.translatable("enclosure.widget.father_land.hover"),
                    {
                        assert(client!!.player != null)
                        close()
                        client!!.player!!.networkHandler.sendChatCommand("enclosure gui " + handler!!.fatherFullName)
                    }, 5, 5, width - 10
            )
            )
        }
        textWidgets.add(ClickableTextWidget(
                client!!, this, Text.empty()
                .append(Text.literal(area.fullName).styled { style: Style -> style.withColor(Formatting.GOLD) })
                .append(" ")
                .append(Text.translatable("enclosure.info.created_by"))
                .append(" ")
                .append(
                        if (owner == null) Text.translatable("enclosure.message.unknown_user").formatted(Formatting.RED)
                        else Text.literal(owner).styled { style: Style -> style.withColor(Formatting.GOLD) }
                )
                .append(", ")
                .append(Text.translatable("enclosure.info.created_on"))
                .append(Text.literal(SimpleDateFormat().format(area.createdOn)).styled { style: Style ->
                    style.withColor(
                            Formatting.GOLD
                    )
                }),
                null, null,
                5, 5, width - 10
        )
        )
        textWidgets.add(
                ClickableTextWidget(
                        client!!, this,
                        formatSelection(handler!!.worldId, area.minX, area.minY, area.minZ, area.maxX, area.maxY, area.maxZ),
                        Text.translatable("enclosure.widget.selection_render.hover"),
                        {
                            assert(client!!.player != null)
                            client!!.player!!.networkHandler.sendChatCommand("enclosure select land " + handler!!.fullName)
                            close()
                        }, 5, 20, width - 10
                )
        )
        for (name in handler!!.subAreaNames) {
            subLandWidgets.add(ClickableTextWidget(
                    client!!, this, Text.literal(">>> ")
                    .styled { style: Style -> style.withColor(Formatting.DARK_GREEN) }
                    .append(Text.literal(name).formatted(Formatting.GOLD)),
                    Text.translatable("enclosure.widget.sub_land.hover"),
                    {
                        assert(client!!.player != null)
                        close()
                        client!!.player!!.networkHandler.sendChatCommand("enclosure gui ${handler!!.fullName}.$name")
                    }, 5, 5, 0
            )
            )
        }
    }

    override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(matrices)
        renderBottom = 5
        for (textWidget in textWidgets) {
            textWidget.y = renderBottom
            renderBottom += textWidget.calcHeight()
        }
        for (textWidget in subLandWidgets) {
            textWidget.y = renderBottom
        }
        renderBottom += if (MinecraftClient.getInstance().player?.uuid == area.owner) {
            transferButton.y = renderBottom
            transferButton.render(matrices, mouseX, mouseY, delta)
            25
        } else 0
        renderBottom += if (subLandWidgets.isNotEmpty()) {
            var subLandsX = 5
            for (textWidget in subLandWidgets) {
                textWidget.x = subLandsX
                textWidget.render(matrices, mouseX, mouseY, delta)
                subLandsX += textWidget.width + 5
            }
            10
        } else 0
        globalWidget.y = renderBottom
        playerWidget.y = renderBottom
        unlistedWidget.y = renderBottom
        aboutWidget.y = renderBottom
        permissionTargetListWidget.top = renderBottom + 25
        super.render(matrices, mouseX, mouseY, delta)
        for (textWidget in textWidgets) {
            textWidget.render(matrices, mouseX, mouseY, delta)
        }
    }

    override fun children(): List<Element> {
        val children: MutableList<Element> = ArrayList(super.children())
        children.addAll(textWidgets)
        children.addAll(subLandWidgets)
        return children
    }

    override fun resize(client: MinecraftClient, width: Int, height: Int) {
        textWidgets.forEach(Consumer { textWidget: ClickableTextWidget -> textWidget.width = width })
        super.resize(client, width, height)
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

    override fun drawForeground(matrices: MatrixStack, mouseX: Int, mouseY: Int) {}
    override fun drawBackground(matrices: MatrixStack, delta: Float, mouseX: Int, mouseY: Int) {}
}
