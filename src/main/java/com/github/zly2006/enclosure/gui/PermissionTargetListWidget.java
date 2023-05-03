package com.github.zly2006.enclosure.gui;

import com.github.zly2006.enclosure.ReadOnlyEnclosureArea;
import com.github.zly2006.enclosure.network.UUIDCacheS2CPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.github.zly2006.enclosure.command.EnclosureCommandKt.CONSOLE;

public class PermissionTargetListWidget extends ElementListWidget<PermissionTargetListWidget.Entry> {
    final ReadOnlyEnclosureArea area;
    final String fullName;
    final Screen parent;
    enum Mode {
        Players,
        Unspecified,
    }
    Mode mode = Mode.Players;
    final SearchEntry searchEntry = new SearchEntry();

    public PermissionTargetListWidget(MinecraftClient minecraftClient, ReadOnlyEnclosureArea area, String fullName, Screen parent, int width, int height, int top, int bottom) {
        super(minecraftClient, width, height, top, bottom, 20);
        this.area = area;
        this.fullName = fullName;
        this.parent = parent;
        setRenderBackground(false); // 不渲染背景
    }

    @Override
    public int getRowWidth() {
        return width - 60;
    }

    @Override
    protected int getScrollbarPositionX() {
        return width - 15;
    }

    public void showPlayers() {
        clearEntries();
        mode = Mode.Players;
        setScrollAmount(0);
        addEntry(searchEntry);
        area.getPermissionsMap().keySet().stream()
                .filter(uuid -> !uuid.equals(CONSOLE))
                .map(uuid -> new PlayerEntry(Text.of(UUIDCacheS2CPacket.getName(uuid)), uuid))
                .sorted(Comparator.comparing(o -> o.name.getString()))
                .forEach(this::addEntry);
    }

    public void showUnlistedPlayers() {
        clearEntries();
        mode = Mode.Unspecified;
        setScrollAmount(0);
        addEntry(searchEntry);
        UUIDCacheS2CPacket.uuid2name.keySet().stream()
                .filter(uuid -> !CONSOLE.equals(uuid))
                .filter(uuid -> !area.getPermissionsMap().containsKey(uuid))
                .map(uuid -> new PlayerEntry(Text.of(UUIDCacheS2CPacket.getName(uuid)), uuid))
                .sorted(Comparator.comparing(o -> o.name.getString()))
                .forEach(this::addEntry);
    }

    public void setTop(int top) {
        this.top = top;
    }

    abstract static class Entry extends ElementListWidget.Entry<Entry> { }
    class PlayerEntry extends Entry {
        final Text name;
        final UUID uuid;
        final ButtonWidget setButton;
        private PermissionScreen screen = null;

        PlayerEntry(Text name, UUID uuid) {
            this.name = name;
            this.uuid = uuid;
            this.setButton = ButtonWidget.builder(Text.translatable("enclosure.widget.set"), button -> {
                if (screen == null) {
                    screen = new PermissionScreen(area, uuid, fullName, parent);
                }
                client.setScreen(screen);
            }).size(40, 20).build();
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return List.of(setButton);
        }

        @Override
        public List<? extends Element> children() {
            return List.of(setButton);
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            client.textRenderer.draw(matrices, name, x + 20, y + 3, 0xffffff);
            setButton.setX(x + entryWidth - 40);
            setButton.setY(y);
            setButton.render(matrices, mouseX, mouseY, tickDelta);
            assert client.player != null;
            Optional.ofNullable(client.player.networkHandler.getPlayerListEntry(uuid))
                    .map(PlayerListEntry::getSkinTexture)
                    .ifPresent(texture -> {
                        RenderSystem.setShaderTexture(0, texture);
                        PlayerSkinDrawer.draw(matrices, x, y, 16);
                    });
        }
    }
    @Environment(EnvType.CLIENT)
    public class SearchEntry extends Entry {
        final TextFieldWidget searchWidget;

        public SearchEntry() {
            searchWidget = new TextFieldWidget(client.textRenderer, 0, 0, 100, 16, Text.of("search"));
            searchWidget.setChangedListener(s -> {
                clearEntries();
                addEntry(this);
                Stream<PlayerEntry> entryStream = Stream.of();
                switch (mode) {
                    case Players -> entryStream = area.getPermissionsMap().keySet().stream()
                            .filter(uuid -> !uuid.equals(CONSOLE))
                            .map(uuid -> new PlayerEntry(Text.literal(UUIDCacheS2CPacket.getName(uuid)), uuid));
                    case Unspecified -> entryStream = UUIDCacheS2CPacket.uuid2name.keySet().stream()
                            .map(uuid -> new PlayerEntry(Text.literal(UUIDCacheS2CPacket.getName(uuid)), uuid));
                }
                entryStream.filter(entry -> entry.name.getString().contains(s))
                        .sorted(Comparator.comparing(o -> o.name.getString()))
                        .forEach(PermissionTargetListWidget.this::addEntry);
            });
        }

        @Override
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            searchWidget.setY(y);
            searchWidget.setX(x + 70);
            searchWidget.setWidth(entryWidth - 70 - 2);
            searchWidget.render(matrices, mouseX, mouseY, tickDelta);
            client.textRenderer.draw(matrices, Text.translatable("enclosure.widget.search"), x, y + 3, 0xFFFFFF);
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return List.of(searchWidget);
        }

        @Override
        public List<? extends Element> children() {
            return List.of(searchWidget);
        }
    }
}
