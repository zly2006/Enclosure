package com.github.zly2006.enclosure.backup

import com.github.zly2006.enclosure.EnclosureArea
import net.minecraft.nbt.NbtHelper
import net.minecraft.nbt.NbtIo
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.math.BlockPos
import java.io.ByteArrayOutputStream
import java.sql.DriverManager

class BackupManager {
    private val connection = DriverManager.getConnection("jdbc:sqlite:world/backup_info.db")
    init {
        connection.createStatement()
            .execute("CREATE TABLE IF NOT EXISTS backup_info (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, last_backup_time INTEGER, backup_player TEXT, table_name TEXT)")
    }
    private val updateStatement = connection.prepareStatement("INSERT INTO backup_info (name, last_backup_time, backup_player, table_name) VALUES (?, ?, ?, ?)")
    fun backup(area: EnclosureArea, source: ServerCommandSource): Boolean {
        val tableName = "backup_${area.fullName}_${System.currentTimeMillis()}"
        updateStatement.apply {
            setString(1, area.fullName)
            setLong(2, System.currentTimeMillis())
            setString(3, source.name)
            setString(4, tableName)
        }.execute()
        connection.createStatement().execute("CREATE TABLE IF NOT EXISTS block_${tableName} (x INTEGER, y INTEGER, z INTEGER, block_properties BLOB, block_data BLOB)")
        val setBlockStatement = connection.prepareStatement("INSERT INTO block_${tableName} VALUES (?, ?, ?, ?, ?)")
        for (x in area.minX..area.maxX) {
            for (y in area.minY..area.maxY) {
                for (z in area.minZ..area.maxZ) {
                    val block = area.world.getBlockState(BlockPos(x, y, z))
                    setBlockStatement.apply {
                        setInt(1, x)
                        setInt(2, y)
                        setInt(3, z)
                        run {
                            val stream = ByteArrayOutputStream()
                            NbtIo.writeCompressed(NbtHelper.fromBlockState(block), stream)
                            setBytes(4, stream.toByteArray())
                        }
                        if (block.hasBlockEntity()) {
                            val stream = ByteArrayOutputStream()
                            NbtIo.writeCompressed(area.world.getBlockEntity(BlockPos(x, y, z))!!.createNbt(), stream)
                            setBytes(5, stream.toByteArray())
                        }
                    }.execute()
                }
            }
        }
        return true
    }
}