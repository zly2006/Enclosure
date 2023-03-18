package com.github.zly2006.enclosure;

import com.github.zly2006.enclosure.utils.Utils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.github.zly2006.enclosure.ServerMain.Instance;

public class EnclosureGroup implements PermissionHolder {
    public static final String GROUPS_KEY = "enclosure.groups";
    String name;
    UUID owner;
    Set<String> enclosures = new HashSet<>();
    Map<UUID, Map<String, Boolean>> permissionsMap = new HashMap<>();
    public EnclosureGroup() {
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSetPermissionCommand(UUID uuid) {
        return "/enclosure groups set " + getFullName() + " uuid " + uuid + " ";
    }

    public UUID getOwner() {
        return owner;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public Set<String> getEnclosures() {
        return enclosures;
    }

    @Override
    public MutableText serialize(@NotNull SerializationSettings settings, ServerPlayerEntity player) {
        return switch (settings) {
            case Name -> new LiteralText(name);
            case Summarize -> serialize(SerializationSettings.Name, player).styled(style -> style
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, serialize(SerializationSettings.Hover, player))));
            case Full -> {
                MutableText text = (MutableText) MutableText.EMPTY;
                for (String enclosure : enclosures) {
                    text.append(Instance.getEnclosure(enclosure).serialize(SerializationSettings.Summarize, player)).append(", ");
                }
                yield new LiteralText("Group: ").append(serialize(SerializationSettings.Name, player))
                        .append("\nOwner: ").append(Utils.getDisplayNameByUUID(owner))
                        .append("\nEnclosures: ").append(text)
                        .append("\n").append(PermissionHolder.super.serialize(SerializationSettings.Full, player));
            }
            case Hover -> new LiteralText("Owner: ").append(Utils.getDisplayNameByUUID(owner));
            default -> null;
        };
    }

    @Override
    public boolean isOwner(ServerCommandSource source) {
        if (source.hasPermissionLevel(4)) {
            return true;
        } else if (source.getEntity() instanceof ServerPlayerEntity) {
            return source.getEntity().getUuid().equals(owner);
        } else {
            return false;
        }
    }

    @Override
    public boolean isOwnerOrFatherAdmin(ServerCommandSource source) {
        return isOwner(source);
    }

    @Override
    public PermissionHolder getFather() {
        return null;
    }

    @Override
    public Map<UUID, Map<String, Boolean>> getPermissionsMap() {
        return this.permissionsMap;
    }

    @Override
    public void onRemoveChild(PermissionHolder child) {
        enclosures.remove(child.getFullName());
        Instance.groups.markDirty();
    }

    @Override
    public void onAddChild(PermissionHolder child) {
        enclosures.add(child.getFullName());
        Instance.groups.markDirty();
    }

    @Override
    public boolean inheritPermission() {
        return true;
    }
    //todo 修复我
    public static class Groups extends PersistentState {
        Map<String, EnclosureGroup> groups = new HashMap<>();
        public Groups() {
            markDirty();
        }
        public Groups(NbtCompound nbt) {
            nbt.getKeys().forEach(key -> {
                EnclosureGroup group = new EnclosureGroup();
                NbtCompound groupNbt = nbt.getCompound(key);
                group.name = key;
                group.owner = groupNbt.getUuid("owner");
                groupNbt.getList("enclosures", NbtList.STRING_TYPE).forEach(nbtElement -> group.enclosures.add(nbtElement.asString()));
                groupNbt.getCompound("permissions").getKeys().forEach(uuid -> {
                    NbtCompound permNbt = groupNbt.getCompound("permissions").getCompound(uuid);
                    permNbt.getKeys().forEach(perm -> group.permissionsMap.computeIfAbsent(UUID.fromString(uuid), k -> new HashMap<>()).put(perm, permNbt.getBoolean(perm)));
                });
                groups.put(key, group);
            });
        }
        @Override
        public NbtCompound writeNbt(NbtCompound nbt) {
            groups.forEach((name, group) -> {
                NbtCompound sub = new NbtCompound();
                sub.putUuid("owner", group.owner);
                NbtList enclosures = new NbtList();
                group.enclosures.forEach(n -> enclosures.add(NbtString.of(n)));
                sub.put("enclosures", enclosures);
                NbtCompound permissions = new NbtCompound();
                group.permissionsMap.forEach((uuid, perms) -> {
                    NbtCompound permNbt = new NbtCompound();
                    perms.forEach(permNbt::putBoolean);
                    permissions.put(uuid.toString(), permNbt);
                });
                sub.put("permissions", permissions);
                nbt.put(name, sub);
            });
            return nbt;
        }
        public void addGroup(EnclosureGroup group) {
            groups.put(group.name, group);
        }
        public void removeGroup(String name) {
            groups.remove(name);
        }
        public EnclosureGroup getGroup(String name) {
            return groups.get(name);
        }
        public Collection<EnclosureGroup> getGroups() {
            return groups.values();
        }
    }
}
