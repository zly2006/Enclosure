package com.github.zly2006.enclosure

import com.github.zly2006.enclosure.command.Session
import com.github.zly2006.enclosure.utils.Serializable2Text.SerializationSettings
import com.github.zly2006.enclosure.utils.TrT
import com.github.zly2006.enclosure.utils.clickRun
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.RegistryWrapper.WrapperLookup
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos

class Enclosure : EnclosureArea {
    @JvmField
    val subEnclosures: EnclosureList

    /**
     * Create an instance from nbt for a specific world.
     * @param compound the nbt compound tag
     */
    constructor(compound: NbtCompound, world: ServerWorld?) : super(compound, world!!) {
        // process sub enclosures
        val sub = compound.getCompound(SUB_ENCLOSURES_KEY)
        subEnclosures = EnclosureList(sub, world, false)
        subEnclosures.areas.forEach(this::addChild)
    }

    constructor(session: Session, name: String?) : super(session, name!!) {
        subEnclosures = EnclosureList(session.world, false)
    }

    override fun writeNbt(nbt: NbtCompound, registryLookup: WrapperLookup?): NbtCompound {
        val compound = super.writeNbt(nbt, registryLookup)
        val sub = NbtCompound()
        subEnclosures.writeNbt(sub, registryLookup)
        compound.put(SUB_ENCLOSURES_KEY, sub)
        return compound
    }

    override fun changeWorld(world: ServerWorld) {
        if (world === this.world) return
        super.world = world
    }

    override fun areaOf(pos: BlockPos): EnclosureArea {
        for (area in subEnclosures.areas) {
            if (area.contains(pos)) {
                return area.areaOf(pos)
            }
        }
        return super.areaOf(pos)
    }

    override fun serialize(settings: SerializationSettings, player: ServerPlayerEntity?): MutableText {
        if (settings == SerializationSettings.Full) {
            val text = super.serialize(settings, player)
            val subLandsText: MutableText = Text.empty()
            if (subEnclosures.areas.isNotEmpty()) {
                text.append("\n")
                for (area in subEnclosures.areas) {
                    subLandsText.append(area.serialize(SerializationSettings.NameHover, player).styled {
                        it.withColor(Formatting.GOLD)
                            .clickRun("/enclosure info ${area.fullName}")
                    })
                    subLandsText.append(" ")
                }
                text.append(TrT.of("enclosure.message.sub_lands", subLandsText))
            }
            return text
        } else {
            return super.serialize(settings, player)
        }
    }

    override fun onRemoveChild(child: PermissionHolder) {
        if (child is EnclosureArea) child.father = null
        subEnclosures.remove(child.name)
        markDirty()
    }

    override fun addChild(child: PermissionHolder) {
        if (child is EnclosureArea) {
            child.father = this
            subEnclosures.addArea(child)
            markDirty()
        } else {
            throw IllegalArgumentException("child must be an instance of EnclosureArea")
        }
    }
}
