package com.example.unitime.data.local.db.tags

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.unitime.data.local.model.Tag

class PrepopulateTagsCallback(
    private val context: Context
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        try {
            val stmt = db.compileStatement(
                "INSERT OR IGNORE INTO ${Tag.TABLE_NAME} (${Tag.NAME_COLUMN}) VALUES (?)"
            )
            for (name in DefaultTags.ALL) {
                stmt.bindString(1, name)
                stmt.executeInsert()
                stmt.clearBindings()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}