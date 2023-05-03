package com.github.zly2006.enclosure

import com.github.zly2006.enclosure.command.CONSOLE
import com.github.zly2006.enclosure.exceptions.PermissionTargetException
import com.github.zly2006.enclosure.utils.*
import com.github.zly2006.enclosure.utils.Serializable2Text.SerializationSettings
import com.mojang.datafixers.util.Pair
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.*
import net.minecraft.util.Formatting
import java.util.*

interface PermissionHolder : Serializable2Text {
    fun isOwner(source: ServerCommandSource): Boolean
    val owner: UUID
    fun isOwnerOrFatherAdmin(source: ServerCommandSource): Boolean
    val father: PermissionHolder?
    val fullName: String
        get() = name

    /**
     * 这个方法会判断默认权限，uuid那个不会
     */
    fun hasPerm(player: ServerPlayerEntity, perm: Permission): Boolean {
        if (player.commandSource.hasPermissionLevel(4) && perm.isIgnoreOp) {
            return true
        }
        return if (perm === Permission.ADMIN && isOwnerOrFatherAdmin(player.commandSource)) {
            true
        } else hasPerm(player.uuid, perm)
    }

    /**
     * uuid版本只检查permissionMap, 不判断默认权限
     */
    fun hasPerm(uuid: UUID, perm: Permission): Boolean {
        if (perm.target.fitPlayer() && permissionsMap.containsKey(uuid)) {
            val ob = perm.getValue(permissionsMap[uuid])
            if (ob.isPresent) {
                return ob.get()
            }
        }
        if (perm.target.fitEnclosure() && permissionsMap.containsKey(CONSOLE)) {
            val ob = perm.getValue(permissionsMap[CONSOLE])
            if (ob.isPresent) {
                return ob.get()
            }
        }
        return if (father != null && inheritPermission()) {
            father!!.hasPerm(uuid, perm)
        } else perm.defaultValue
    }

    @Throws(PermissionTargetException::class)
    fun hasPubPerm(perm: Permission): Boolean {
        if (!perm.target.fitEnclosure()) {
            throw PermissionTargetException(
                TrT.of("enclosure.message.permission_target_error")+(Text.literal(perm.target.name))
            )
        }
        return hasPerm(CONSOLE, perm)
    }

    fun setPermission(source: ServerCommandSource?, uuid: UUID, perm: Permission, value: Boolean?) {
        if (!permissionsMap.containsKey(uuid) && value != null) {
            permissionsMap[uuid] = HashMap()
        }
        if (uuid == CONSOLE && !perm.target.fitEnclosure()) {
            throw PermissionTargetException(
                TrT.of("enclosure.message.permission_target_error").append(perm.target.name)
            )
        } else if (uuid != CONSOLE && !perm.target.fitPlayer()) {
            throw PermissionTargetException(
                TrT.of("enclosure.message.permission_target_error").append(perm.target.name)
            )
        }
        perm.setValue(permissionsMap[uuid], value)
        if (permissionsMap[uuid]!!.isEmpty()) {
            permissionsMap.remove(uuid)
        }
    }

    val permissionsMap: MutableMap<UUID, MutableMap<String, Boolean>>
    val name: String

    fun serializePermission(map: MutableMap<String, Boolean>): Text {
        val text = Text.literal("")
        map.forEach { (key: String?, value: Boolean) ->
            if (value) {
                text.append(Text.literal(key).setStyle(Style.EMPTY.withColor(Formatting.GREEN)))
            } else {
                text.append(Text.literal(key).setStyle(Style.EMPTY.withColor(Formatting.RED)))
            }
            text.append(" ")
        }
        return text
    }

    fun getSetPermissionCommand(uuid: UUID): String
    override fun serialize(settings: SerializationSettings, player: ServerPlayerEntity?): MutableText {
        if (settings != SerializationSettings.Full) return Text.literal(name)
        val text = TrT.of("enclosure.message.permissions_header")
        permissionsMap.entries.map { (key, value) ->
            var ordinal = -1
            var style = Style.EMPTY
                .withClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, getSetPermissionCommand(key)))
                .hoverText(
                    if (key == CONSOLE) serializePermission(value) else  // 不是默认的，就显示uuid
                        Text.literal("UUID=$key: ").gold() + serializePermission(value)
                )
            var name: String? = null
            if (key == CONSOLE) {
                name = "@GLOBAL"
                style = style.withColor(Formatting.BLUE)
                ordinal = 0
            }
            if (key == owner) {
                style = style.withColor(Formatting.GREEN)
                ordinal = 1
            }
            if (name == null) {
                name = Utils.getNameByUUID(key)
                ordinal = 2
            }
            if (name == null) {
                name = "UNKNOWN"
                style = style.withColor(Formatting.RED)
                ordinal = 3
            }
            val item = Text.literal(name).setStyle(style).append(" ")
            Pair.of(item, ordinal)
        }.sortedBy { it.second }.forEach { text.append(it.first) }
        return text
    }

    fun onRemoveChild(child: PermissionHolder)
    fun addChild(child: PermissionHolder)
    fun inheritPermission(): Boolean
}