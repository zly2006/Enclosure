package com.github.zly2006.enclosure.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class AboutScreen extends Screen {
    final Screen parent;
    final List<ClickableTextWidget> textWidgets = new ArrayList<>();
    public static final String WIKI_ZH = "https://enclosure.fandom.com/zh/wiki/Enclosure_Wiki";
    public static final String WIKI_EN = "https://enclosure.fandom.com";

    public AboutScreen(Screen parent) {
        super(Text.of("About"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        textWidgets.clear();
        super.init();

        assert client != null;

        textWidgets.add(new ClickableTextWidget(client, parent, Text.translatable("enclosure.about.author"), null, button -> {
        }, 5, 5, width - 20));
        textWidgets.add(new ClickableTextWidget(client, parent, Text.translatable("enclosure.about.source").formatted(Formatting.UNDERLINE), Text.translatable("enclosure.about.click_to_open"),
            button -> ConfirmLinkScreen.open(this, "https://github.com/zly2006/Enclosure"), 5, 5, width - 20));
        textWidgets.add(new ClickableTextWidget(client, parent, Text.translatable("enclosure.about.team_page").formatted(Formatting.UNDERLINE), Text.translatable("enclosure.about.click_to_open"),
            button -> ConfirmLinkScreen.open(this, "https://www.starlight.cool/"), 5, 5, width - 20));
        textWidgets.add(new ClickableTextWidget(client, parent, Text.translatable("enclosure.about.copyright"), null,
            button -> {}, 5, 5, width - 20));
        textWidgets.add(new ClickableTextWidget(client, parent, Text.translatable("点击查看中文wiki页面").formatted(Formatting.UNDERLINE), Text.translatable("enclosure.about.click_to_open"),
            button -> ConfirmLinkScreen.open(this, WIKI_ZH), 5, 5, width - 20));
        textWidgets.add(new ClickableTextWidget(client, parent, Text.translatable("Click to open English wiki page").formatted(Formatting.UNDERLINE), Text.translatable("enclosure.about.click_to_open"),
            button -> ConfirmLinkScreen.open(this, WIKI_EN), 5, 5, width - 20));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int centerHeight = height / 2;
        int centerWidth = width / 2;
        int renderStart = Math.max(50, centerHeight - 80);
        context.drawCenteredTextWithShadow(textRenderer, Text.of("Enclosure"), centerWidth, renderStart, 0xffffff);
        renderStart += 10;
        renderInGameBackground(context);
        for (ClickableTextWidget textWidget : textWidgets) {
            textWidget.x = 10;
            textWidget.y = renderStart;
            textWidget.render(context, mouseX, mouseY, delta);
            renderStart += textWidget.getHeight() + 10;
        }
    }

    @Override
    public List<? extends Element> children() {
        List<Element> children = new ArrayList<>(super.children());
        children.addAll(textWidgets);
        return children;
    }

    @Override
    public void close() {
        assert client != null;
        client.setScreen(parent);
    }
}
