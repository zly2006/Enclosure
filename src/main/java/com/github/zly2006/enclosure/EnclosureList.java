package com.github.zly2006.enclosure;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.github.zly2006.enclosure.ServerMain.DATA_VERSION;
import static com.github.zly2006.enclosure.ServerMain.Instance;

public class EnclosureList extends PersistentState {
    public static final String DATA_VERSION_KEY = "data_version";
    static final String SUB_ENCLOSURES_KEY = "sub_lands";
    static final String ENCLOSURE_LIST_KEY = "enclosure.land_list";
    static final String ENCLOSURE_PREFIX = "enclosure:";
    Map<String, EnclosureArea> areas = new HashMap<>();
    ServerWorld boundWorld;

    public EnclosureList(NbtCompound nbt, ServerWorld world) {
        this(world);
        NbtElement nbtElement = nbt.get(ENCLOSURE_LIST_KEY);
        if (nbtElement instanceof NbtList list) {
            for (NbtElement element : list) {
                if (element instanceof NbtString nbtString) {
                    String name = nbtString.asString();
                    if (nbt.get(name) instanceof NbtCompound compound) {
                        if (compound.getKeys().contains(SUB_ENCLOSURES_KEY)) {
                            areas.put(name, new Enclosure(compound, world));
                        } else {
                            areas.put(name, new EnclosureArea(compound, world));
                        }
                    }
                }
            }
        } else {
            nbt.put(ENCLOSURE_LIST_KEY, new NbtList());
        }
    }

    public EnclosureList(ServerWorld world) {
        boundWorld = world;
        boundWorld.getChunkManager().getPersistentStateManager().set(
            ENCLOSURE_LIST_KEY,
            this
        );
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (EnclosureArea area : areas.values()) {
            list.add(NbtString.of(area.getName()));
            NbtCompound compound = new NbtCompound();
            area.writeNbt(compound);
            nbt.put(area.getName(), compound);
        }
        nbt.put(ENCLOSURE_LIST_KEY, list);
        nbt.putInt(DATA_VERSION_KEY, DATA_VERSION);
        return nbt;
    }

    /**
     * 只会查找到下一级，要获取最小的领地，请使用areaOf
     */
    @Nullable
    public EnclosureArea getArea(@NotNull BlockPos pos) {
        for (EnclosureArea enclosureArea : areas.values()) {
            if (enclosureArea.isInner(pos)) {
                return enclosureArea;
            }
        }
        return null;
    }

    @Override
    public void markDirty() {
        if (boundWorld != null) {
            EnclosureList list = Instance.getAllEnclosures(boundWorld);
            if (list == this) {
                setDirty(true);
            }
            else if (list != null) {
                list.markDirty();
            }
        }
    }

    public Collection<EnclosureArea> getAreas() {
        return this.areas.values();
    }

    public ServerWorld getBoundWorld() {
        return this.boundWorld;
    }

    public boolean remove(String name) {
        if (areas.containsKey(name)) {
            areas.remove(name);
            markDirty();
            return true;
        }
        return false;
    }

    public void addArea(EnclosureArea area) {
        areas.put(area.getName(), area);
        markDirty();
    }

    @Override
    public void save(File file) {
        super.save(file);
    }
}
