package com.github.zly2006.enclosure.gui

import com.github.zly2006.enclosure.Enclosure
import com.github.zly2006.enclosure.EnclosureArea
import com.github.zly2006.enclosure.EnclosureView
import com.github.zly2006.enclosure.EnclosureView.Companion.readonly
import com.github.zly2006.enclosure.utils.Serializable2Text
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier

class EnclosureScreenHandler private constructor(
    syncId: Int,
    @JvmField val area: EnclosureView.ReadOnly,
    @JvmField val fullName: String,
    @JvmField val fatherFullName: String?,
    @JvmField val worldId: Identifier,
    @JvmField val subAreaNames: List<String>
) : ScreenHandler(ENCLOSURE_SCREEN_HANDLER, syncId) {
    class Data(
        var fullName: String,
        var fatherFullName: String?,
        var worldId: Identifier,
        var compound: NbtCompound,
        var subAreaNames: List<String>,
    )

    override fun quickMove(player: PlayerEntity, slot: Int): ItemStack {
        return ItemStack.EMPTY
    }

    override fun canUse(player: PlayerEntity): Boolean {
        return true
    }

    companion object {
        @JvmField
        val ENCLOSURE_SCREEN_ID: Identifier = Identifier.of("enclosure", "screen.enclosure")
        val ENCLOSURE_SCREEN_HANDLER = ExtendedScreenHandlerType(
            { syncId: Int, _: PlayerInventory?, data: Data ->
                val area: EnclosureView.ReadOnly = readonly(data.compound)
                EnclosureScreenHandler(
                    syncId,
                    area,
                    data.fullName,
                    data.fatherFullName,
                    data.worldId,
                    data.subAreaNames
                )
            }, PacketCodec.of(
                { value: Data, buf: RegistryByteBuf ->
                    buf.writeString(value.fullName)
                    buf.writeString(value.fatherFullName)
                    buf.writeIdentifier(value.worldId)
                    buf.writeNbt(value.compound)
                    buf.writeVarInt(value.subAreaNames.size)
                    for (subAreaName in value.subAreaNames) {
                        buf.writeString(subAreaName)
                    }
                }, { buf: RegistryByteBuf ->
                    Data(
                        buf.readString(),
                        buf.readString(),
                        buf.readIdentifier(),
                        buf.readNbt()!!,
                        buf.readList(PacketByteBuf::readString)
                    )
                })
        )

        fun register() {
            Registry.register(Registries.SCREEN_HANDLER, ENCLOSURE_SCREEN_ID, ENCLOSURE_SCREEN_HANDLER)
        }

        fun open(player: ServerPlayerEntity, area: EnclosureArea) {
            player.openHandledScreen(object : ExtendedScreenHandlerFactory<Data> {
                override fun getScreenOpeningData(player: ServerPlayerEntity) = Data(
                    area.fullName,
                    if (area.father is Enclosure) {
                        area.father!!.fullName
                    } else if (area.father != null) {
                        "$" + area.father!!.fullName
                    } else {
                        ""
                    },
                    area.world.registryKey.value,
                    area.writeNbt(NbtCompound(), area.world.registryManager),
                    if (area is Enclosure) {
                        area.subEnclosures.names
                    } else {
                        emptyList()
                    }
                )

                override fun getDisplayName() =
                    area.serialize(Serializable2Text.SerializationSettings.Name, player)

                override fun createMenu(syncId: Int, inv: PlayerInventory, player: PlayerEntity) =
                    ENCLOSURE_SCREEN_HANDLER.create(syncId, inv, getScreenOpeningData(player as ServerPlayerEntity))
            })
        }
    }
}
