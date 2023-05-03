package com.github.zly2006.enclosure.gui;

import com.github.zly2006.enclosure.ReadOnlyEnclosureArea;
import com.github.zly2006.enclosure.network.UUIDCacheS2CPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EnclosureScreen extends HandledScreen<EnclosureScreenHandler> {
    final ReadOnlyEnclosureArea area;
    PermissionTargetListWidget permissionTargetListWidget;
    ButtonWidget globalWidget;
    ButtonWidget playerWidget;
    ButtonWidget unlistedWidget;
    ButtonWidget aboutWidget;
    final List<ClickableTextWidget> textWidgets = new ArrayList<>();
    final List<ClickableTextWidget> subLandWidgets = new ArrayList<>();
    int renderBottom = 5;

    public EnclosureScreen(EnclosureScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.area = handler.area;
    }

    @Override
    protected void init() {
        super.init();
        textWidgets.clear();
        subLandWidgets.clear();
        permissionTargetListWidget = addDrawableChild(new PermissionTargetListWidget(client, area, handler.fullName, this, width, height, 60, height));
        globalWidget = addDrawableChild(ButtonWidget.builder(Text.translatable("enclosure.widget.global"), button -> {
                assert client != null;
                client.setScreen(new PermissionScreen(area, new UUID(0, 0), handler.fullName, this));
            })
            .size(100, 20)
            .position(5, 35)
            .build());
        playerWidget = addDrawableChild(ButtonWidget.builder(Text.translatable("enclosure.widget.player"), button -> {
                assert client != null;
                button.active = false;
                unlistedWidget.active = true;
                permissionTargetListWidget.showPlayers();
            })
            .size(100, 20)
            .position(110, 35)
            .build());
        unlistedWidget = addDrawableChild(ButtonWidget.builder(Text.translatable("enclosure.widget.unspecified_player"), button -> {
                assert client != null;
                button.active = false;
                playerWidget.active = true;
                permissionTargetListWidget.showUnlistedPlayers();
            })
            .size(100, 20)
            .position(215, 35)
            .build());
        aboutWidget = addDrawableChild(ButtonWidget.builder(Text.translatable("enclosure.widget.about"), button -> {
                assert client != null;
                client.setScreen(new AboutScreen(this));
            })
            .size(50, 20)
            .position(320, 35)
            .build());
        String owner = UUIDCacheS2CPacket.getName(area.getOwner());
        assert client != null;
        if (!handler.fatherFullName.isEmpty()) {
            textWidgets.add(new ClickableTextWidget(client, this, Text.literal("<<< ")
                .styled(style -> style.withColor(Formatting.DARK_GREEN))
                .append(Text.literal(handler.fatherFullName).formatted(Formatting.GOLD)),
                Text.translatable("enclosure.widget.father_land.hover"),
                button -> {
                    assert client.player != null;
                    close();
                    client.player.networkHandler.sendChatCommand("enclosure gui " + handler.fatherFullName);
                }, 5, 5, width - 10));
        }
        textWidgets.add(new ClickableTextWidget(client, this, Text.empty()
            .append(Text.literal(area.getFullName()).styled(style -> style.withColor(Formatting.GOLD)))
            .append(" ")
            .append(Text.translatable("enclosure.info.created_by"))
            .append(" ")
            .append(owner == null ?
                Text.translatable("enclosure.message.unknown_user").styled(style -> style.withColor(Formatting.RED)) :
                Text.literal(owner).styled(style -> style.withColor(Formatting.GOLD)))
            .append(", ")
            .append(Text.translatable("enclosure.info.created_on"))
            .append(Text.literal(new SimpleDateFormat().format(area.getCreatedOn())).styled(style -> style.withColor(Formatting.GOLD))),
            null, null,
            5, 5, width - 10));
        textWidgets.add(new ClickableTextWidget(client, this, Text.translatable("enclosure.message.select.from")
            .append(Text.literal("[").styled(style -> style.withColor(Formatting.DARK_GREEN)))
            .append(Text.literal(String.valueOf(area.getMinX())).styled(style -> style.withColor(Formatting.GREEN)))
            .append(Text.literal(", ").styled(style -> style.withColor(Formatting.DARK_GREEN)))
            .append(Text.literal(String.valueOf(area.getMinY())).styled(style -> style.withColor(Formatting.GREEN)))
            .append(Text.literal(", ").styled(style -> style.withColor(Formatting.DARK_GREEN)))
            .append(Text.literal(String.valueOf(area.getMinZ())).styled(style -> style.withColor(Formatting.GREEN)))
            .append(Text.literal("]").styled(style -> style.withColor(Formatting.DARK_GREEN)))
            .append(Text.translatable("enclosure.message.select.to"))
            .append(Text.literal("[").styled(style -> style.withColor(Formatting.DARK_GREEN)))
            .append(Text.literal(String.valueOf(area.getMaxX())).styled(style -> style.withColor(Formatting.GREEN)))
            .append(Text.literal(", ").styled(style -> style.withColor(Formatting.DARK_GREEN)))
            .append(Text.literal(String.valueOf(area.getMaxY())).styled(style -> style.withColor(Formatting.GREEN)))
            .append(Text.literal(", ").styled(style -> style.withColor(Formatting.DARK_GREEN)))
            .append(Text.literal(String.valueOf(area.getMaxZ())).styled(style -> style.withColor(Formatting.GREEN)))
            .append(Text.literal("]").styled(style -> style.withColor(Formatting.DARK_GREEN)))
            .append(Text.translatable("enclosure.message.select.world"))
            .append(Text.literal(handler.worldId.toString()).styled(style -> style.withColor(Formatting.GOLD))),
            Text.translatable("enclosure.widget.selection_render.hover"),
            button -> {
                assert client.player != null;
                client.player.networkHandler.sendChatCommand("enclosure select land " + handler.fullName);
                close();
            }, 5, 20, width - 10));
        for (String name : handler.subAreaNames) {
            subLandWidgets.add(new ClickableTextWidget(client, this, Text.literal(">>> ")
                .styled(style -> style.withColor(Formatting.DARK_GREEN))
                .append(Text.literal(name).formatted(Formatting.GOLD)),
                Text.translatable("enclosure.widget.sub_land.hover"),
                button -> {
                    assert client.player != null;
                    close();
                    client.player.networkHandler.sendChatCommand("enclosure gui %s.%s".formatted(handler.fullName, name));
                }, 5, 5, 0));
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        renderBottom = 5;
        for (ClickableTextWidget textWidget : textWidgets) {
            textWidget.y = renderBottom;
            renderBottom += textWidget.calcHeight();
        }
        for (ClickableTextWidget textWidget : subLandWidgets) {
            textWidget.y = renderBottom;
        }
        renderBottom += subLandWidgets.isEmpty() ? 0 : 10;
        globalWidget.setY(renderBottom);
        playerWidget.setY(renderBottom);
        unlistedWidget.setY(renderBottom);
        aboutWidget.setY(renderBottom);
        permissionTargetListWidget.setTop(renderBottom + 25);
        super.render(matrices, mouseX, mouseY, delta);
        for (ClickableTextWidget textWidget : textWidgets) {
            textWidget.render(matrices, mouseX, mouseY, delta);
        }
        int subLandsX = 5;
        for (ClickableTextWidget textWidget : subLandWidgets) {
            textWidget.x = subLandsX;
            textWidget.render(matrices, mouseX, mouseY, delta);
            subLandsX += textWidget.width + 5;
        }
    }

    @Override
    public List<? extends Element> children() {
        List<Element> children = new ArrayList<>(super.children());
        children.addAll(textWidgets);
        children.addAll(subLandWidgets);
        return children;
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        textWidgets.forEach(textWidget -> textWidget.width = width);
        super.resize(client, width, height);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && this.shouldCloseOnEsc()) {
            this.close();
            return true;
        }
        if (getFocused() == null) {
            return false;
        }
        return getFocused().keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {

    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {

    }

    public void requestConfirm(Text readString) {
        assert client != null;
        client.execute(() -> client.setScreen(new ConfirmScreen(this, readString, () -> {
            assert client.player != null;
            client.player.networkHandler.sendCommand("enclosure confirm");
        })));
    }
}
