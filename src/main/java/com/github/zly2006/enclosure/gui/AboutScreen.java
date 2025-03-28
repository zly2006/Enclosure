package com.github.zly2006.enclosure.gui;

import com.github.zly2006.enclosure.utils.CosTransitionValue;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.util.ArrayList;
import java.util.List;

public class AboutScreen extends Screen {
    final Screen parent;
    final List<ClickableTextWidget> textWidgets = new ArrayList<>();
    public static final String WIKI_ZH = "https://enclosure.fandom.com/zh/wiki/Enclosure_Wiki";
    public static final String WIKI_EN = "https://enclosure.fandom.com";

    public static final Identifier MOD_ICON_TEXTURE = Identifier.of("enclosure", "icon.png");
    public static final int MOD_ICON_TEXTURE_WIDTH = 80;
    public static final int MOD_ICON_TEXTURE_HEIGHT = 80;

    private boolean isHovering = false;
    private final CosTransitionValue cosTransitionValue = new CosTransitionValue(500);

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
        textWidgets.add(new ClickableTextWidget(client, parent, Text.literal("点击查看中文wiki页面").formatted(Formatting.UNDERLINE), Text.translatable("enclosure.about.click_to_open"),
            button -> ConfirmLinkScreen.open(this, WIKI_ZH), 5, 5, width - 20));
        textWidgets.add(new ClickableTextWidget(client, parent, Text.literal("Click to open English wiki page").formatted(Formatting.UNDERLINE), Text.translatable("enclosure.about.click_to_open"),
            button -> ConfirmLinkScreen.open(this, WIKI_EN), 5, 5, width - 20));

        addDrawableChild(ButtonWidget.builder(ScreenTexts.BACK, button -> client.setScreen(parent))
                .width(200)
                .position((width - 200) / 2, height - 25)
                .build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int renderStart;
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        try {
            int renderIconX = (width - MOD_ICON_TEXTURE_WIDTH) / 2;
            int renderIconY = 5;

            boolean mouseHoverIcon = mouseX > renderIconX && mouseX < renderIconX + MOD_ICON_TEXTURE_WIDTH && mouseY > renderIconY && mouseY < renderIconY + MOD_ICON_TEXTURE_HEIGHT;


            if (mouseHoverIcon != isHovering) {
                isHovering = mouseHoverIcon;
                cosTransitionValue.setToValue(360 * (isHovering ? 1 : 0));
            }

            matrices.translate(renderIconX + MOD_ICON_TEXTURE_WIDTH / 2D, renderIconY + MOD_ICON_TEXTURE_HEIGHT / 2D, 0);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(cosTransitionValue.calcCurrent()));

            context.drawTexture(MOD_ICON_TEXTURE, -MOD_ICON_TEXTURE_WIDTH / 2, -MOD_ICON_TEXTURE_HEIGHT / 2, MOD_ICON_TEXTURE_WIDTH, MOD_ICON_TEXTURE_HEIGHT, 0, 0, 640, 640, 640, 640);

            renderStart = renderIconY + MOD_ICON_TEXTURE_HEIGHT + 20;
        } finally {
            matrices.pop();
        }

        for (ClickableTextWidget textWidget : textWidgets) {
            textWidget.x = 10;
            textWidget.y = renderStart;
            textWidget.render(context, mouseX, mouseY, delta);
            renderStart += textWidget.getHeight() + 5;
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
