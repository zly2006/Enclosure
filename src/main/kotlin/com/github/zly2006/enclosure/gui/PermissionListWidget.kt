package com.github.zly2006.enclosure.gui

import com.github.zly2006.enclosure.ReadOnlyEnclosureArea
import com.github.zly2006.enclosure.command.CONSOLE
import com.github.zly2006.enclosure.utils.Permission
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.gui.widget.ElementListWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.item.ItemStack
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.text.TranslatableTextContent
import net.minecraft.util.Formatting
import net.minecraft.util.Language
import org.lwjgl.glfw.GLFW
import java.util.*

class PermissionListWidget(
    minecraftClient: MinecraftClient?,
    private val parent: Screen,
    private val fullName: String,
    private val area: ReadOnlyEnclosureArea,
    private val uuid: UUID,
    width: Int,
    height: Int,
    top: Int,
    bottom: Int
) : ElementListWidget<PermissionListWidget.Entry?>(minecraftClient, width, height, top, bottom, 20) {
    private val target: Permission.Target = if (uuid == CONSOLE) Permission.Target.Enclosure else Permission.Target.Player

    init {
        addEntry(SearchEntry())
        Permission.PERMISSIONS.values
            .filter { it.target.fitPlayer() && target.fitPlayer() || it.target.fitEnclosure() && target.fitEnclosure() }
            .sortedBy { it.name }
            .forEach { addEntry(PermissionEntry(it)) }
        setRenderBackground(false) // 不渲染背景
        setRenderHorizontalShadows(true) // 渲染上下两道杠
    }

    override fun getRowWidth(): Int {
        return width - 60
    }

    override fun getScrollbarPositionX(): Int {
        return width - 15
    }

    @Environment(EnvType.CLIENT)
    abstract class Entry : ElementListWidget.Entry<Entry?>()

    @Environment(EnvType.CLIENT)
    inner class PermissionEntry(val permission: Permission) : Entry() {
        val buttonWidget = SetButtonWidget(0, 0, 40, 20, value()) { }
        private fun value(value: Boolean? = this.value): Text {
            return if (value == null) Text.translatable("enclosure.widget.none").setStyle(
                Style.EMPTY.withColor(
                    Formatting.DARK_AQUA
                )
            ) else if (value) Text.translatable("enclosure.widget.true").setStyle(
                Style.EMPTY.withColor(
                    Formatting.GREEN
                )
            ) else Text.translatable("enclosure.widget.false").setStyle(
                Style.EMPTY.withColor(
                    Formatting.RED
                )
            )
        }

        private var value: Boolean?
            get() = permission.getValue(area.permissionsMap.getOrDefault(uuid, emptyMap()))
            set(value) {
                val perm = area.permissionsMap.getOrDefault(uuid, HashMap())
                permission.setValue(perm, value)
                area.permissionsMap[uuid] = perm
            }

        override fun render(
            drawContext: DrawContext,
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
            buttonWidget.y = y
            buttonWidget.x = x + entryWidth - 40
            buttonWidget.message = value()
            buttonWidget.render(drawContext, mouseX, mouseY, tickDelta)
            drawContext.drawText(client.textRenderer, permission.name, x + 20, y + 3, 0xFFFFFF, false)
            drawContext.drawText(client.textRenderer, permission.description, x + 140, y + 3, 0x999999, false)
            drawContext.drawItem(ItemStack(permission.icon), x, y)
            if (buttonWidget.isHovered) {
                drawContext.drawTooltip(client.textRenderer, listOf<Text>(
                    Text.translatable("enclosure.widget.click.left")
                        .formatted(Formatting.GREEN),
                    Text.translatable("enclosure.widget.click.right")
                        .formatted(Formatting.RED)
                ), mouseX, mouseY)
            } else if (hovered) {
                drawContext.drawTooltip(
                    client.textRenderer,
                    listOf(
                        permission.description,
                        Text.translatable("enclosure.widget.default_value_is")
                            .setStyle(Style.EMPTY.withColor(Formatting.GOLD))
                            .append(" ").append(value(permission.defaultValue))
                    ),
                    mouseX, mouseY
                )
            }
        }

        override fun selectableChildren() = listOf(buttonWidget)
        override fun children() = listOf(buttonWidget)

        inner class SetButtonWidget(
            x: Int,
            y: Int,
            width: Int,
            height: Int,
            message: Text?,
            onPress: PressAction?
        ) : ButtonWidget(x, y, width, height, message, onPress, { it.get() }) {
            override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
                assert(client.player != null)
                if (!visible || !active) {
                    return false
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    value = if (value == null) true else null
                    client.player!!.networkHandler.sendChatCommand(
                        "enclosure set " + fullName + " uuid " +
                                uuid.toString() + " " +
                                permission.name + " " +
                                (value?.toString() ?: "none")
                    )
                    buttonWidget.message = value()
                    return true
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    value = if (value == null) false else null
                    client.player!!.networkHandler.sendChatCommand("enclosure set " + fullName + " uuid " +
                            uuid.toString() + " " +
                            permission.name + " " +
                            (value?.toString() ?: "none")
                    )
                    buttonWidget.message = value()
                    return true
                }
                return super.mouseClicked(mouseX, mouseY, button)
            }
        }
    }

    @Environment(EnvType.CLIENT)
    inner class SearchEntry : Entry() {
        val searchWidget: TextFieldWidget

        init {
            searchWidget = TextFieldWidget(client.textRenderer, 0, 0, 100, 16, Text.of("search"))
            searchWidget.setChangedListener { s: String? ->
                clearEntries()
                addEntry(this)
                Permission.PERMISSIONS.values
                    .filter { it.target.fitPlayer() && target.fitPlayer() || it.target.fitEnclosure() && target.fitEnclosure() }
                    .filter { permission ->
                        if (permission.name.contains(s!!)) return@filter true
                        val content = permission.description.content
                        if (content is TranslatableTextContent) {
                            return@filter Language.getInstance().hasTranslation(content.key) &&
                                    Language.getInstance()[content.key].contains(s)
                        }
                        false
                    }
                    .sortedBy { it.name }
                    .forEach { addEntry(PermissionEntry(it)) }
            }
        }

        override fun render(
            drawContext: DrawContext,
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
            searchWidget.render(drawContext, mouseX, mouseY, tickDelta)
            drawContext.drawText(client.textRenderer, Text.translatable("enclosure.widget.search"), x, y + 3, 0xFFFFFF, false)
        }

        override fun selectableChildren(): List<Selectable?> {
            return listOf(searchWidget)
        }

        override fun children(): List<Element?> {
            return listOf(searchWidget)
        }
    }
}
