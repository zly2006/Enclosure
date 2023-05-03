package com.github.zly2006.enclosure

import com.github.zly2006.enclosure.utils.Serializable2Text.SerializationSettings
import com.github.zly2006.enclosure.utils.Utils
import com.github.zly2006.enclosure.utils.hoverText
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.world.PersistentState
import java.util.*

class EnclosureGroup : PermissionHolder {
    override lateinit var name: String
    override lateinit var owner: UUID
    var enclosures: MutableSet<String> = HashSet()
    override var permissionsMap: MutableMap<UUID, MutableMap<String, Boolean>> = HashMap()

    override fun getSetPermissionCommand(uuid: UUID): String {
        return "/enclosure groups set $fullName uuid $uuid "
    }

    override fun serialize(settings: SerializationSettings, player: ServerPlayerEntity?): MutableText {
        return when (settings) {
            SerializationSettings.Name -> Text.literal(name)
            SerializationSettings.Summarize -> serialize(SerializationSettings.Name, player).hoverText(
                serialize(SerializationSettings.Hover, player)
            )

            SerializationSettings.Full -> {
                val text = Text.empty()
                for (enclosure in enclosures) {
                    text.append(
                        ServerMain.getEnclosure(enclosure)!!
                            .serialize(SerializationSettings.Summarize, player)
                    ).append(", ")
                }
                Text.literal("Group: ").append(serialize(SerializationSettings.Name, player))
                    .append("\nOwner: ").append(Utils.getDisplayNameByUUID(owner))
                    .append("\nEnclosures: ").append(text)
                    .append("\n").append(super.serialize(SerializationSettings.Full, player))
            }

            SerializationSettings.Hover -> Text.literal("Owner: ").append(Utils.getDisplayNameByUUID(owner))
            else -> Text.literal("Unknown serialization settings").formatted(Formatting.RED)
        }
    }

    override fun isOwner(source: ServerCommandSource): Boolean {
        return if (source.hasPermissionLevel(4)) {
            true
        } else if (source.player != null) {
            source.player!!.uuid == owner
        } else {
            false
        }
    }

    override fun isOwnerOrFatherAdmin(source: ServerCommandSource): Boolean {
        return isOwner(source)
    }

    override val father: PermissionHolder?
        get() = null

    override fun onRemoveChild(child: PermissionHolder) {
        enclosures.remove(child.fullName)
        ServerMain.groups.markDirty()
    }

    override fun addChild(child: PermissionHolder) {
        enclosures.add(child.fullName)
        ServerMain.groups.markDirty()
    }

    override fun inheritPermission(): Boolean {
        return true
    }

    class Groups : PersistentState {
        var groups: MutableMap<String, EnclosureGroup> = HashMap()

        constructor() {
            markDirty()
        }

        constructor(nbt: NbtCompound) {
            nbt.keys.forEach { key ->
                val group = EnclosureGroup()
                val groupNbt = nbt.getCompound(key)
                group.name = key
                group.owner = groupNbt.getUuid("owner")
                groupNbt.getList("enclosures", NbtList.STRING_TYPE.toInt())
                    .forEach { nbtElement -> group.enclosures.add(nbtElement.asString()) }
                groupNbt.getCompound("permissions").keys.forEach { uuid: String ->
                    val permNbt = groupNbt.getCompound("permissions").getCompound(uuid)
                    permNbt.keys.forEach { perm ->
                        group.permissionsMap.computeIfAbsent(UUID.fromString(uuid)) { _ -> HashMap() }[perm] =
                            permNbt.getBoolean(perm)
                    }
                }
                groups[key] = group
            }
        }

        override fun writeNbt(nbt: NbtCompound): NbtCompound {
            groups.forEach { (name: String, group: EnclosureGroup) ->
                val sub = NbtCompound()
                sub.putUuid("owner", group.owner)
                sub.put("enclosures", NbtList().apply {
                    group.enclosures.forEach { n -> add(NbtString.of(n)) }
                })
                sub.put("permissions", NbtCompound().apply {
                    group.permissionsMap.forEach { (uuid, perms) ->
                        val permNbt = NbtCompound()
                        perms.forEach { (key, value) -> permNbt.putBoolean(key, value) }
                        put(uuid.toString(), permNbt)
                    }
                })
                nbt.put(name, sub)
            }
            return nbt
        }

        fun addGroup(group: EnclosureGroup) {
            groups[group.name] = group
        }

        fun removeGroup(name: String) {
            groups.remove(name)
        }

        fun getGroup(name: String): EnclosureGroup? {
            return groups[name]
        }

        fun getGroups(): Collection<EnclosureGroup> {
            return groups.values
        }
    }

    companion object {
        const val GROUPS_KEY = "enclosure.groups"
    }
}