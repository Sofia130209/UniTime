package com.example.unitime.db

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.unitime.db.tags.DefaultTags
import com.example.unitime.db.tags.Tag

class PrepopulateTagsCallback(
    private val context: Context
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

        // * ВАЖНО: внутри Room-колбэка нельзя трогать EventsDbProvider.get(...) —
        // * база ещё не достроена и это приводит к рекурсии/зависанию (вылету) главного экрана.
        // * Поэтому теги вставляем напрямую в уже открытый SupportSQLiteDatabase.
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