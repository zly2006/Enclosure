package com.github.zly2006.enclosure.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

public class ClickableTextWidget implements Element, Drawable, Selectable {
    boolean hovered;
    boolean selected;

    @Override
    public SelectionType getType() {
        if (hovered) {
            return SelectionType.HOVERED;
        } else if (selected) {
            return SelectionType.FOCUSED;
        } else {
            return SelectionType.NONE;
        }
    }

    @Override
    public void appendNarrations(NarrationMessageBuilder builder) {
    }

    private final Screen parent;
    final Text text;
    final Text hover;
    final Consumer<Integer> onClick;
    int x;
    int y;
    int width;
    int renderedWidth;
    private int height;
    final TextRenderer textRenderer;
    public int getHeight() {
        return height;
    }
    public int calcHeight() {
        return textRenderer.wrapLines(text, width).size() * (textRenderer.fontHeight + 1);
    }
    public ClickableTextWidget(MinecraftClient client, Screen parent, Text text, Text hover, Consumer<Integer> onClick, int x, int y, int width) {
        textRenderer = client.textRenderer;
        this.parent = parent;
        this.text = text;
        this.hover = hover;
        this.onClick = onClick;
        this.x = x;
        this.y = y;
        this.width = width;
        height = 0;
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= x && mouseX <= x + renderedWidth) {
            if (mouseY >= y && mouseY <= y + height) {
                selected = true;
                if (onClick != null) {
                    onClick.accept(button);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
        selected = focused;
    }

    @Override
    public boolean isFocused() {
        return selected;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (mouseX >= x && mouseX <= x + renderedWidth) {
            if (mouseY >= y && mouseY <= y + height) {
                hovered = true;
                if (hover != null) {
                    parent.renderOrderedTooltip(matrices, textRenderer.wrapLines(hover, Math.max(200, parent.width / 2)), mouseX, mouseY);
                }
            }
        }
        height = 0;
        if (width == 0) {
            width = Math.min(textRenderer.getWidth(text), parent.width);
        }
        List<OrderedText> orderedTexts = textRenderer.wrapLines(text, width);
        if (orderedTexts.size() == 1) {
            renderedWidth = textRenderer.getWidth(orderedTexts.get(0));
        } else {
            renderedWidth = width;
        }
        for (OrderedText orderedText : orderedTexts) {
            textRenderer.draw(matrices, orderedText, x, y + height, 0xffffff);
            height += textRenderer.fontHeight + 1;
        }
    }
}
