package com.github.zly2006.enclosure.backup

import com.github.zly2006.enclosure.EnclosureArea
import java.sql.DriverManager

class BackupManager {
    private val connection = DriverManager.getConnection("jdbc:sqlite:backup_info.db")
    init {
        connection.createStatement()
            .execute("CREATE TABLE IF NOT EXISTS backup_info (id INTEGER PRIMARY KEY AUTOINCREMENT, last_backup_time INTEGER, table_name TEXT)")
        connection.createStatement()
            .execute("CREATE TABLE IF NOT EXISTS all_backup (name TEXT, id INTEGER)")
    }
    private val updateStatement = connection.prepareStatement("INSERT INTO all_backup (name, id) VALUES (?, (select max(id) + 1 from all_backup) )")
    fun backup(area: EnclosureArea): Boolean {
        updateStatement.apply {
            setString(1, area.fullName)
        }.execute()
        return true
    }
}