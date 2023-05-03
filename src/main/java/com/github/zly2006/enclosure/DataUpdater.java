package com.github.zly2006.enclosure;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtList;

import java.util.HashMap;
import java.util.Map;

public class DataUpdater {
    private interface Updater {
        NbtCompound update(NbtCompound data);
    }
    static final Map<Integer, Updater> updaters = new HashMap<>() {
        private NbtCompound update1(NbtCompound compound) {
            if (compound.contains("tp_pos") && !compound.contains("yaw")) {
                NbtList nbtList = (NbtList) compound.get("tp_pos");
                assert nbtList != null;
                int x = nbtList.getInt(0);
                int y = nbtList.getInt(1);
                int z = nbtList.getInt(2);
                nbtList.clear();
                nbtList.add(NbtDouble.of(x));
                nbtList.add(NbtDouble.of(y));
                nbtList.add(NbtDouble.of(z));
                compound.put("tp_pos", nbtList);
                compound.put("yaw", NbtFloat.of(0));
                compound.put("pitch", NbtFloat.of(0));
            }
            return compound;
        }

        {
        put(1, this::update1);
    }};
    public static NbtCompound update(int versionBefore, NbtCompound compound) {
        if (versionBefore == 0) {
            return compound;
        }
        else if (updaters.get(versionBefore) != null) {
            Updater updater = updaters.get(versionBefore);
            compound.getKeys().forEach(key -> {
                if (compound.get(key) instanceof NbtCompound nbt) {
                    compound.put(key, updater.update(nbt));
                    if (compound.getCompound(key).contains(EnclosureListKt.SUB_ENCLOSURES_KEY)) {
                        NbtCompound update = update(versionBefore, compound.getCompound(key));
                        nbt.put(EnclosureListKt.SUB_ENCLOSURES_KEY, update);
                    }
                }
            });
            return compound;
        }
        else {
            throw new Error("Enclosure cannot update this version of data: " + versionBefore);
        }
    }
}
