package com.github.zly2006.enclosure.gui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.screen.ConfirmChatLinkScreen
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text
import net.minecraft.text.LiteralText
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting

class AboutScreen(private val parent: Screen) : Screen(Text.of("About")) {
    private val textWidgets: MutableList<ClickableTextWidget> = ArrayList()
    override fun init() {
        textWidgets.clear()
        super.init()
        val client = MinecraftClient.getInstance()
        textWidgets.add(
            ClickableTextWidget(
                client,
                parent,
                TranslatableText("enclosure.about.author"),
                null,
                { },
                5,
                5,
                width - 20
            )
        )
        textWidgets.add(
            ClickableTextWidget(
                client,
                parent,
                TranslatableText("enclosure.about.translator"),
                null,
                { },
                5,
                5,
                width - 20
            )
        )
        textWidgets.add(
            ClickableTextWidget(
                client, parent, TranslatableText("enclosure.about.team_page").formatted(
                    Formatting.UNDERLINE
                ), TranslatableText("enclosure.about.click_to_open"),
                { ConfirmLinkScreen.open("https://www.starlight.cool/", this, true) }, 5, 5, width - 20
            )
        )
        textWidgets.add(
            ClickableTextWidget(
                client,
                parent,
                TranslatableText("enclosure.about.copyright"),
                null,
                { },
                5,
                5,
                width - 20
            )
        )
        textWidgets.add(
            ClickableTextWidget(
                client,
                parent,
                LiteralText("Get source code at Github").formatted(
                    Formatting.UNDERLINE
                ),
                TranslatableText("enclosure.about.click_to_open"),
                { ConfirmLinkScreen.open("https://github.com/zly2006/Enclosure/", this, true) },
                5,
                5,
                width - 20
            )
        )
        textWidgets.add(
            ClickableTextWidget(
                client, parent, TranslatableText("点击查看中文wiki页面").formatted(
                    Formatting.UNDERLINE
                ), TranslatableText("enclosure.about.click_to_open"),
                { ConfirmLinkScreen.open(WIKI_ZH, this, true) }, 5, 5, width - 20
            )
        )
        textWidgets.add(
            ClickableTextWidget(
                client,
                parent,
                TranslatableText("Click to open English wiki page").formatted(
                    Formatting.UNDERLINE
                ),
                TranslatableText("enclosure.about.click_to_open"),
                { ConfirmLinkScreen.open(WIKI_EN, this, true) },
                5,
                5,
                width - 20
            )
        )
    }

    override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
        val centerHeight = height / 2
        val centerWidth = width / 2
        var renderStart = (centerHeight - 80).coerceAtLeast(50)
        drawTextAtCenter(matrices, Text.of("About Enclosure"), centerWidth, 10)
        renderStart += 10
        renderBackgroundTexture(0)
        for (textWidget in textWidgets) {
            textWidget.x = 10
            textWidget.y = renderStart
            textWidget.render(matrices, mouseX, mouseY, delta)
            renderStart += textWidget.height + 10
        }
    }

    override fun children(): List<Element> {
        val children: MutableList<Element> = ArrayList(super.children())
        children.addAll(textWidgets)
        return children
    }

    override fun close() {
        assert(client != null)
        client!!.setScreen(parent)
    }

    private fun drawTextAtCenter(
        matrices: MatrixStack,
        text: Text,
        center: Int,
        @Suppress("SameParameterValue") y: Int
    ) {
        // draw text at the center
        textRenderer.draw(matrices, text, center - textRenderer.getWidth(text) / 2.0f, y.toFloat(), 0xffffff)
    }

    companion object {
        const val WIKI_ZH = "https://enclosure.fandom.com/zh/wiki/Enclosure_Wiki"
        const val WIKI_EN = "https://enclosure.fandom.com"
    }


    private object ConfirmLinkScreen {
        fun open(url: String?, aboutScreen: AboutScreen?, b: Boolean) {
            MinecraftClient.getInstance().setScreen(ConfirmChatLinkScreen({
                MinecraftClient.getInstance().setScreen(aboutScreen)
            }, url, b))
        }
    }
}
