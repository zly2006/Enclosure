package com.github.zly2006.enclosure.gui

import com.github.zly2006.enclosure.Enclosure
import com.github.zly2006.enclosure.EnclosureArea
import com.github.zly2006.enclosure.ReadOnlyEnclosureArea
import com.github.zly2006.enclosure.ReadOnlyEnclosureArea.Companion.fromTag
import com.github.zly2006.enclosure.utils.Serializable2Text
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

class EnclosureScreenHandler private constructor(
    syncId: Int,
    val area: ReadOnlyEnclosureArea,
    val fullName: String,
    val fatherFullName: String,
    val worldId: Identifier,
    val subAreaNames: List<String>
) : ScreenHandler(ENCLOSURE_SCREEN_HANDLER, syncId) {
    override fun canUse(player: PlayerEntity) = true

    companion object {
        @JvmField val ENCLOSURE_SCREEN_ID = Identifier("enclosure", "screen.enclosure")
        @JvmField val ENCLOSURE_SCREEN_HANDLER =
            ExtendedScreenHandlerType { syncId: Int, inventory: PlayerInventory?, buf: PacketByteBuf ->
                val fullName = buf.readString()
                val fatherFullName = buf.readString()
                val worldId = buf.readIdentifier()
                val compound = buf.readNbt()!!
                val area = fromTag(compound)
                val subAreaNames: MutableList<String> = ArrayList()
                val size = buf.readVarInt()
                for (i in 0 until size) {
                    subAreaNames.add(buf.readString())
                }
                EnclosureScreenHandler(syncId, area, fullName, fatherFullName, worldId, subAreaNames)
            }

        @JvmStatic
        fun register() {
            Registry.register(Registry.SCREEN_HANDLER, ENCLOSURE_SCREEN_ID, ENCLOSURE_SCREEN_HANDLER)
        }

        @JvmStatic
        fun open(player: ServerPlayerEntity, area: EnclosureArea) {
            player.openHandledScreen(object : ExtendedScreenHandlerFactory {
                override fun writeScreenOpeningData(player: ServerPlayerEntity?, buf: PacketByteBuf) {
                    buf.writeString(area.fullName)
                    buf.writeString(
                        if (area.father is Enclosure)
                            (area.father as Enclosure).fullName
                        else if (area.father != null) "$" + area.father!!.fullName
                        else ""
                    )
                    buf.writeIdentifier(area.world.registryKey.value)
                    val compound = NbtCompound()
                    area.writeNbt(compound)
                    buf.writeNbt(compound)
                    if (area is Enclosure) {
                        buf.writeVarInt(area.subEnclosures.areas.size)
                        for (subArea in area.subEnclosures.areas) {
                            buf.writeString(subArea.name)
                        }
                    } else {
                        buf.writeVarInt(0)
                    }
                }

                override fun getDisplayName(): Text {
                    return area.serialize(Serializable2Text.SerializationSettings.Name, player)
                }

                override fun createMenu(syncId: Int, inv: PlayerInventory, player: PlayerEntity): ScreenHandler? {
                    val buf = PacketByteBufs.create()
                    writeScreenOpeningData(null, buf)
                    return ENCLOSURE_SCREEN_HANDLER
                        .create(syncId, inv, buf)
                }
            })
        }
    }
}
