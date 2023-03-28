package com.github.zly2006.enclosure

import com.github.zly2006.enclosure.utils.Serializable2Text.SerializationSettings
import com.github.zly2006.enclosure.utils.Utils
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtString
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.HoverEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.world.PersistentState
import java.util.*
import java.util.function.Consumer

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
            SerializationSettings.Summarize -> serialize(SerializationSettings.Name, player).styled { style: Style ->
                style
                    .withHoverEvent(
                        HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            serialize(SerializationSettings.Hover, player)
                        )
                    )
            }

            SerializationSettings.Full -> {
                val text = Text.empty()
                for (enclosure in enclosures) {
                    text.append(
                        ServerMain.Instance.getEnclosure(enclosure)!!
                            .serialize(SerializationSettings.Summarize, player)
                    ).append(", ")
                }
                Text.literal("Group: ").append(serialize(SerializationSettings.Name, player))
                    .append("\nOwner: ").append(Utils.getDisplayNameByUUID(owner))
                    .append("\nEnclosures: ").append(text)
                    .append("\n").append(super<PermissionHolder>.serialize(SerializationSettings.Full, player))
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
        ServerMain.Instance.groups.markDirty()
    }

    override fun addChild(child: PermissionHolder) {
        enclosures.add(child.fullName)
        ServerMain.Instance.groups.markDirty()
    }

    override fun inheritPermission(): Boolean {
        return true
    }

    class Groups : PersistentState {
        var groups: MutableMap<String?, EnclosureGroup> = HashMap()

        constructor() {
            markDirty()
        }

        constructor(nbt: NbtCompound) {
            nbt.keys.forEach(Consumer { key ->
                val group = EnclosureGroup()
                val groupNbt = nbt.getCompound(key)
                group.name = key
                group.owner = groupNbt.getUuid("owner")
                groupNbt.getList("enclosures", NbtList.STRING_TYPE.toInt())
                    .forEach(Consumer { nbtElement: NbtElement -> group.enclosures.add(nbtElement.asString()) })
                groupNbt.getCompound("permissions").keys.forEach(Consumer { uuid: String? ->
                    val permNbt = groupNbt.getCompound("permissions").getCompound(uuid)
                    permNbt.keys.forEach(Consumer { perm: String ->
                        group.permissionsMap.computeIfAbsent(
                            UUID.fromString(
                                uuid
                            )
                        ) { k: UUID? -> HashMap() }[perm] = permNbt.getBoolean(perm)
                    })
                })
                groups[key] = group
            })
        }

        override fun writeNbt(nbt: NbtCompound): NbtCompound {
            groups.forEach { (name: String?, group: EnclosureGroup) ->
                val sub = NbtCompound()
                sub.putUuid("owner", group.owner)
                val enclosures = NbtList()
                group.enclosures.forEach(Consumer { n: String? -> enclosures.add(NbtString.of(n)) })
                sub.put("enclosures", enclosures)
                val permissions = NbtCompound()
                group.permissionsMap.forEach { (uuid: UUID, perms: Map<String, Boolean>) ->
                    val permNbt = NbtCompound()
                    perms.forEach { (key: String?, value: Boolean?) -> permNbt.putBoolean(key, value) }
                    permissions.put(uuid.toString(), permNbt)
                }
                sub.put("permissions", permissions)
                nbt.put(name, sub)
            }
            return nbt
        }

        fun addGroup(group: EnclosureGroup) {
            groups[group.name] = group
        }

        fun removeGroup(name: String?) {
            groups.remove(name)
        }

        fun getGroup(name: String?): EnclosureGroup? {
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