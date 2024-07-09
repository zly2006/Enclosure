package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.Enclosure;
import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.access.ChunkAccess;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.BlendingData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(WorldChunk.class)
public abstract class MixinWorldChunk extends Chunk implements ChunkAccess {
    public MixinWorldChunk(ChunkPos pos, UpgradeData upgradeData, HeightLimitView heightLimitView, Registry<Biome> biomeRegistry, long inhabitedTime, @Nullable ChunkSection[] sectionArray, @Nullable BlendingData blendingData) {
        super(pos, upgradeData, heightLimitView, biomeRegistry, inhabitedTime, sectionArray, blendingData);
    }

    @Shadow public abstract World getWorld();

    @Unique @Nullable
    private List<Enclosure> cache = null;

    @NotNull
    @Override
    public List<Enclosure> enclosure$cache() {
        if (cache == null) {
            if (getWorld() instanceof ServerWorld serverWorld) {
                cache = (List) ServerMain.INSTANCE.getAllEnclosures(serverWorld).getAreas()
                        .stream()
                        .filter(enclosure -> enclosure.containsChunk(getPos()))
                        .collect(Collectors.toList());
            } else {
                cache = List.of();
            }
        }
        return cache;
    }

    @Override
    public void enclosure$putCache(@NotNull Enclosure enclosure) {
        if (!getWorld().isClient && enclosure.containsChunk(getPos())) {
            enclosure$cache().add(enclosure);
        }
    }

    @Override
    public void enclosure$removeCache(@NotNull Enclosure enclosure) {
        if (!getWorld().isClient) {
            enclosure$cache().remove(enclosure);
        }
    }
}
