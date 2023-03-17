package com.github.zly2006.enclosure;

import com.github.zly2006.enclosure.exceptions.PermissionTargetException;
import com.github.zly2006.enclosure.utils.Permission;
import com.github.zly2006.enclosure.utils.Serializable2Text;
import com.github.zly2006.enclosure.utils.TrT;
import com.github.zly2006.enclosure.utils.Utils;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.github.zly2006.enclosure.commands.EnclosureCommand.CONSOLE;

public interface PermissionHolder extends Serializable2Text {
    boolean isOwner(ServerCommandSource source);

    UUID getOwner();

    boolean isOwnerOrFatherAdmin(ServerCommandSource source) throws CommandSyntaxException;

    PermissionHolder getFather();

    /**
     * 这个方法会判断默认权限，uuid那个不会
     */
    default boolean hasPerm(@NotNull ServerPlayerEntity player, @NotNull Permission perm) {
        if (player.getCommandSource().hasPermissionLevel(4) && perm.isIgnoreOp()) {
            return true;
        }
        try {
            if (perm == Permission.ADMIN && isOwnerOrFatherAdmin(player.getCommandSource())) {
                return true;
            }
        } catch (CommandSyntaxException e) {
            return false;
        }
        return hasPerm(player.getUuid(), perm);
    }
    /**
     * uuid版本只检查permissionMap, 不判断默认权限
     */
    default boolean hasPerm(@NotNull UUID uuid, @NotNull Permission perm) {
        if (perm.getTarget().fitPlayer() && getPermissionsMap().containsKey(uuid)) {
            Optional<Boolean> ob = perm.get(getPermissionsMap().get(uuid));
            if (ob.isPresent()) {
                return ob.get();
            }
        }
        if (perm.getTarget().fitEnclosure() && getPermissionsMap().containsKey(CONSOLE)) {
            Optional<Boolean> ob = perm.get(getPermissionsMap().get(CONSOLE));
            if (ob.isPresent()) {
                return ob.get();
            }
        }
        if (getFather() != null && inheritPermission()) {
            return getFather().hasPerm(uuid, perm);
        }
        return perm.getDefaultValue();
    }

    default boolean hasPubPerm(@NotNull Permission perm) throws PermissionTargetException {
        if (!perm.getTarget().fitEnclosure()) {
            throw new PermissionTargetException(TrT.of("enclosure.message.permission_target_error").append(new LiteralText(perm.getTarget().name())));
        }
        return hasPerm(CONSOLE, perm);
    }

    default void setPermission(@Nullable ServerCommandSource source, @NotNull UUID uuid, @NotNull Permission perm, @Nullable Boolean value) throws PermissionTargetException {
        if (!getPermissionsMap().containsKey(uuid) && value != null) {
            getPermissionsMap().put(uuid, new HashMap<>());
        }
        if (uuid.equals(CONSOLE) && !perm.getTarget().fitEnclosure()) {
            throw new PermissionTargetException(TrT.of("enclosure.message.permission_target_error").append(perm.getTarget().name()));
        } else if (!uuid.equals(CONSOLE) && !perm.getTarget().fitPlayer()) {
            throw new PermissionTargetException(TrT.of("enclosure.message.permission_target_error").append(perm.getTarget().name()));
        }
        perm.set(getPermissionsMap().get(uuid), value);
        if (getPermissionsMap().get(uuid).isEmpty()) {
            getPermissionsMap().remove(uuid);
        }
    }
    Map<UUID, Map<String, Boolean>> getPermissionsMap();

    String getName();

    default String getFullName() {
        return getName();
    }

    default Text serializePermission(Map<String, Boolean> map) {
        MutableText text = new LiteralText("");
        map.forEach((key, value) -> {
            if (value) {
                text.append(new LiteralText(key).setStyle(Style.EMPTY.withColor(Formatting.GREEN)));
            } else {
                text.append(new LiteralText(key).setStyle(Style.EMPTY.withColor(Formatting.RED)));
            }
            text.append(" ");
        });
        return text;
    }

    String getSetPermissionCommand(UUID uuid);

    @Override
    default MutableText serialize(SerializationSettings settings, ServerPlayerEntity player) {
        if (settings != SerializationSettings.Full) return null;
        MutableText text = TrT.of("enclosure.message.permissions_header");

        getPermissionsMap().entrySet().stream().map(pair -> {
                    var key = pair.getKey();
                    var value = pair.getValue();
                    int ordinal = -1;
                    Style style = Style.EMPTY
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, getSetPermissionCommand(key)))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    key.equals(CONSOLE) ? serializePermission(value) :
                                            // 不是默认的，就显示uuid
                                            new LiteralText("UUID=" + key + ": ").setStyle(Style.EMPTY.withColor(Formatting.GOLD))
                                                    .append(serializePermission(value))));
                    String name = null;
                    if (key.equals(CONSOLE)) {
                        name = "@GLOBAL";
                        style = style.withColor(Formatting.BLUE);
                        ordinal = 0;
                    }
                    if (key.equals(getOwner())) {
                        style = style.withColor(Formatting.GREEN);
                        ordinal = 1;
                    }
                    if (name == null) {
                        name = Utils.getNameByUUID(key);
                        ordinal = 2;
                    }
                    if (name == null) {
                        name = "UNKNOWN";
                        style = style.withColor(Formatting.RED);
                        ordinal = 3;
                    }
                    MutableText item = new LiteralText(name).setStyle(style).append(" ");
                    return Pair.of(item, ordinal);
                })
                .sorted(Comparator.comparingInt(Pair::getSecond))
                .forEach(pair -> text.append(pair.getFirst()));
        return text;
    }

    void onRemoveChild(PermissionHolder child);
    void onAddChild(PermissionHolder child);
    boolean inheritPermission();
}
