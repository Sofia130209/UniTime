package com.example.unitime.data.local.db.events

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.unitime.data.local.db.tags.PrepopulateTagsCallback

object EventsDbProvider {

    @Volatile
    private var instance: EventsDb? = null

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS event_participants (
                    event_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    PRIMARY KEY(event_id, user_id),
                    FOREIGN KEY(event_id)
                        REFERENCES events(id)
                        ON UPDATE NO ACTION
                        ON DELETE CASCADE
                )
            """)

            db.execSQL("""
                CREATE INDEX IF NOT EXISTS index_event_participants_user_id
                ON event_participants(user_id)
            """)
        }
    }

    fun get(context: Context): EventsDb =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                EventsDb::class.java,
                "events_database"
            )
                .addMigrations(MIGRATION_4_5)
                .addCallback(
                    PrepopulateTagsCallback(context.applicationContext)
                )
                .build()
                .also { instance = it }
        }
}
