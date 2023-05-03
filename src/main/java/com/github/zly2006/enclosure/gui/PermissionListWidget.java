package com.github.zly2006.enclosure.gui;

import com.github.zly2006.enclosure.ReadOnlyEnclosureArea;
import com.github.zly2006.enclosure.utils.Permission;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
    private final ReadOnlyEnclosureArea area;
    private final UUID uuid;
    private final Permission.Target target;

    public PermissionListWidget(MinecraftClient minecraftClient, Screen parent, String fullName, ReadOnlyEnclosureArea area, UUID uuid,
                                int width, int height, int top, int bottom) {
        super(minecraftClient, width, height, top, bottom, 20);
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
        setRenderBackground(false); // 不渲染背景
        setRenderHorizontalShadows(true); // 渲染上下两道杠
    }

    @Override
    public int getRowWidth() {
        return width - 60;
    }

    @Override
    protected int getScrollbarPositionX() {
        return width - 15;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
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
            return value(getValue().orElse(null));
        }

        private Optional<Boolean> getValue() {
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
        public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            buttonWidget.setY(y);
            buttonWidget.setX(x + entryWidth - 40);
            buttonWidget.setMessage(value());
            buttonWidget.render(matrices, mouseX, mouseY, tickDelta);
            client.textRenderer.draw(matrices, permission.getName(), x + 20, y + 3, 0xFFFFFF);
            client.textRenderer.draw(matrices, permission.getDescription(), x + 140, y + 3, 0x999999);
            if (permission.getIcon() != null) {
                client.getItemRenderer().renderInGui(matrices, new ItemStack(permission.getIcon()), x, y);
            }
            else {
                client.getItemRenderer().renderInGui(matrices, new ItemStack(Items.STRUCTURE_VOID), x, y);
            }
            if (buttonWidget.isHovered()) {
                parent.renderTooltip(matrices, List.of(
                    Text.translatable("enclosure.widget.click.left").styled(style -> style.withColor(Formatting.GREEN)),
                    Text.translatable("enclosure.widget.click.right").styled(style -> style.withColor(Formatting.RED))
                ), mouseX, mouseY);
            }
            else if (hovered) {
                parent.renderTooltip(matrices,
                    List.of(permission.getDescription(),
                        Text.translatable("enclosure.widget.default_value_is").setStyle(Style.EMPTY.withColor(Formatting.GOLD))
                            .append(" ").append(value(permission.getDefaultValue()))),
                    mouseX, mouseY);
            }
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return List.of(buttonWidget);
        }

        @Override
        public List<? extends Element> children() {
            return List.of(buttonWidget);
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
                    Boolean value = getValue().orElse(null);
                    if (value == null) setValue(true);
                    else setValue(null);

                    client.player.networkHandler.sendChatCommand("enclosure set " + fullName + " uuid " +
                        uuid.toString() + " " +
                        permission.getName() + " " +
                        getValue().map(String::valueOf).orElse("none"));
                    buttonWidget.setMessage(value());
                    return true;
                }
                else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    Boolean value = getValue().orElse(null);
                    if (value == null) setValue(false);
                    else setValue(null);
                    client.player.networkHandler.sendChatCommand("enclosure set " + fullName + " uuid " +
                        uuid.toString() + " " +
                        permission.getName() + " " +
                        getValue().map(String::valueOf).orElse("none"));
                    buttonWidget.setMessage(value());
                    return true;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public class SearchEntry extends Entry {
        TextFieldWidget searchWidget;

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
