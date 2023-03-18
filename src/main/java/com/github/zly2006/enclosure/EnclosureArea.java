package com.github.zly2006.enclosure;

import com.github.zly2006.enclosure.commands.Session;
import com.github.zly2006.enclosure.exceptions.PermissionTargetException;
import com.github.zly2006.enclosure.utils.Permission;
import com.github.zly2006.enclosure.utils.TrT;
import com.github.zly2006.enclosure.utils.Utils;
import dev.architectury.registry.registries.Registries;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.github.zly2006.enclosure.ServerMain.Instance;
import static com.github.zly2006.enclosure.ServerMain.minecraftServer;
//todo 修复我
public class EnclosureArea extends PersistentState implements PermissionHolder {
    int minX;
    int minY;
    int minZ;
    int maxX;
    int maxY;
    int maxZ;
    ServerWorld world;
    String name = "";
    UUID owner;
    Vec3d teleportPos;
    float yaw;
    float pitch;
    @NotNull
    String enterMessage = "";
    @NotNull
    String leaveMessage = "";
    Map<UUID, Map<String, Boolean>> permissionsMap = new HashMap<>();
    long createdOn = 0;
    PermissionHolder father;

    private EnclosureArea() {
    }

    /**
     * Create an instance from nbt
     *
     * @param compound nbt compound
     */
    public EnclosureArea(NbtCompound compound) {
        name = compound.getString("name");
        minX = compound.getInt("min_x");
        minY = compound.getInt("min_y");
        minZ = compound.getInt("min_z");
        maxX = compound.getInt("max_x");
        maxY = compound.getInt("max_y");
        maxZ = compound.getInt("max_z");
        yaw = compound.getFloat("yaw");
        pitch = compound.getInt("pitch");
        enterMessage = compound.getString("enter_msg");
        leaveMessage = compound.getString("leave_msg");
        createdOn = compound.getLong("created_on");
        NbtList tpPos = (NbtList) compound.get("tp_pos");
        assert tpPos != null;
        teleportPos = new Vec3d(
            tpPos.getDouble(0),
            tpPos.getDouble(1),
            tpPos.getDouble(2)
        );
        for (String player : compound.getCompound("permission").getKeys()) {
            Map<String, Boolean> perm = new HashMap<>();
            NbtCompound nbtPerm = compound.getCompound("permission").getCompound(player);
            for (String key : nbtPerm.getKeys()) {
                perm.put(key, nbtPerm.getBoolean(key));
            }
            permissionsMap.put(UUID.fromString(player), perm);
        }
        owner = compound.getUuid("owner");
    }

    public EnclosureArea(Session session, String name) {
        owner = session.getOwner();
        world = session.getWorld();
        this.name = name;
        permissionsMap.put(owner, new HashMap<>());
        Permission.PERMISSIONS.values().stream().filter(p -> p.getTarget().fitPlayer())
            .forEach(p -> p.set(permissionsMap.get(owner), true));
        minX = Math.min(session.getPos1().getX(), session.getPos2().getX());
        minY = Math.min(session.getPos1().getY(), session.getPos2().getY());
        minZ = Math.min(session.getPos1().getZ(), session.getPos2().getZ());
        maxX = Math.max(session.getPos1().getX(), session.getPos2().getX());
        maxY = Math.max(session.getPos1().getY(), session.getPos2().getY());
        maxZ = Math.max(session.getPos1().getZ(), session.getPos2().getZ());
        yaw = 0;
        pitch = 0;
        int centerX = (minX + maxX) / 2;
        int centerZ = (minZ + maxZ) / 2;
        int centerY = Utils.topYOf(session.getWorld(), centerX, centerZ, maxY);
        if (centerY < minY) {
            centerY = maxY;
        }
        createdOn = System.currentTimeMillis();
        teleportPos = new Vec3d(centerX, centerY, centerZ);
        markDirty();
    }

    @Contract(" -> new")
    public static @NotNull EnclosureArea empty() {
        return new EnclosureArea();
    }

    public void kickPlayer(ServerPlayerEntity player) {
        int x = minX - 1;
        int z = minZ - 1;
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y == 0) {
            // this player is alive, but in a void
            minecraftServer.getPlayerManager().respawnPlayer(player, true);
            minecraftServer.getOverworld().getChunkManager().updatePosition(player);
        }
        else {
            player.teleport(world, x, y, z, 0, 0);
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putString("name", name);
        nbt.putInt("min_x", minX);
        nbt.putInt("min_y", minY);
        nbt.putInt("min_z", minZ);
        nbt.putInt("max_x", maxX);
        nbt.putInt("max_y", maxY);
        nbt.putInt("max_z", maxZ);
        nbt.putLong("created_on", createdOn);
        nbt.putString("enter_msg", enterMessage);
        nbt.putString("leave_msg", leaveMessage);
        NbtCompound nbtPermission = new NbtCompound();
        permissionsMap.forEach((key, value) -> {
            NbtCompound compound = new NbtCompound();
            for (Map.Entry<String, Boolean> entry : value.entrySet()) {
                compound.putBoolean(entry.getKey(), entry.getValue());
            }
            nbtPermission.put(key.toString(), compound);
        });
        nbt.put("permission", nbtPermission);
        NbtList nbtTpPos = new NbtList();
        nbt.putFloat("yaw", yaw);
        nbt.putFloat("pitch", pitch);
        nbtTpPos.add(NbtDouble.of(teleportPos.getX()));
        nbtTpPos.add(NbtDouble.of(teleportPos.getY()));
        nbtTpPos.add(NbtDouble.of(teleportPos.getZ()));
        nbt.put("tp_pos", nbtTpPos);
        nbt.putUuid("owner", owner);
        return nbt;
    }

    public EnclosureArea areaOf(BlockPos pos) {
        if (isInner(pos)) {
            return this;
        } else {
            throw new RuntimeException("The position " + pos + " is not in the area" + name);
        }
    }

    public boolean includesArea(EnclosureArea area) {
        return minX <= area.minX &&
            minY <= area.minY &&
            minZ <= area.minZ &&
            maxX >= area.maxX &&
            maxY >= area.maxY &&
            maxZ >= area.maxZ;
    }

    public boolean inArea(EnclosureArea area) {
        return minX >= area.minX &&
            minY >= area.minY &&
            minZ >= area.minZ &&
            maxX <= area.maxX &&
            maxY <= area.maxY &&
            maxZ <= area.maxZ;
    }

    public boolean intersect(@NotNull EnclosureArea area) {
        return intersect(area.minX, area.minY, area.minZ, area.maxX, area.maxY, area.maxZ);
    }

    public boolean intersect(int x1, int y1, int z1, int x2, int y2, int z2) {
        return this.minX <= x2
            && this.maxX >= x1
            && this.minY <= y2
            && this.maxY >= y1
            && this.minZ <= z2
            && this.maxZ >= z1;
    }

    public boolean isSame(Object obj) {
        if (obj instanceof EnclosureArea enclosure) {
            return minX == enclosure.minX &&
                minY == enclosure.minY &&
                minZ == enclosure.minZ &&
                maxX == enclosure.maxX &&
                maxY == enclosure.maxY &&
                maxZ == enclosure.maxZ;
        } else {
            return false;
        }
    }

    public boolean isInner(@NotNull BlockPos pos) {
        return pos.getX() >= minX &&
            pos.getY() >= minY &&
            pos.getZ() >= minZ &&
            pos.getX() <= maxX &&
            pos.getY() <= maxY &&
            pos.getZ() <= maxZ;
    }

    @Override
    public boolean isOwner(ServerCommandSource source) {
        if (source.hasPermissionLevel(4)) {
            return true;
        }
        if (father != null && father.isOwner(source)) {
            return true;
        }
        if (source.getEntity() instanceof ServerPlayerEntity) {
            return source.getEntity() .getUuid().equals(owner);
        } else {
            return false;
        }
    }

    @Override
    public boolean isOwnerOrFatherAdmin(ServerCommandSource source) {
        if (source.hasPermissionLevel(4)) {
            return true;
        }
        if (father != null && father.isOwnerOrFatherAdmin(source)) {
            return true;
        }
        if (source.getEntity() instanceof ServerPlayerEntity) {
            return source.getEntity().getUuid().equals(owner) || hasPerm(source.getEntity().getUuid(), Permission.ADMIN);
        } else {
            return false;
        }
    }

    @Override
    public void setPermission(@Nullable ServerCommandSource source, @NotNull UUID uuid, @NotNull Permission perm, @Nullable Boolean value) throws PermissionTargetException {
        if (source != null && source.getEntity() instanceof ServerPlayerEntity && !hasPerm((ServerPlayerEntity) source.getEntity(), Permission.ADMIN)) {
            ServerMain.LOGGER.warn("Player " + source.getName() + " try to set permission of " + uuid + " in " + name + " without admin permission");
            ServerMain.LOGGER.warn("allowing, if you have any problem please report to the author");
        }
        ServerMain.LOGGER.info(Optional.ofNullable(source).map(ServerCommandSource::getName).orElse("<null>") +
            " set perm " + perm.getName() + " to " + value + " for " + uuid + " in " + getFullName());
        PermissionHolder.super.setPermission(source, uuid, perm, value);
        Instance.getAllEnclosures(world).markDirty();
    }

    public String getFullName() {
        if (father != null) {
            return father.getName() + "." + getName();
        } else {
            return getName();
        }
    }

    @Override
    public String getSetPermissionCommand(UUID uuid) {
        return "/enclosure set " + getFullName() + " uuid " + uuid + " ";
    }

    public Vec3i distanceTo(BlockPos pos) {
        if (isInner(pos)) {
            return Vec3i.ZERO;
        }
        int x = 0, y = 0, z = 0;
        if (pos.getX() < minX) {
            x = minX - pos.getX();
        } else if (pos.getX() > maxX) {
            x = pos.getX() - maxX;
        }
        if (pos.getY() < minY) {
            y = minY - pos.getY();
        } else if (pos.getY() > maxY) {
            y = pos.getY() - maxY;
        }
        if (pos.getZ() < minZ) {
            z = minZ - pos.getZ();
        } else if (pos.getZ() > maxZ) {
            z = pos.getZ() - maxZ;
        }
        return new Vec3i(x, y, z);
    }

    @Override
    public MutableText serialize(@NotNull SerializationSettings settings, @Nullable ServerPlayerEntity player) {
        switch (settings) {
            case Name:
                return new LiteralText(getFullName());
            case Hover: {
                return TrT.of("enclosure.message.select.from")
                    .append(new LiteralText("[").formatted(Formatting.DARK_GREEN))
                    .append(new LiteralText(String.valueOf(minX)).formatted(Formatting.GREEN))
                    .append(new LiteralText(", ").formatted(Formatting.DARK_GREEN))
                    .append(new LiteralText(String.valueOf(minY)).formatted(Formatting.GREEN))
                    .append(new LiteralText(", ").formatted(Formatting.DARK_GREEN))
                    .append(new LiteralText(String.valueOf(minZ)).formatted(Formatting.GREEN))
                    .append(new LiteralText("]").formatted(Formatting.DARK_GREEN))
                    .append(TrT.of("enclosure.message.select.to"))
                    .append(new LiteralText("[").formatted(Formatting.DARK_GREEN))
                    .append(new LiteralText(String.valueOf(maxX)).formatted(Formatting.GREEN))
                    .append(new LiteralText(", ").formatted(Formatting.DARK_GREEN))
                    .append(new LiteralText(String.valueOf(maxY)).formatted(Formatting.GREEN))
                    .append(new LiteralText(", ").formatted(Formatting.DARK_GREEN))
                    .append(new LiteralText(String.valueOf(maxZ)).formatted(Formatting.GREEN))
                    .append(new LiteralText("]").formatted(Formatting.DARK_GREEN))
                    .append(TrT.of("enclosure.message.select.world"))
                    .append(new LiteralText(world.getRegistryKey().getValue().toString()).formatted(Formatting.GREEN))
                    .append("\n")
                    .append(TrT.of("enclosure.info.created_on"))
                    .append(new LiteralText(new SimpleDateFormat().format(new Date(createdOn))).formatted(Formatting.GOLD));
            }
            case Summarize: {
                MutableText text = serialize(SerializationSettings.Name, player);
                text.setStyle(Style.EMPTY.withColor(Formatting.GOLD)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/enclosure tp " + getFullName()))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, serialize(SerializationSettings.Hover, player))));

                text.append(TrT.of("enclosure.info.created_by").styled(style -> style.withColor(Formatting.WHITE)));
                String ownerName = Utils.getNameByUUID(owner);
                text.append((ownerName == null
                    ? TrT.of("enclosure.message.unknown_user").styled(style -> style.withColor(Formatting.RED))
                    : new LiteralText(ownerName).formatted(Formatting.GOLD))
                    .styled(style -> style.withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new LiteralText("UUID: " + owner))
                    )));
                return text;
            }
            case Full: {
                MutableText text = (MutableText) MutableText.EMPTY;

                if (father != null) {
                    text.append(TrT.of("enclosure.info.father_land").styled(style -> style.withColor(Formatting.WHITE)));
                    text.append(father.serialize(SerializationSettings.Name, player).styled(
                        style -> style.withColor(Formatting.GOLD)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, father.serialize(SerializationSettings.Hover, player)))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/enclosure info " + father.getFullName()))));
                    text.append("\n");
                }

                text.append(serialize(SerializationSettings.Summarize, player))
                    .append("\n")
                    .append(PermissionHolder.super.serialize(SerializationSettings.Full, player));
                return text;
            }
            case BarredFull: {
                final MutableText bar = new LiteralText("------------------------------")
                    .styled(style -> style.withColor(Formatting.YELLOW).withBold(true));
                return Text.empty()
                    .append(bar)
                    .append("\n")
                    .append(serialize(SerializationSettings.Full, player))
                    .append("\n")
                    .append(bar);
            }
            default:
                throw new RuntimeException("Unknown serialization settings: " + settings);
        }
    }

    public ServerWorld getWorld() {
        return this.world;
    }

    public void setWorld(ServerWorld world) {
        if (world == this.world) {
            return;
        }
        if (this.world != null) {
            markDirty();
        }
        this.world = world;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
        markDirty();
    }

    public int getMinX() {
        return this.minX;
    }

    public void setMinX(int minX) {
        this.minX = minX;
        markDirty();
    }

    public int getMinY() {
        return this.minY;
    }

    public void setMinY(int minY) {
        this.minY = minY;
        markDirty();
    }

    @Override
    public void markDirty() {
        Instance.getAllEnclosures(world).markDirty();
    }

    public int getMinZ() {
        return this.minZ;
    }

    public void setMinZ(int minZ) {
        this.minZ = minZ;
        markDirty();
    }

    public int getMaxX() {
        return this.maxX;
    }

    public void setMaxX(int maxX) {
        this.maxX = maxX;
        markDirty();
    }

    public int getMaxY() {
        return this.maxY;
    }

    public void setMaxY(int maxY) {
        this.maxY = maxY;
        markDirty();
    }

    public int getMaxZ() {
        return this.maxZ;
    }

    public void setMaxZ(int maxZ) {
        this.maxZ = maxZ;
        markDirty();
    }

    public UUID getOwner() {
        return this.owner;
    }

    public void setOwner(UUID owner) {
        permissionsMap.remove(this.owner);
        this.owner = owner;
        markDirty();
    }

    public Vec3d getTeleportPos() {
        return this.teleportPos;
    }

    public void setTeleportPos(Vec3d teleportPos, float yaw, float pitch) {
        this.teleportPos = teleportPos;
        this.yaw = yaw;
        this.pitch = pitch;
        markDirty();
    }

    public @NotNull String getEnterMessage() {
        return this.enterMessage;
    }

    public void setEnterMessage(@NotNull String enterMessage) {
        this.enterMessage = enterMessage;
        markDirty();
    }

    public @NotNull String getLeaveMessage() {
        return this.leaveMessage;
    }

    public void setLeaveMessage(@NotNull String leaveMessage) {
        this.leaveMessage = leaveMessage;
        markDirty();
    }

    public PermissionHolder getFather() {
        return this.father;
    }

    @Override
    public Map<UUID, Map<String, Boolean>> getPermissionsMap() {
        return this.permissionsMap;
    }

    /**
     * 自动化，封装好了，只需要调用一次
     *
     * @param father 权限继承父节点
     */
    public void setFather(@Nullable PermissionHolder father) {
        if (father instanceof Enclosure enclosure && this.world != enclosure.world) {
            throw new IllegalArgumentException("father must be in the same world");
        }
        if (this.father != null) {
            this.father.onRemoveChild(this);
        }
        this.father = father;
        if (father != null) {
            this.father.onAddChild(this);
        }
        Instance.getAllEnclosures(world).markDirty();
    }

    public long getCreatedOn() {
        return this.createdOn;
    }

    public void teleport(ServerPlayerEntity player) {
        player.teleport(world, teleportPos.x, teleportPos.y, teleportPos.z, yaw, pitch);
    }

    @Override
    public void onRemoveChild(PermissionHolder child) {
        throw new IllegalStateException("Enclosure area cannot have child");
    }

    @Override
    public void onAddChild(PermissionHolder child) {
        throw new IllegalStateException("Enclosure area cannot have child");
    }

    @Override
    public boolean inheritPermission() {
        return false;
    }

    public NbtCompound saveTerrain() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("name", getFullName());
        nbt.putInt("version", 1);
        nbt.put("enclosure", this.writeNbt(new NbtCompound()));
        NbtList blocks = new NbtList();
        for (int i = 0; i <= maxX - minX; i++) {
            NbtList yzList = new NbtList();
            for (int j = 0; j <= maxY - minY; j++) {
                NbtList zList = new NbtList();
                for (int k = 0; k <= maxZ - minZ; k++) {
                    NbtCompound block = new NbtCompound();
                    BlockPos pos = new BlockPos(minX + i, minY + j, minZ + k);
                    BlockState state = world.getBlockState(pos);
                    // air is represented by empty compound
                    if (!state.isAir()) {
                        block.put("state", NbtHelper.fromBlockState(state));
                        if (world.getWorldChunk(pos).getBlockEntities().containsKey(pos)) {
                            BlockEntity blockEntity = world.getBlockEntity(pos);
                            if (blockEntity != null) {
                                block.put("blockEntity", blockEntity.toInitialChunkDataNbt());
                            }
                        }
                    }
                    zList.add(block);
                }
                yzList.add(zList);
            }
            blocks.add(yzList);
        }
        nbt.put("blocks", blocks);
        NbtList entities = new NbtList();
        world.getEntitiesByClass(
            Entity.class,
            new Box(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1),
            entity -> !(entity instanceof PlayerEntity)
        ).forEach(entity -> {
            NbtCompound entityNbt = new NbtCompound();
            if (entity.saveSelfNbt(entityNbt)) {
                entities.add(entityNbt);
            }
        });
        nbt.put("entities", entities);
        return nbt;
    }

    public void rollback(@NotNull NbtCompound nbt) {
        if (!nbt.contains("enclosure") || !nbt.contains("blocks") || !nbt.contains("entities")) {
            throw new IllegalArgumentException("nbt id not enclosure backup");
        }
        Enclosure enclosure = new Enclosure(nbt.getCompound("enclosure"));
        if (!enclosure.name.equals(name)) {
            throw new IllegalArgumentException("nbt is not for this enclosure");
        }
        this.minX = enclosure.minX;
        this.minY = enclosure.minY;
        this.minZ = enclosure.minZ;
        this.maxX = enclosure.maxX;
        this.maxY = enclosure.maxY;
        this.maxZ = enclosure.maxZ;
        this.owner = enclosure.owner;
        this.teleportPos = enclosure.teleportPos;
        this.yaw = enclosure.yaw;
        this.pitch = enclosure.pitch;
        this.enterMessage = enclosure.enterMessage;
        this.leaveMessage = enclosure.leaveMessage;
        this.permissionsMap = enclosure.permissionsMap;
        this.createdOn = enclosure.createdOn;

        Profiler profiler = world.getProfiler();
        profiler.push("enclosureRollbackProcess");
        Box box = new Box(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
        world.getEntitiesByClass(ServerPlayerEntity.class, box, player -> true)
            .forEach(player -> {
                player.sendMessage(TrT.of("enclosure.message.kick.rollback"),false);
                this.kickPlayer(player);
            });
        profiler.push("block");
        NbtList blocks = nbt.getList("blocks", NbtTypes.LIST_TYPE);
        for (int i = 0; i <= maxX - minX; i++) {
            NbtList yzList = blocks.getList(i);
            for (int j = 0; j <= maxY - minY; j++) {
                NbtList zList = yzList.getList(j);
                for (int k = 0; k <= maxZ - minZ; k++) {
                    NbtCompound block = zList.getCompound(k);
                    BlockPos pos = new BlockPos(minX + i, minY + j, minZ + k);
                    if (world.getWorldBorder().contains(pos) && pos.getY() >= 0 && pos.getY() < world.getTopY()) {
                        if (block.isEmpty()) {
                            world.setBlockState(pos, Blocks.AIR.getDefaultState());
                        } else {
                            world.setBlockState(pos, NbtHelper.toBlockState(Registry..BLOCK.getReadOnlyWrapper(), block.getCompound("state")),
                                // don't update neighbors & light
                                Block.NOTIFY_LISTENERS & Block.SKIP_LIGHTING_UPDATES);
                            if (block.contains("blockEntity")) {
                                BlockEntity blockEntity = world.getBlockEntity(pos);
                                if (blockEntity != null) {
                                    profiler.push("rollbackBlockEntity");
                                    blockEntity.readNbt(block.getCompound("blockEntity"));
                                    profiler.pop();
                                }
                            }
                        }
                        world.getChunkManager().markForUpdate(pos);
                    }
                }
            }
        }
        profiler.swap("updateLighting");
        for (int i = 0; i <= maxX - minX; i++) {
            for (int k = 0; k <= maxZ - minZ; k++) {
                BlockPos pos = new BlockPos(minX + i, maxY, minZ + k);
                world.getChunkManager().getLightingProvider().checkBlock(pos);
            }
        }
        profiler.swap("rollbackEntities");
        world.getEntitiesByClass(Entity.class, box, entity -> !(entity instanceof PlayerEntity))
            .forEach(Entity::remove);
        NbtList entities = nbt.getList("entities", NbtElement.COMPOUND_TYPE);
        for (NbtElement entityNbt : entities) {
            EntityType.loadEntityWithPassengers((NbtCompound) entityNbt, world, entity -> {
                world.spawnEntity(entity);
                return entity;
            });
        }
        profiler.pop();
        markDirty();
        profiler.pop();
    }
}
