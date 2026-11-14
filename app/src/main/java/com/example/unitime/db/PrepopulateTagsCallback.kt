package com.example.unitime.db

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.unitime.db.events.EventsDbProvider
import com.example.unitime.db.tags.DefaultTags
import com.example.unitime.db.tags.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrepopulateTagsCallback(
    private val context: Context
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)

        CoroutineScope(Dispatchers.IO).launch {
            val tagDao = EventsDbProvider.get(context).getTagDao()
            tagDao.insertAll(DefaultTags.ALL.map { Tag(name = it) })
        }
    }
}