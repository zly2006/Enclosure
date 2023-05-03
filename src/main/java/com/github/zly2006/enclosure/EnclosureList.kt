package com.github.zly2006.enclosure

import com.github.zly2006.enclosure.ServerMain.enclosures
import com.github.zly2006.enclosure.ServerMain.getAllEnclosures
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.world.PersistentState
import java.io.File

class EnclosureList(world: ServerWorld, isRoot: Boolean) : PersistentState() {
    private val areaMap: MutableMap<String, EnclosureArea> = HashMap()
    private val boundWorld: ServerWorld?
    val areas = areaMap.values

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

    override fun writeNbt(nbt: NbtCompound): NbtCompound {
        val list = NbtList()
        for (area in areaMap.values) {
            list.add(NbtString.of(area.name))
            val compound = NbtCompound()
            area.writeNbt(compound)
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
            if (enclosureArea.isInner(pos)) {
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
            areaMap.remove(name)
            markDirty()
            return true
        }
        return false
    }

    fun addArea(area: EnclosureArea) {
        areaMap[area.name] = area
        markDirty()
    }

    override fun save(file: File) {
        super.save(file)
    }
}

const val DATA_VERSION_KEY = "data_version"
const val SUB_ENCLOSURES_KEY = "sub_lands"
const val ENCLOSURE_LIST_KEY = "enclosure.land_list"
const val ENCLOSURE_PREFIX = "enclosure:"