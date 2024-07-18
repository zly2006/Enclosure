package com.github.zly2006.enclosure.gui;

import com.github.zly2006.enclosure.EnclosureView;
import com.github.zly2006.enclosure.network.config.UUIDCacheS2CPacket;
import com.github.zly2006.enclosure.utils.UtilsKt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EnclosureScreen extends HandledScreen<EnclosureScreenHandler> implements EnclosureGui {
    final EnclosureView.ReadOnly area;
    PermissionTargetListWidget<ButtonWidget> permissionTargetListWidget;
    ButtonWidget globalWidget;
    ButtonWidget playerWidget;
    ButtonWidget unlistedWidget;
    ButtonWidget aboutWidget;
    @Nullable
    ButtonWidget transferWidget;
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
        permissionTargetListWidget = addDrawableChild(new PermissionTargetListWidget<ButtonWidget>(client, area, handler.fullName, this, width, height, 60, height,
                (widget, uuid) -> ButtonWidget.builder(Text.translatable("enclosure.widget.set"), button -> {
                    if (client != null) {
                        client.setScreen(new PermissionScreen(area, uuid, widget.fullName, widget.parent));
                    }
                }).size(40, 20).build()
        ));
        globalWidget = addDrawableChild(ButtonWidget.builder(Text.translatable("enclosure.widget.global"), button -> {
                assert client != null;
                client.setScreen(new PermissionScreen(area, new UUID(0, 0), handler.fullName, this));
            })
            .size(100, 20)
            .position(5, 35)
            .build());
        playerWidget = addDrawableChild(ButtonWidget.builder(Text.translatable("enclosure.widget.showplayer"), button -> {
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
        if (area.getOwner().equals(client.player.getUuid()) || client.player.hasPermissionLevel(4)) {
            transferWidget = addDrawableChild(ButtonWidget.builder(Text.translatable("enclosure.widget.transfer"), button -> {
                client.setScreen(new TransferScreen(area, handler.fullName, this));
            }).position(5, 0).build());
        }
        String owner = UUIDCacheS2CPacket.getName(area.getOwner());
        assert client != null;
        if (handler.fatherFullName != null && !handler.fatherFullName.isEmpty()) {
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
            .append(Text.translatable("enclosure.info.created",
                Text.literal(area.getFullName()).styled(style -> style.withColor(Formatting.GOLD)),
                (owner == null ?
                Text.translatable("enclosure.message.unknown_user").styled(style -> style.withColor(Formatting.RED)) : Text.literal(owner).styled(style -> style.withColor(Formatting.GOLD))),
                Text.literal(new SimpleDateFormat().format(area.getCreatedOn())).styled(style -> style.withColor(Formatting.GOLD))
            )),
            null, null,
            5, 5, width - 10));
        MutableText selectionText = UtilsKt.formatSelection(handler.worldId, area.getMinX(), area.getMinY(), area.getMinZ(), area.getMaxX(), area.getMaxY(), area.getMaxZ());
        textWidgets.add(new ClickableTextWidget(client, this, selectionText, Text.translatable("enclosure.widget.selection_render.hover"),
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
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {

    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBottom = 5;
        for (ClickableTextWidget textWidget : textWidgets) {
            textWidget.y = renderBottom;
            renderBottom += textWidget.calcHeight();
        }

        for (ClickableTextWidget textWidget : subLandWidgets) {
            textWidget.y = renderBottom;
        }
        if (!subLandWidgets.isEmpty()) {
            renderBottom += 10;
        }

        globalWidget.setY(renderBottom);
        playerWidget.setY(renderBottom);
        unlistedWidget.setY(renderBottom);
        aboutWidget.setY(renderBottom);
        if (transferWidget != null) {
            transferWidget.setY(renderBottom + 20);
            permissionTargetListWidget.setTop(renderBottom + 45);
        }
        else {
            permissionTargetListWidget.setTop(renderBottom + 25);
        }
        super.render(context, mouseX, mouseY, delta);
        if (!subLandWidgets.isEmpty()) {
            int subLandsX = 5;
            for (ClickableTextWidget textWidget : subLandWidgets) {
                textWidget.x = subLandsX;
                textWidget.render(context, mouseX, mouseY, delta);
                subLandsX += textWidget.width + 5;
            }
        }
        for (ClickableTextWidget textWidget : textWidgets) {
            textWidget.render(context, mouseX, mouseY, delta);
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
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) { }

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
}
