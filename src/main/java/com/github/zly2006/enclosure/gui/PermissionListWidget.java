package com.github.zly2006.enclosure.gui;

import com.github.zly2006.enclosure.EnclosureView;
import com.github.zly2006.enclosure.utils.Permission;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Supplier;

import static com.github.zly2006.enclosure.command.EnclosureCommandKt.CONSOLE;

public class PermissionListWidget extends ElementListWidget<PermissionListWidget.Entry> {
    private final Screen parent;
    private final String fullName;
    private final EnclosureView.ReadOnly area;
    private final UUID uuid;
    private final Permission.Target target;

    public PermissionListWidget(MinecraftClient minecraftClient, Screen parent, String fullName, EnclosureView.ReadOnly area, UUID uuid,
                                int width, int height, int top, int bottom) {
        super(minecraftClient, width, height, top, 20);
        this.parent = parent;
        this.fullName = fullName;
        this.area = area;
        this.uuid = uuid;
        this.target = uuid.equals(CONSOLE) ? Permission.Target.Enclosure : Permission.Target.Player;
        addEntry(new SearchEntry());
        Permission.PERMISSIONS.values().stream().filter(permission ->
                        (permission.getTarget().fitPlayer() && target.fitPlayer()) ||
                                (permission.getTarget().fitEnclosure() && target.fitEnclosure()))
                .sorted(Comparator.comparing(Permission::getName))
                .forEach(permission -> addEntry(new PermissionEntry(permission)));
    }

    @Override
    public int getRowWidth() {
        return width - 60;
    }

    @Environment(EnvType.CLIENT)
    public abstract static class Entry extends ElementListWidget.Entry<Entry> { }

    @Environment(EnvType.CLIENT)
    public class PermissionEntry extends Entry {
        final ButtonWidget buttonWidget;
        final Permission permission;

        private Text value(Boolean value) {
            return value == null ? Text.translatable("enclosure.widget.none").setStyle(Style.EMPTY.withColor(Formatting.DARK_AQUA))
                    : value ? Text.translatable("enclosure.widget.true").setStyle(Style.EMPTY.withColor(Formatting.GREEN))
                            : Text.translatable("enclosure.widget.false").setStyle(Style.EMPTY.withColor(Formatting.RED));
        }
        private Text value() {
            return value(getValue());
        }

        private @Nullable Boolean getValue() {
            return permission.getValue(area.getPermissionsMap().getOrDefault(uuid, Collections.emptyMap()));
        }

        private void setValue(@Nullable Boolean value) {
            Map<String, Boolean> perm = area.getPermissionsMap().getOrDefault(uuid, new HashMap<>());
            permission.setValue(perm, value);
            area.getPermissionsMap().put(uuid, perm);
        }

        public PermissionEntry(Permission permission) {
            this.permission = permission;
            buttonWidget = new SetButtonWidget(0, 0, 40, 20, value(), buttonWidget -> {}, Supplier::get);
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return List.of(buttonWidget);
        }

        @Override
        public List<? extends Element> children() {
            return List.of(buttonWidget);
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            buttonWidget.setY(y);
            buttonWidget.setX(x + entryWidth - buttonWidget.getWidth());
            buttonWidget.setMessage(value());
            buttonWidget.render(context, mouseX, mouseY, tickDelta);
            context.drawText(client.textRenderer, permission.getName(), x + 20, y + 3, 0xFFFFFF, false);
            context.drawText(client.textRenderer, permission.getDescription(), x + 140, y + 3, 0x999999, false);
            permission.getIcon();
            context.drawItem(permission.getIcon(), x, y);
            if (buttonWidget.isHovered()) {
                context.drawTooltip(client.textRenderer, List.of(
                        Text.translatable("enclosure.widget.click.left").styled(style -> style.withColor(Formatting.GREEN)),
                        Text.translatable("enclosure.widget.click.right").styled(style -> style.withColor(Formatting.RED))
                ), mouseX, mouseY);
            }
            else if (hovered) {
                context.drawTooltip(client.textRenderer,
                        List.of(permission.getDescription(),
                                Text.translatable("enclosure.widget.default_value_is").setStyle(Style.EMPTY.withColor(Formatting.GOLD))
                                        .append(" ").append(value(permission.getDefaultValue()))),
                        mouseX, mouseY);
            }
        }

        public class SetButtonWidget extends ButtonWidget {
            public SetButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress, NarrationSupplier narrationSupplier) {
                super(x, y, width, height, message, onPress, narrationSupplier);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                assert client.player != null;
                if (!visible || !active) {
                    return false;
                }
                else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    Boolean value = getValue();
                    if (value == null) setValue(true);
                    else setValue(null);

                    client.player.networkHandler.sendChatCommand("enclosure set " + fullName + " uuid " +
                        uuid.toString() + " " +
                        permission.getName() + " " +
                        Optional.ofNullable(getValue()).map(String::valueOf).orElse("none"));
                    buttonWidget.setMessage(value());
                    return true;
                }
                else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    Boolean value = getValue();
                    if (value == null) setValue(false);
                    else setValue(null);
                    client.player.networkHandler.sendChatCommand("enclosure set " + fullName + " uuid " +
                        uuid.toString() + " " +
                        permission.getName() + " " +
                        Optional.ofNullable(getValue()).map(String::valueOf).orElse("none"));
                    buttonWidget.setMessage(value());
                    return true;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }
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
                Permission.PERMISSIONS.values().stream().filter(permission ->
                                (permission.getTarget().fitPlayer() && target.fitPlayer()) ||
                                        (permission.getTarget().fitEnclosure() && target.fitEnclosure()))
                        .filter(permission -> {
                            if (permission.getName().contains(s))
                                return true;
                            if (permission.getDescription().getContent() instanceof TranslatableTextContent content) {
                                return Language.getInstance().hasTranslation(content.getKey()) &&
                                        Language.getInstance().get(content.getKey()).contains(s);
                            }
                            return false;
                        })
                        .sorted(Comparator.comparing(Permission::getName))
                        .forEach(permission -> addEntry(new PermissionEntry(permission)));
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

    @Override
    protected boolean isSelectButton(int button) {
        return true;
    }
}
