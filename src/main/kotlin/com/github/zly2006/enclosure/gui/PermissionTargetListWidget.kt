package com.github.zly2006.enclosure.gui

import com.github.zly2006.enclosure.ReadOnlyEnclosureArea
import com.github.zly2006.enclosure.command.CONSOLE
import com.github.zly2006.enclosure.network.UUIDCacheS2CPacket
import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.PlayerSkinDrawer
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.ElementListWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text
import java.util.*

class PermissionTargetListWidget(
    minecraftClient: MinecraftClient?,
    val area: ReadOnlyEnclosureArea,
    val fullName: String,
    val parent: Screen,
    width: Int,
    height: Int,
    top: Int,
    bottom: Int
) : ElementListWidget<PermissionTargetListWidget.Entry>(minecraftClient, width, height, top, bottom, 20) {
    enum class Mode {
        Players,
        Unspecified
    }

    private var mode = Mode.Players
    private val searchEntry: SearchEntry = SearchEntry()

    init {
        setRenderBackground(false) // 不渲染背景
    }

    override fun getRowWidth(): Int {
        return width - 60
    }

    override fun getScrollbarPositionX(): Int {
        return width - 15
    }

    var top: Int = 0

    fun showPlayers() {
        clearEntries()
        mode = Mode.Players
        scrollAmount = 0.0
        addEntry(searchEntry)
        area.permissionsMap.keys
            .filter { it != CONSOLE }
            .map { PlayerEntry(Text.of(UUIDCacheS2CPacket.getName(it)), it) }
            .sortedBy { it.name.string }
            .forEach { entry -> addEntry(entry) }
    }

    fun showUnlistedPlayers() {
        clearEntries()
        mode = Mode.Unspecified
        scrollAmount = 0.0
        addEntry(searchEntry)
        UUIDCacheS2CPacket.uuid2name.keys
            .filter { it != CONSOLE }
            .filter { !area.permissionsMap.containsKey(it) }
            .map { PlayerEntry(Text.of(UUIDCacheS2CPacket.getName(it)), it) }
            .sortedBy { it.name.string }
            .forEach { entry -> addEntry(entry) }
    }

    abstract class Entry : ElementListWidget.Entry<Entry>()
    internal inner class PlayerEntry(val name: Text, val uuid: UUID) : Entry() {
        private val setButton = ButtonWidget.builder(Text.translatable("enclosure.widget.set")) {
            client.setScreen(screenCache)
        }.size(40, 20).build()
        private val screenCache: PermissionScreen by lazy {
            PermissionScreen(area, uuid, fullName, parent)
        }

        override fun selectableChildren(): List<Selectable?> {
            return listOf(setButton)
        }

        override fun children(): List<Element?> {
            return listOf(setButton)
        }

        override fun render(
            matrices: MatrixStack,
            index: Int,
            y: Int,
            x: Int,
            entryWidth: Int,
            entryHeight: Int,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            tickDelta: Float
        ) {
            client.textRenderer.draw(matrices, name, (x + 20).toFloat(), (y + 3).toFloat(), 0xffffff)
            setButton.x = x + entryWidth - 40
            setButton.y = y
            setButton.render(matrices, mouseX, mouseY, tickDelta)
            client.player!!.networkHandler.getPlayerListEntry(uuid)?.skinTexture?.let {
                RenderSystem.setShaderTexture(0, it)
                PlayerSkinDrawer.draw(matrices, x, y, 16)
            }
        }
    }

    @Environment(EnvType.CLIENT)
    inner class SearchEntry : Entry() {
        private val searchWidget = TextFieldWidget(client.textRenderer, 0, 0, 100, 16, Text.of("search"))

        init {
            searchWidget.setChangedListener { s: String? ->
                clearEntries()
                addEntry(this)
                when (mode) {
                    Mode.Players -> area.permissionsMap.keys
                        .filter { it != CONSOLE }
                        .map { uuid ->
                            PlayerEntry(Text.literal(UUIDCacheS2CPacket.getName(uuid)), uuid)
                        }

                    Mode.Unspecified -> UUIDCacheS2CPacket.uuid2name.keys
                        .map { uuid ->
                            PlayerEntry(Text.literal(UUIDCacheS2CPacket.getName(uuid)), uuid)
                        }
                }.filter { it.name.string.contains(s!!) }
                    .sortedBy { it.name.string }
                    .forEach { entry -> addEntry(entry) }
            }
        }

        override fun render(
            matrices: MatrixStack,
            index: Int,
            y: Int,
            x: Int,
            entryWidth: Int,
            entryHeight: Int,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            tickDelta: Float
        ) {
            searchWidget.y = y
            searchWidget.x = x + 70
            searchWidget.width = entryWidth - 70 - 2
            searchWidget.render(matrices, mouseX, mouseY, tickDelta)
            client.textRenderer.draw(
                matrices,
                Text.translatable("enclosure.widget.search"),
                x.toFloat(),
                (y + 3).toFloat(),
                0xFFFFFF
            )
        }

        override fun selectableChildren(): List<Selectable?> {
            return listOf(searchWidget)
        }

        override fun children(): List<Element?> {
            return listOf(searchWidget)
        }
    }
}
