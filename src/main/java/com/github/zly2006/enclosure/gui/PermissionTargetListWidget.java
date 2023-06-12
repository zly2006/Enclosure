package com.github.zly2006.enclosure.gui;

import com.github.zly2006.enclosure.EnclosureView;
import com.github.zly2006.enclosure.network.UUIDCacheS2CPacket;
import kotlin.jvm.functions.Function2;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.github.zly2006.enclosure.command.EnclosureCommandKt.CONSOLE;

public class PermissionTargetListWidget<T extends ButtonWidget> extends ElementListWidget<PermissionTargetListWidget.Entry> {
    final EnclosureView.ReadOnly area;
    final String fullName;
    final Screen parent;
    private final Function2<PermissionTargetListWidget<T>, UUID, T> buttonFactory;

    enum Mode {
        Players,
        Unspecified,
    }
    Mode mode = Mode.Players;
    final SearchEntry searchEntry = new SearchEntry();

    public PermissionTargetListWidget(MinecraftClient minecraftClient, EnclosureView.ReadOnly area, String fullName, Screen parent, int width, int height, int top, int bottom, Function2<PermissionTargetListWidget<T>, UUID, T> buttonFactory) {
        super(minecraftClient, width, height, top, bottom, 20);
        this.area = area;
        this.fullName = fullName;
        this.parent = parent;
        this.buttonFactory = buttonFactory;
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

        PlayerEntry(Text name, UUID uuid) {
            this.name = name;
            this.uuid = uuid;
            this.setButton = buttonFactory.invoke(PermissionTargetListWidget.this, uuid);
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
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            context.drawText(client.textRenderer, name, x + 20, y + 3, 0xFFFFFF, false);
            setButton.setX(x + entryWidth - setButton.getWidth());
            setButton.setY(y);
            setButton.render(context, mouseX, mouseY, tickDelta);
            assert client.player != null;
            Optional.ofNullable(client.player.networkHandler.getPlayerListEntry(uuid))
                    .map(PlayerListEntry::getSkinTexture)
                    .ifPresent(texture -> {
                        PlayerSkinDrawer.draw(context, texture, x, y, 16);
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
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            searchWidget.setY(y);
            searchWidget.setX(x + 70);
            searchWidget.setWidth(entryWidth - 70 - 2);
            searchWidget.render(context, mouseX, mouseY, tickDelta);
            context.drawText(client.textRenderer, Text.translatable("enclosure.widget.search"), x, y + 3, 0xFFFFFF, false);
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
