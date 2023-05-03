package com.github.zly2006.enclosure

import net.minecraft.nbt.NbtCompound
import net.minecraft.server.command.ServerCommandSource
import java.util.*

interface ReadOnlyEnclosureArea: PermissionHolder {
    val minX: Int
    val minY: Int
    val minZ: Int
    val maxX: Int
    val maxY: Int
    val maxZ: Int
    val enterMessage: String
    val leaveMessage: String
    val createdOn: Long
    companion object {
        fun fromTag(nbt: NbtCompound): ReadOnlyEnclosureArea {
            return object : ReadOnlyEnclosureArea {
                override val minX: Int = nbt.getInt("min_x")
                override val minY: Int = nbt.getInt("min_y")
                override val minZ: Int = nbt.getInt("min_z")
                override val maxX: Int = nbt.getInt("max_x")
                override val maxY: Int = nbt.getInt("max_y")
                override val maxZ: Int = nbt.getInt("max_z")
                override val enterMessage: String = nbt.getString("enter_msg")
                override val leaveMessage: String = nbt.getString("leave_msg")
                override val createdOn: Long = nbt.getLong("created_on")
                override var permissionsMap: MutableMap<UUID, MutableMap<String, Boolean>> = mutableMapOf()
                override val name: String = nbt.getString("name")
                override val owner: UUID = nbt.getUuid("owner")
                override fun isOwner(source: ServerCommandSource): Boolean = false
                override fun getSetPermissionCommand(uuid: UUID): String = ""
                override fun onRemoveChild(child: PermissionHolder) { }
                override fun addChild(child: PermissionHolder) { }
                override fun inheritPermission(): Boolean = false
                override fun isOwnerOrFatherAdmin(source: ServerCommandSource): Boolean = false
                override val father: PermissionHolder? = null

                init {
                    val permissions = nbt.getCompound("permission")
                    permissions.keys.forEach {
                        val uuid = UUID.fromString(it)
                        val permission = permissions.getCompound(it)
                        val permissionMap = mutableMapOf<String, Boolean>()
                        permission.keys.forEach { perm ->
                            permissionMap[perm] = permission.getBoolean(perm)
                        }
                        permissionsMap[uuid] = permissionMap
                    }
                }
            }
        }
    }
}
