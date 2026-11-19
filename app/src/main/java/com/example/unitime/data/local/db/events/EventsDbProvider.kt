package com.example.unitime.data.local.db.events // Проверьте ваш пакет

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

// Если у вас есть отдельный объект-провайдер, он должен выглядеть так:
object EventsDbProvider {
    @Volatile
    private var INSTANCE: EventsDb? = null

    fun get(context: Context): EventsDb {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                EventsDb::class.java,
                "events_database" // Имя вашей БД
            )
                // ВАЖНО: Эта строка решает проблему миграции, позволяя пересоздать БД при изменении версии
                .fallbackToDestructiveMigration()
                .build()
            INSTANCE = instance
            instance
        }
    }
}