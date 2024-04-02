package com.github.zly2006.enclosure.gui

import com.github.zly2006.enclosure.Enclosure
import com.github.zly2006.enclosure.EnclosureArea
import com.github.zly2006.enclosure.EnclosureView
import com.github.zly2006.enclosure.EnclosureView.Companion.readonly
import com.github.zly2006.enclosure.utils.Serializable2Text
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType.ExtendedFactory
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.PacketByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.screen.ScreenHandler
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class EnclosureScreenHandler private constructor(
    syncId: Int,
    @JvmField val area: EnclosureView.ReadOnly,
    @JvmField val fullName: String,
    @JvmField val fatherFullName: String,
    @JvmField val worldId: Identifier,
    @JvmField val subAreaNames: List<String>
) : ScreenHandler(
    ENCLOSURE_SCREEN_HANDLER, syncId
) {
    class Data(
        val fullName: String,
        val fatherFullName: String,
        val worldId: Identifier,
        val compound: NbtCompound,
        val subAreaNames: List<String>
    )

    override fun quickMove(player: PlayerEntity, slot: Int): ItemStack {
        return Items.AIR.defaultStack
    }

    override fun canUse(player: PlayerEntity): Boolean {
        return true
    }

    companion object {
        @JvmField
        val ENCLOSURE_SCREEN_ID: Identifier = Identifier("enclosure", "screen.enclosure")
        @JvmField
        val ENCLOSURE_SCREEN_HANDLER: ExtendedScreenHandlerType<EnclosureScreenHandler, Data> =
            ExtendedScreenHandlerType(
                ExtendedFactory<EnclosureScreenHandler, Data> { syncId: Int, inventory: PlayerInventory?, buf: Data ->
                    EnclosureScreenHandler(
                        syncId,
                        readonly(buf.compound),
                        buf.fullName,
                        buf.fatherFullName,
                        buf.worldId,
                        buf.subAreaNames
                    )
                }, PacketCodec.ofStatic(
                    { buf: PacketByteBuf, data: Data ->
                        buf.writeString(data.fullName)
                        buf.writeString(data.fatherFullName)
                        buf.writeIdentifier(data.worldId)
                        buf.writeNbt(data.compound)
                        buf.writeVarInt(data.subAreaNames.size)
                        for (name in data.subAreaNames) {
                            buf.writeString(name)
                        }
                    },
                    { buf: PacketByteBuf ->
                        Data(
                            buf.readString(),
                            buf.readString(),
                            buf.readIdentifier(),
                            buf.readNbt()!!,
                            (0 until buf.readVarInt()).map { buf.readString() }
                        )
                    }

                ))

        fun register() {
            Registry.register(Registries.SCREEN_HANDLER, ENCLOSURE_SCREEN_ID, ENCLOSURE_SCREEN_HANDLER)
        }

        fun open(player: ServerPlayerEntity, area: EnclosureArea) {
            player.openHandledScreen(object : ExtendedScreenHandlerFactory<Any?> {
                override fun getDisplayName(): Text {
                    return area.serialize(Serializable2Text.SerializationSettings.Name, player)
                }

                override fun getScreenOpeningData(player: ServerPlayerEntity?): Data {
                    return Data(
                        area.fullName,
                        area.father?.fullName ?: "",
                        area.world.registryKey.value,
                        NbtCompound().apply { area.writeNbt(this, null) },
                        if (area is Enclosure) area.subEnclosures.areas.map { it.name } else emptyList()
                    )
                }

                override fun createMenu(syncId: Int, inv: PlayerInventory, player: PlayerEntity): ScreenHandler? {
                    return ENCLOSURE_SCREEN_HANDLER.create(syncId, inv, getScreenOpeningData(null))
                }
            })
        }
    }
}
