package com.example.unitime.data.local.db.events

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.unitime.data.local.db.tags.PrepopulateTagsCallback

object EventsDbProvider {

    @Volatile
    private var instance: EventsDb? = null

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
            ALTER TABLE events
            ADD COLUMN created_by_login TEXT NOT NULL DEFAULT ''
            """
            )
        }
    }

    fun get(context: Context): EventsDb =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                EventsDb::class.java,
                "events_database"
            )
                .addMigrations(MIGRATION_5_6)
                .addCallback(
                    PrepopulateTagsCallback(context.applicationContext)
                )
                .build()
                .also { instance = it }
        }
}
