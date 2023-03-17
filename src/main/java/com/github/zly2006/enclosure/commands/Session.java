package com.github.zly2006.enclosure.commands;

import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.EnclosureList;
import com.github.zly2006.enclosure.config.LandLimits;
import com.github.zly2006.enclosure.events.PaidPartEvents;
import com.github.zly2006.enclosure.network.EnclosureInstalledC2SPacket;
import com.github.zly2006.enclosure.utils.TrT;
import com.github.zly2006.enclosure.utils.Utils;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static com.github.zly2006.enclosure.commands.EnclosureCommand.CONSOLE;

public class Session {
    UUID owner = new UUID(0, 0);
    ServerWorld world;
    BlockPos pos1;
    BlockPos pos2;

    public void trySync() {
        if (owner.equals(CONSOLE) || world == null || pos1 == null || pos2 == null) {
            return;
        }
        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(owner);
        if (player == null) {
            return;
        }
        if (EnclosureInstalledC2SPacket.isInstalled(player)) {
            PaidPartEvents.INSTANCE.syncSession(player);
        }
    }

    public ServerWorld getWorld() {
        return this.world;
    }

    public Session setWorld(ServerWorld world) {
        this.world = world;
        return this;
    }

    public BlockPos getPos1() {
        return this.pos1;
    }

    public Session setPos1(BlockPos pos1) {
        this.pos1 = pos1;
        return this;
    }

    public BlockPos getPos2() {
        return this.pos2;
    }

    public Session setPos2(BlockPos pos2) {
        this.pos2 = pos2;
        return this;
    }

    public UUID getOwner() {
        return this.owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public void reset(ServerWorld serverWorld) {
        world = serverWorld;
        pos1 = world.getSpawnPos();
        pos2 = world.getSpawnPos();
    }

    public void sync(@NotNull ServerPlayerEntity player) {
        if (world != player.getWorld()) {
            reset(player.getWorld());
        }
    }

    public int size() {
        return (Math.abs(pos1.getX() - pos2.getX()) + 1) *
                (Math.abs(pos1.getY() - pos2.getY()) + 1) *
                (Math.abs(pos1.getZ() - pos2.getZ()) + 1);
    }

    public EnclosureArea intersect(@NotNull EnclosureList list2check) {
        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());
        for (EnclosureArea area : list2check.getAreas()) {
            if (area.intersect(minX, minY, minZ, maxX, maxY, maxZ)) {
                return area;
            }
        }
        return null;
    }

    private <T extends Comparable<T>> @Nullable Text singleCheck(T value, T limit, boolean lessThan, String name) {
        int r = value.compareTo(limit);
        if (r < 0 && lessThan) {
            return null;
        }
        if (r == 0) {
            return null;
        }
        if (r > 0 && !lessThan) {
            return null;
        }
        if (lessThan) {
            return TrT.of("enclosure.limit." + Utils.camelCaseToSnakeCase(name))
                    .append(TrT.of("enclosure.message.limit_exceeded.0"))
                    .append(String.valueOf(limit))
                    .append(TrT.of("enclosure.message.limit_exceeded.1"))
                    .append(Text.literal(String.valueOf(value)));
        } else {
            return TrT.of("enclosure.limit." + Utils.camelCaseToSnakeCase(name))
                    .append(TrT.of("enclosure.message.limit_exceeded.2"))
                    .append(String.valueOf(limit))
                    .append(TrT.of("enclosure.message.limit_exceeded.1"))
                    .append(Text.literal(String.valueOf(value)));
        }
    }

    public @Nullable Text isValid(LandLimits limits) {
        int minX = Math.min(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());

        Text text = singleCheck(maxX - minX + 1, limits.maxXRange, true, "maxXRange");
        if (text != null) {
            return text;
        }
        text = singleCheck(maxZ - minZ + 1, limits.maxZRange, true, "maxZRange");
        if (text != null) {
            return text;
        }
        text = singleCheck(maxY - minY + 1, limits.maxHeight, true, "maxHeight");
        if (text != null) {
            return text;
        }
        text = singleCheck(maxX - minX + 1, limits.minXRange, false, "minXRange");
        if (text != null) {
            return text;
        }
        text = singleCheck(maxZ - minZ + 1, limits.minZRange, false, "minZRange");
        if (text != null) {
            return text;
        }
        text = singleCheck(minY, limits.minY, false, "minY");
        if (text != null) {
            return text;
        }
        text = singleCheck(maxY, limits.maxY, true, "maxY");
        return text;
    }

    public void shrink(Direction direction, int amount) {
        BlockPos p1 = new BlockPos(Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()));
        BlockPos p2 = new BlockPos(Math.max(pos1.getX(), pos2.getX()), Math.max(pos1.getY(), pos2.getY()), Math.max(pos1.getZ(), pos2.getZ()));
        pos1 = p1;
        pos2 = p2;
        switch (direction) {
            case NORTH -> pos1 = pos1.add(0, 0, amount);
            case SOUTH -> pos2 = pos2.add(0, 0, -amount);
            case WEST -> pos1 = pos1.add(amount, 0, 0);
            case EAST -> pos2 = pos2.add(-amount, 0, 0);
            case UP -> pos2 = pos2.add(0, -amount, 0);
            case DOWN -> pos1 = pos1.add(0, amount, 0);
        }
    }

    public void expand(Direction direction, int amount) {
        shrink(direction, -amount);
    }

    public void shift(Direction direction, int amount) {
        pos1 = pos1.offset(direction, amount);
        pos2 = pos2.offset(direction, amount);
    }
}
