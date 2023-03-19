package com.github.zly2006.enclosure.backup.executors;

import com.github.zly2006.enclosure.Enclosure;
import com.github.zly2006.enclosure.EnclosureArea;
import com.github.zly2006.enclosure.backup.BackupExecutor;
import com.github.zly2006.enclosure.backup.StreamDataInput;
import com.github.zly2006.enclosure.backup.StreamDataOutput;
import com.github.zly2006.enclosure.utils.TrT;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class BEv1 implements BackupExecutor {
    boolean blockFinished = false;
    final boolean isRollback;
    @Nullable
    final private StreamDataInput dataInput;
    @NotNull
    final private EnclosureArea area;
    final ServerWorld world;
    final Profiler profiler;
    @Nullable
    final private StreamDataOutput dataOutput;
    int x;
    int y;
    int z;
    int entitiesCount = -1;
    int remainingEntities = -1;
    Box box;
    public boolean finished = false;

    public BEv1(StreamDataInput dataInput, EnclosureArea area) throws IOException {
        this.dataInput = dataInput;
        this.dataOutput = null;
        this.area = area;
        this.isRollback = true;
        this.world = area.getWorld();
        this.profiler = world.getProfiler();
        NbtCompound nbt = dataInput.readNbt();
        if (!nbt.contains("enclosure") || !nbt.contains("blocks") || !nbt.contains("entities")) {
            throw new IllegalArgumentException("nbt id not enclosure backup");
        }
        int version = nbt.getInt("version"); // unused
        // read enclosure data
        Enclosure enclosure = new Enclosure(nbt.getCompound("enclosure"));
        if (!enclosure.getName().equals(area.getName())) { // name is not same
            throw new IllegalArgumentException("nbt is not for this enclosure");
        }
        area.mirrorFrom(enclosure);
        area.lock();
        x = area.getMinX();
        y = area.getMinY();
        z = area.getMinZ();
        box = new Box(area.getMinX(), area.getMinY(), area.getMinZ(), area.getMaxX() + 1, area.getMaxY() + 1, area.getMaxZ() + 1);
        world.getEntitiesByClass(ServerPlayerEntity.class, box, player -> true)
            .forEach(player -> {
                player.sendMessage(TrT.of("enclosure.message.kick.rollback"));
                area.kickPlayer(player);
            });
        world.getEntitiesByClass(Entity.class, box, entity -> !(entity instanceof PlayerEntity))
            .forEach(Entity::discard);
    }

    public BEv1(StreamDataOutput dataOutput, EnclosureArea area) throws IOException {
        this.dataOutput = dataOutput;
        this.dataInput = null;
        this.area = area;
        this.isRollback = false;
        this.world = area.getWorld();
        this.profiler = world.getProfiler();
        NbtCompound compound = new NbtCompound();
        area.writeNbt(compound);
        compound.putInt("version", 1);
        dataOutput.writeNbt(compound);
        area.lock();
        x = area.getMinX();
        y = area.getMinY();
        z = area.getMinZ();
        box = new Box(area.getMinX(), area.getMinY(), area.getMinZ(), area.getMaxX() + 1, area.getMaxY() + 1, area.getMaxZ() + 1);
    }

    @Override
    public void doBackup(int steps) throws IOException {
        for (int i = 0; i < steps; i++) {
            doBackup();
        }
    }

    public void doBackup() throws IOException {
        assert dataOutput != null;
        if (!blockFinished) {
            NbtCompound block = new NbtCompound();
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = world.getBlockState(pos);
            // air is represented by empty compound
            if (!state.isAir()) {
                block.put("state", NbtHelper.fromBlockState(state));
                if (world.getWorldChunk(pos).getBlockEntities().containsKey(pos)) {
                    BlockEntity blockEntity = world.getBlockEntity(pos);
                    if (blockEntity != null) {
                        block.put("blockEntity", blockEntity.createNbt());
                    }
                }
            }
            dataOutput.writeNbt(block);
        }
        else {
            List<NbtCompound> entities = world.getEntitiesByClass(Entity.class, box, entity -> !(entity instanceof PlayerEntity))
                .stream().map(entity -> {
                    NbtCompound entityNbt = new NbtCompound();
                    if (entity.saveSelfNbt(entityNbt)) {
                        return entityNbt;
                    }
                    return null;
                }).filter(Objects::nonNull).toList();
            dataOutput.writeInt(entities.size());
            for (NbtCompound entityNbt : entities) {
                dataOutput.writeNbt(entityNbt);
            }
        }
    }

    @Override
    public void doRollback(int steps) throws IOException {
        for (int i = 0; i < steps; i++) {
            doRollback();
        }
    }

    public void doRollback() throws IOException {
        assert dataInput != null;
        if (!blockFinished) {
            NbtCompound block = dataInput.readNbt();
            BlockPos pos = new BlockPos(x, y, z);
            if (world.getWorldBorder().contains(pos) && pos.getY() >= world.getBottomY() && pos.getY() < world.getTopY()) {
                if (block.isEmpty()) {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());
                } else {
                    world.setBlockState(pos, NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), block.getCompound("state")),
                        // don't update neighbors & light
                        Block.NOTIFY_LISTENERS & Block.SKIP_LIGHTING_UPDATES);
                    if (block.contains("blockEntity")) {
                        BlockEntity blockEntity = world.getBlockEntity(pos);
                        if (blockEntity != null) {
                            blockEntity.readNbt(block.getCompound("blockEntity"));
                        }
                    }
                }
                world.getChunkManager().markForUpdate(pos);
            }
        }
        else {
            if (entitiesCount == -1) {
                // update lighting
                for (int i = 0; i <= area.getMaxX() - area.getMinX(); i++) {
                    for (int k = 0; k <= area.getMaxZ() - area.getMinZ(); k++) {
                        BlockPos pos = new BlockPos(area.getMinX() + i, area.getMaxY(), area.getMinZ() + k);
                        world.getChunkManager().getLightingProvider().checkBlock(pos);
                    }
                }
                entitiesCount = dataInput.readInt();
                remainingEntities = entitiesCount;
            }
            else {
                if (remainingEntities > 0) {
                    NbtCompound entityNbt = dataInput.readNbt();
                    EntityType.loadEntityWithPassengers(entityNbt, world, entity -> {
                        world.spawnEntity(entity);
                        return entity;
                    });
                    remainingEntities--;
                }
                else {
                    area.unlock();
                    dataInput.close();
                    finished = true;
                }
            }
        }
    }

    private void gotoNextBlock() throws IOException {
        z++;
        if (z > area.getMaxZ()) {
            z = area.getMinZ();
            y++;
        }
        if (y > area.getMaxY()) {
            y = area.getMinY();
            x++;
        }
        if (x > area.getMaxX()) {
            blockFinished = true;
        }
    }

    @Override
    public int totalSteps() {
        //todo
        return 0;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }
}
