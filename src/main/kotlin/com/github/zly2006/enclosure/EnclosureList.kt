package com.github.zly2006.enclosure

import com.github.zly2006.enclosure.ServerMain.enclosures
import com.github.zly2006.enclosure.ServerMain.getAllEnclosures
import com.github.zly2006.enclosure.access.ChunkAccess
import com.github.zly2006.enclosure.command.MAX_CHUNK_LEVEL
import com.github.zly2006.enclosure.utils.Serializable2Text.SerializationSettings.NameHover
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString
import net.minecraft.registry.RegistryWrapper
import net.minecraft.server.world.ChunkLevelType
import net.minecraft.server.world.ChunkLevels
import net.minecraft.server.world.ChunkTicketType
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.world.PersistentState
import net.minecraft.world.chunk.ChunkStatus

class EnclosureList(world: ServerWorld, private val isRoot: Boolean) : PersistentState() {
    private val areaMap: MutableMap<String, EnclosureArea> = mutableMapOf()
    private val boundWorld: ServerWorld?
    val areas = areaMap.values
    val names get() = areaMap.keys.toList()

    constructor(nbt: NbtCompound, world: ServerWorld, isRoot: Boolean) : this(world, isRoot) {
        (nbt[ENCLOSURE_LIST_KEY] as? NbtList)?.forEach {
            val name = (it as? NbtString)?.asString() ?: return@forEach
            val compound = nbt[name] as? NbtCompound ?: return@forEach
            if (compound.keys.contains(SUB_ENCLOSURES_KEY)) {
                areaMap[name] = Enclosure(compound, world)
            } else {
                areaMap[name] = EnclosureArea(compound, world)
            }
        }
    }

    init {
        boundWorld = world
        if (isRoot) {
            enclosures[world.registryKey] = this
            LOGGER.debug("Creating new enclosure list for world {}", world.registryKey.value)
            boundWorld.chunkManager.persistentStateManager[ENCLOSURE_LIST_KEY] = this
        }
    }

    override fun writeNbt(nbt: NbtCompound, registryLookup: RegistryWrapper.WrapperLookup?): NbtCompound {
        val list = NbtList()
        for (area in areaMap.values) {
            list.add(NbtString.of(area.name))
            val compound = NbtCompound()
            area.writeNbt(compound, registryLookup)
            nbt.put(area.name, compound)
        }
        nbt.put(ENCLOSURE_LIST_KEY, list)
        nbt.putInt(DATA_VERSION_KEY, DATA_VERSION)
        return nbt
    }

    /**
     * 只会查找到下一级，要获取最小的领地，请使用areaOf
     */
    fun getArea(pos: BlockPos): EnclosureArea? {
        for (enclosureArea in areaMap.values) {
            if (enclosureArea.contains(pos)) {
                return enclosureArea
            }
        }
        return null
    }

    override fun markDirty() {
        if (boundWorld != null) {
            val list = getAllEnclosures(boundWorld)
            if (list === this) {
                isDirty = true
            } else {
                list.markDirty()
            }
        }
    }

    fun remove(name: String): Boolean {
        if (areaMap.containsKey(name)) {
            val area = areaMap.remove(name)
            if (isRoot) {
                area!!.toBlockBox().streamChunkPos().forEach {
                    val chunk = boundWorld!!.getChunk(it.x, it.z, ChunkStatus.FULL, false);
                    if (chunk is ChunkAccess && area is Enclosure) {
                        chunk.removeCache(area)
                    }
                }
            }
            markDirty()
            return true
        }
        return false
    }

    fun addArea(area: EnclosureArea) {
        areaMap[area.name] = area
        if (isRoot) {
            area.toBlockBox().streamChunkPos().forEach {
                val chunk = boundWorld!!.getChunk(it.x, it.z, ChunkStatus.FULL, false);
                if (chunk is ChunkAccess && area is Enclosure) {
                    chunk.putCache(area)
                }
            }
        }
        markDirty()
    }

    fun tickTickets(world: ServerWorld) {
        areas.forEach { area ->
            if (area.ticket != null) {
                area.ticket!!.remainingTicks--
                if (area.ticket!!.remainingTicks <= 0) {
                    removeTicket(area, world)
                    area.ticket = null
                    world.server.playerManager.broadcast(
                        Text.literal("Force loading for ").append(area.serialize(NameHover, null)).append(" expired."), false
                    )
                    // todo: message
                }
                area.markDirty()
            }
        }
    }

    private fun removeTicket(area: EnclosureArea, world: ServerWorld) {
        area.toBlockBox().streamChunkPos().forEach {
            val chunk = world.getChunkAsView(it.x, it.z)
            if (chunk is ChunkAccess) {
                val targetLevel = chunk.cache
                    .filter { it.ticket != null && it != area }
                    .minOfOrNull { it.ticket!!.level }
                    ?: ChunkLevels.getLevelFromType(ChunkLevelType.FULL) // chunk border level
                if (targetLevel > area.ticket!!.level) {
                    world.chunkManager.removeTicket(ChunkTicketType.FORCED, it, MAX_CHUNK_LEVEL - area.ticket!!.level, it)
                }
            } else {
                world.chunkManager.removeTicket(ChunkTicketType.FORCED, it, MAX_CHUNK_LEVEL - area.ticket!!.level, it)
            }
        }
    }

    fun initTickets(world: ServerWorld) {
        areas.forEach { area ->
            if (area.ticket != null) {
                if (area.ticket!!.remainingTicks > 0) {
                    area.toBlockBox().streamChunkPos().forEach {
                        world.chunkManager.addTicket(ChunkTicketType.FORCED, it, MAX_CHUNK_LEVEL - area.ticket!!.level, it)
                    }
                    // todo: message
                }
            }
        }
    }
}

const val DATA_VERSION_KEY = "data_version"
const val SUB_ENCLOSURES_KEY = "sub_lands"
const val ENCLOSURE_LIST_KEY = "enclosure.land_list"
const val ENCLOSURE_PREFIX = "enclosure:"
