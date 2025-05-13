package com.github.zly2006.enclosure

import net.minecraft.nbt.NbtCompound
import net.minecraft.server.command.ServerCommandSource
import java.util.*

interface EnclosureView: PermissionHolder {
    val minX: Int
    val minY: Int
    val minZ: Int
    val maxX: Int
    val maxY: Int
    val maxZ: Int
    val enterMessage: String
    val leaveMessage: String
    val createdOn: Long
    class ReadOnly(private val nbt: NbtCompound): EnclosureView, Cloneable {
        public override fun clone(): ReadOnly = ReadOnly(nbt.copy())
        override val minX: Int = nbt.getInt("min_x").orElseThrow()
        override val minY: Int = nbt.getInt("min_y").orElseThrow()
        override val minZ: Int = nbt.getInt("min_z").orElseThrow()
        override val maxX: Int = nbt.getInt("max_x").orElseThrow()
        override val maxY: Int = nbt.getInt("max_y").orElseThrow()
        override val maxZ: Int = nbt.getInt("max_z").orElseThrow()
        override val enterMessage: String = nbt.getString("enter_msg").orElseThrow()
        override val leaveMessage: String = nbt.getString("leave_msg").orElseThrow()
        override val createdOn: Long = nbt.getLong("created_on").orElseThrow()
        override var permissionsMap: MutableMap<UUID, MutableMap<String, Boolean>> = mutableMapOf()
        override val name: String = nbt.getString("name").orElseThrow()
        override val owner: UUID = nbt.getUuid("owner")
        override fun isOwner(source: ServerCommandSource): Boolean = false
        override fun getSetPermissionCommand(uuid: UUID): String = ""
        override fun onRemoveChild(child: PermissionHolder) {}
        override fun addChild(child: PermissionHolder) {}
        override fun inheritPermission(): Boolean = false
        override fun isOwnerOrFatherAdmin(source: ServerCommandSource): Boolean = false
        override val father: PermissionHolder? = null

        init {
            val permissions = nbt.getCompound("permission").orElseThrow()
            permissions.keys.forEach {
                val uuid = UUID.fromString(it)
                val permission = permissions.getCompound(it).orElseThrow()
                val permissionMap = mutableMapOf<String, Boolean>()
                permission.keys.forEach { perm ->
                    permissionMap[perm] = permission.getBoolean(perm).orElseThrow()
                }
                permissionsMap[uuid] = permissionMap
            }
        }
    }
    companion object {
        fun readonly(nbt: NbtCompound) = ReadOnly(nbt)
    }
}
