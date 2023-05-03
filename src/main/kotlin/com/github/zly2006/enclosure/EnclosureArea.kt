package com.github.zly2006.enclosure

import com.github.zly2006.enclosure.command.CONSOLE
import com.github.zly2006.enclosure.command.Session
import com.github.zly2006.enclosure.gui.EnclosureScreenHandler
import com.github.zly2006.enclosure.network.NetworkChannels
import com.github.zly2006.enclosure.utils.*
import com.github.zly2006.enclosure.utils.Serializable2Text.SerializationSettings
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtDouble
import net.minecraft.nbt.NbtList
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.ClickEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.Heightmap
import net.minecraft.world.PersistentState
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.ReadWriteProperty

open class EnclosureArea : PersistentState, ReadOnlyEnclosureArea {
    private fun <T> lockChecker(initialValue: T): ReadWriteProperty<Any?, T> {
        return object : ReadWriteProperty<Any?, T> {
            var value = initialValue
            override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T {
                return value
            }

            override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: T) {
                if (locked) {
                    throw IllegalStateException("Cannot modify locked area")
                }
                this.value = value
                markDirty()
            }
        }
    }
    private var locked = false
    final override var minX by lockChecker(0)
    final override var minY by lockChecker(0)
    final override var minZ by lockChecker(0)
    final override var maxX by lockChecker(0)
    final override var maxY by lockChecker(0)
    final override var maxZ by lockChecker(0)
    var world: ServerWorld
        protected set
    final override var name = ""
    final override var owner: UUID by lockChecker(CONSOLE)
    var teleportPos: Vec3d? = null
    var yaw by lockChecker(0f)
    var pitch by lockChecker(0f)
    final override var enterMessage by lockChecker("")
    final override var leaveMessage by lockChecker("")
    final override var permissionsMap: MutableMap<UUID, MutableMap<String, Boolean>> = HashMap()
    final override var createdOn: Long = 0
        protected set
    override var father: PermissionHolder? = null
        protected set

    override val fullName: String
        get() = if (father != null) {
            father!!.name + "." + name
        } else {
            name
        }

    /**
     * Create an instance from nbt
     *
     * @param compound nbt compound
     */
    constructor(compound: NbtCompound, world: ServerWorld) {
        this.world = world
        name = compound.getString("name")
        minX = compound.getInt("min_x")
        minY = compound.getInt("min_y")
        minZ = compound.getInt("min_z")
        maxX = compound.getInt("max_x")
        maxY = compound.getInt("max_y")
        maxZ = compound.getInt("max_z")
        yaw = compound.getFloat("yaw")
        pitch = compound.getInt("pitch").toFloat()
        enterMessage = compound.getString("enter_msg")
        leaveMessage = compound.getString("leave_msg")
        createdOn = compound.getLong("created_on")
        val tpPos = (compound["tp_pos"] as NbtList?)!!
        teleportPos = Vec3d(
            tpPos.getDouble(0),
            tpPos.getDouble(1),
            tpPos.getDouble(2)
        )
        for (playerUuid in compound.getCompound("permission").keys) {
            val perm: MutableMap<String, Boolean> = HashMap()
            val nbtPerm = compound.getCompound("permission").getCompound(playerUuid)
            for (key in nbtPerm.keys) {
                perm[key] = nbtPerm.getBoolean(key)
            }
            permissionsMap[UUID.fromString(playerUuid)] = perm
        }
        owner = compound.getUuid("owner")
    }

    operator fun Map<String, Boolean>.get(perm: Permission): Boolean? {
        return perm.getValue(this).orElse(null)
    }

    operator fun Map<String, Boolean>.set(perm: Permission, value: Boolean?) {
        return perm.setValue(this, value)
    }

    constructor(session: Session, name: String) {
        world = session.world
        owner = session.owner
        this.name = name
        permissionsMap[owner] = mutableMapOf()
        Permission.PERMISSIONS.values.filter { p -> p.target.fitPlayer() }
            .forEach { p -> permissionsMap[owner]!![p] = true }
        minX = min(session.pos1.x, session.pos2.x)
        minY = min(session.pos1.y, session.pos2.y)
        minZ = min(session.pos1.z, session.pos2.z)
        maxX = max(session.pos1.x, session.pos2.x)
        maxY = max(session.pos1.y, session.pos2.y)
        maxZ = max(session.pos1.z, session.pos2.z)
        yaw = 0f
        pitch = 0f
        val centerX = (minX + maxX) / 2
        val centerZ = (minZ + maxZ) / 2
        var centerY = Utils.topYOf(session.world, centerX, centerZ, maxY)
        if (centerY < minY) {
            centerY = maxY
        }
        createdOn = System.currentTimeMillis()
        teleportPos = Vec3d(centerX.toDouble(), centerY.toDouble(), centerZ.toDouble())
        markDirty()
    }

    fun kickPlayer(player: ServerPlayerEntity) {
        val x = minX - 1
        val z = minZ - 1
        val y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z)
        if (y == world.bottomY) {
            // this player is alive, but in a void
            minecraftServer.playerManager.respawnPlayer(player, true)
            minecraftServer.overworld.chunkManager.updatePosition(player)
        } else {
            player.teleport(world, x.toDouble(), y.toDouble(), z.toDouble(), 0f, 0f)
        }
    }

    override fun writeNbt(nbt: NbtCompound): NbtCompound {
        nbt.putString("name", name)
        nbt.putInt("min_x", minX)
        nbt.putInt("min_y", minY)
        nbt.putInt("min_z", minZ)
        nbt.putInt("max_x", maxX)
        nbt.putInt("max_y", maxY)
        nbt.putInt("max_z", maxZ)
        nbt.putLong("created_on", createdOn)
        nbt.putString("enter_msg", enterMessage)
        nbt.putString("leave_msg", leaveMessage)
        val nbtPermission = NbtCompound()
        permissionsMap.forEach { (key: UUID, value) ->
            nbtPermission.put(key.toString(), value.toNbt())
        }
        nbt.put("permission", nbtPermission)
        val nbtTpPos = NbtList()
        nbt.putFloat("yaw", yaw)
        nbt.putFloat("pitch", pitch)
        nbtTpPos.add(NbtDouble.of(teleportPos!!.getX()))
        nbtTpPos.add(NbtDouble.of(teleportPos!!.getY()))
        nbtTpPos.add(NbtDouble.of(teleportPos!!.getZ()))
        nbt.put("tp_pos", nbtTpPos)
        nbt.putUuid("owner", owner)
        return nbt
    }

    open fun areaOf(pos: BlockPos): EnclosureArea {
        return if (isInner(pos)) {
            this
        } else {
            throw RuntimeException("The position $pos is not in the area$name")
        }
    }

    fun includesArea(area: EnclosureArea): Boolean {
        return minX <= area.minX && minY <= area.minY && minZ <= area.minZ && maxX >= area.maxX && maxY >= area.maxY && maxZ >= area.maxZ
    }

    fun inArea(area: EnclosureArea): Boolean {
        return minX >= area.minX && minY >= area.minY && minZ >= area.minZ && maxX <= area.maxX && maxY <= area.maxY && maxZ <= area.maxZ
    }

    fun intersect(area: EnclosureArea): Boolean {
        return intersect(area.minX, area.minY, area.minZ, area.maxX, area.maxY, area.maxZ)
    }

    fun intersect(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Boolean {
        return minX <= x2 && maxX >= x1 && minY <= y2 && maxY >= y1 && minZ <= z2 && maxZ >= z1
    }

    fun isSame(obj: Any): Boolean {
        return if (obj is EnclosureArea) {
            minX == obj.minX && minY == obj.minY && minZ == obj.minZ && maxX == obj.maxX && maxY == obj.maxY && maxZ == obj.maxZ
        } else {
            false
        }
    }

    fun isInner(pos: BlockPos): Boolean {
        return pos.x >= minX && pos.y >= minY && pos.z >= minZ && pos.x <= maxX && pos.y <= maxY && pos.z <= maxZ
    }

    override fun isOwner(source: ServerCommandSource): Boolean {
        if (source.hasPermissionLevel(4)) {
            return true
        }
        if (father != null && father!!.isOwner(source)) {
            return true
        }
        return if (source.player != null) {
            source.player!!.uuid == owner
        } else {
            false
        }
    }

    override fun isOwnerOrFatherAdmin(source: ServerCommandSource): Boolean {
        if (source.hasPermissionLevel(4)) {
            return true
        }
        if (father != null && father!!.isOwnerOrFatherAdmin(source)) {
            return true
        }
        return if (source.player != null) {
            source.player!!.uuid == owner || hasPerm(
                source.player!!.uuid,
                Permission.ADMIN
            )
        } else {
            false
        }
    }

    private fun checkLock() {
        if (locked) {
            throw RuntimeException("This area is locked")
        }
    }

    override fun setPermission(source: ServerCommandSource?, uuid: UUID, perm: Permission, value: Boolean?) {
        checkLock()
        if (source != null && source.player != null && !hasPerm(source.player!!, Permission.ADMIN)) {
            LOGGER.warn("Player " + source.name + " try to set permission of " + uuid + " in " + name + " without admin permission")
            LOGGER.warn("allowing, if you have any problem please report to the author")
        }
        LOGGER.info("${source?.name ?: "<null>"} set perm ${perm.name} to $value for $uuid in $fullName")
        super.setPermission(source, uuid, perm, value)
        // sync to client
        minecraftServer.playerManager.playerList.forEach {
            val handler = it.currentScreenHandler as? EnclosureScreenHandler ?: return@forEach
            if (handler.fullName == fullName) {
                val buf = PacketByteBufs.create()
                buf.writeUuid(uuid)
                buf.writeNbt(permissionsMap[uuid].toNbt())
                ServerPlayNetworking.send(it, NetworkChannels.SYNC_PERMISSION, buf)
            }
        }
        markDirty()
    }

    override fun getSetPermissionCommand(uuid: UUID): String {
        return "/enclosure set $fullName uuid $uuid "
    }

    fun distanceTo(pos: Vec3d): Vec3d {
        if (isInner(BlockPos.ofFloored(pos))) {
            return Vec3d.ZERO
        }
        var x = 0.0
        var y = 0.0
        var z = 0.0
        if (pos.getX() < minX) {
            x = minX - pos.getX()
        } else if (pos.getX() > maxX) {
            x = pos.getX() - maxX
        }
        if (pos.getY() < minY) {
            y = minY - pos.getY()
        } else if (pos.getY() > maxY) {
            y = pos.getY() - maxY
        }
        if (pos.getZ() < minZ) {
            z = minZ - pos.getZ()
        } else if (pos.getZ() > maxZ) {
            z = pos.getZ() - maxZ
        }
        return Vec3d(x, y, z)
    }

    override fun serialize(settings: SerializationSettings, player: ServerPlayerEntity?): MutableText {
        return when (settings) {
            SerializationSettings.Name -> Text.literal(fullName)
            SerializationSettings.Hover -> {
                TrT.of("enclosure.message.select.from") +
                        literalText("[").darkGreen() +
                        literalText(minX).green() +
                        literalText(", ").darkGreen() +
                        literalText(minY).green() +
                        literalText(", ").darkGreen() +
                        literalText(minZ).green() +
                        literalText("]").darkGreen() +
                        TrT.of("enclosure.message.select.to") +
                        literalText("[").darkGreen() +
                        literalText(maxX).green() +
                        literalText(", ").darkGreen() +
                        literalText(maxY).green() +
                        literalText(", ").darkGreen() +
                        literalText(maxZ).green() +
                        literalText("]").darkGreen() +
                        TrT.of("enclosure.message.select.world") +
                        literalText(world.registryKey.value).green() +
                        literalText("\n") +
                        TrT.of("enclosure.info.created_on") +
                        literalText(SimpleDateFormat().format(Date(createdOn))).gold()
            }

            SerializationSettings.Summarize -> {
                val text = serialize(SerializationSettings.Name, player).gold().styled {
                    it.withClickEvent(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/enclosure tp $fullName"))
                        .hoverText(serialize(SerializationSettings.Hover, player))
                }
                text += TrT.of("enclosure.info.created_by").white()
                val ownerName = Utils.getNameByUUID(owner)
                text += (ownerName?.let { Text.literal(it).gold() } ?: TrT.of("enclosure.message.unknown_user").red())
                    .hoverText(Text.literal("UUID: $owner"))
                text
            }

            SerializationSettings.Full -> {
                val text = Text.empty()
                if (father != null) {
                    text += (TrT.of("enclosure.info.father_land").white())
                    text += father!!.serialize(SerializationSettings.Name, player).gold()
                        .hoverText(father!!.serialize(SerializationSettings.Hover, player))
                        .styled {
                            it.withClickEvent(
                                ClickEvent(
                                    ClickEvent.Action.SUGGEST_COMMAND,
                                    "/enclosure info ${father!!.fullName}"
                                )
                            )
                        }
                    text.append("\n")
                }
                text += serialize(SerializationSettings.Summarize, player)
                    .append("\n") + (super.serialize(SerializationSettings.Full, player))
                text
            }

            SerializationSettings.BarredFull -> {
                val bar = Text.literal("------------------------------")
                    .styled { style: Style -> style.withColor(Formatting.YELLOW).withBold(true) }
                Text.empty()
                    .append(bar)
                    .append("\n")
                    .append(serialize(SerializationSettings.Full, player))
                    .append("\n")
                    .append(bar)
            }
        }
    }

    open fun changeWorld(world: ServerWorld) {
        checkLock()
        if (world === this.world) {
            return
        }
        markDirty()
        this.world = world
    }

    final override fun markDirty() {
        ServerMain.getAllEnclosures(world).markDirty()
    }

    fun setTeleportPos(teleportPos: Vec3d?, yaw: Float, pitch: Float) {
        checkLock()
        this.teleportPos = teleportPos
        this.yaw = yaw
        this.pitch = pitch
        markDirty()
    }

    fun teleport(player: ServerPlayerEntity) {
        player.teleport(world, teleportPos!!.x, teleportPos!!.y, teleportPos!!.z, yaw, pitch)
    }

    override fun onRemoveChild(child: PermissionHolder) {
        throw IllegalStateException("Enclosure area cannot have child")
    }

    override fun addChild(child: PermissionHolder) {
        throw IllegalStateException("Enclosure area cannot have child")
    }

    override fun inheritPermission(): Boolean {
        return false
    }

    fun lock() {
        locked = true
    }

    fun unlock() {
        locked = false
    }

    fun mirrorFrom(enclosure: EnclosureArea) {
        minX = enclosure.minX
        minY = enclosure.minY
        minZ = enclosure.minZ
        maxX = enclosure.maxX
        maxY = enclosure.maxY
        maxZ = enclosure.maxZ
        owner = enclosure.owner
        teleportPos = enclosure.teleportPos
        yaw = enclosure.yaw
        pitch = enclosure.pitch
        enterMessage = enclosure.enterMessage
        leaveMessage = enclosure.leaveMessage
        permissionsMap = enclosure.permissionsMap
        createdOn = enclosure.createdOn
        locked = false
        markDirty()
    }
}

private fun Map<String, Boolean>?.toNbt(): NbtCompound {
    val nbt = NbtCompound()
    this?.forEach { (key, value) ->
        nbt.putBoolean(key, value)
    }
    return nbt
}
