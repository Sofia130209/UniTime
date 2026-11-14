package com.example.unitime.db.events

import android.content.Context
import androidx.room.Room
import com.example.unitime.db.PrepopulateTagsCallback

object EventsDbProvider {
    @Volatile
    private var instance: EventsDb? = null

    fun get(context: Context): EventsDb =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                EventsDb::class.java,
                "events_database"
            )
                .addCallback(PrepopulateTagsCallback(context.applicationContext))
                .build()
                .also { instance = it }
        }
}
