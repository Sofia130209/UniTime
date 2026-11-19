package com.example.unitime.data.local.db.events

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import com.example.unitime.data.local.dao.EventParticipantDao // Импорт DAO участников
import com.example.unitime.data.local.model.Event
import com.example.unitime.data.local.model.EventParticipant
import com.example.unitime.data.local.model.EventTagCrossRef
import com.example.unitime.data.local.model.EventWithTags
import com.example.unitime.data.local.model.Tag
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [
        Event::class,
        EventTagCrossRef::class,
        Tag::class,
        EventParticipant::class // Таблица участников добавлена в схему
    ],
    version = 7, // Версия увеличена
    exportSchema = false // Отключаем экспорт схемы, чтобы убрать предупреждение
)
abstract class EventsDb : RoomDatabase() {
    abstract fun getEventsDao(): EventsDao
    abstract fun getTagDao(): TagDao
    abstract fun getEventParticipantDao(): EventParticipantDao
}

@Dao
interface EventsDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: Event): Long

    @Query("SELECT COUNT(*) FROM ${Event.TABLE_NAME} WHERE ${Event.CREATED_BY_LOGIN} = :login")
    fun observeCreatedCount(login: String?): Flow<Int>

    @Update suspend fun update(event: Event)
    @Delete suspend fun delete(event: Event)

    @Query("SELECT * FROM ${Event.TABLE_NAME} WHERE ${Event.ID_COLUMN} = :id")
    suspend fun getById(id: Long): Event?

    @Transaction
    @Query("SELECT * FROM ${Event.TABLE_NAME} WHERE ${Event.ID_COLUMN} = :id")
    suspend fun getWithTags(id: Long): EventWithTags?

    @Transaction
    @Query("SELECT * FROM ${Event.TABLE_NAME} ORDER BY ${Event.ACTIVITY_DATE} ASC")
    fun observeAllWithTags(): Flow<List<EventWithTags>>

    @Transaction
    @Query("""
    SELECT e.* FROM ${Event.TABLE_NAME} e
    INNER JOIN ${EventTagCrossRef.TABLE_NAME} r
        ON r.${EventTagCrossRef.EVENT_ID_COLUMN} = e.${Event.ID_COLUMN}
    WHERE r.${EventTagCrossRef.TAG_ID_COLUMN} = :tagId
        AND e.${Event.ACTIVITY_DATE} >= :now
    ORDER BY e.${Event.ACTIVITY_DATE} ASC
    """)
    fun observeByTag(
        tagId: Long,
        now: Long = System.currentTimeMillis()
    ): Flow<List<EventWithTags>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefs(refs: List<EventTagCrossRef>)

    @Query("DELETE FROM ${EventTagCrossRef.TABLE_NAME} WHERE ${EventTagCrossRef.EVENT_ID_COLUMN} = :eventId")
    suspend fun deleteCrossRefsForEvent(eventId: Long)

    @Transaction
    suspend fun insertEventWithTags(event: Event, tagIds: List<Long>): Long {
        val eventId = insert(event)
        if (eventId > 0 && tagIds.isNotEmpty()) {
            insertCrossRefs(tagIds.map { EventTagCrossRef(eventId = eventId, tagId = it) })
        }
        return eventId
    }

    @Transaction
    @Query("""
    SELECT * FROM ${Event.TABLE_NAME}
    WHERE ${Event.ACTIVITY_CATEGORY} = :category
    ORDER BY ${Event.ACTIVITY_DATE} ASC
    """)
    fun observeByCategoryWithTags(category: String): Flow<List<EventWithTags>>

    @Transaction
    @Query("""
    SELECT * FROM ${Event.TABLE_NAME}
    WHERE ${Event.ACTIVITY_DATE} >= :now
    ORDER BY ${Event.ACTIVITY_DATE} ASC
    """)
    fun observeUpcoming(now: Long = System.currentTimeMillis()): Flow<List<EventWithTags>>

    @Transaction
    @Query("SELECT * FROM ${Event.TABLE_NAME} ORDER BY ${Event.ACTIVITY_DATE} ASC")
    fun observeAll(): Flow<List<EventWithTags>>

    @Query("""
    SELECT * FROM ${Event.TABLE_NAME}
    WHERE ${Event.CREATED_BY_LOGIN} = :login
    ORDER BY ${Event.ACTIVITY_DATE} ASC
    """)
    fun observeCreatedByUser(login: String?): Flow<List<Event>>
}

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: Tag): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(tags: List<Tag>)

    @Query("SELECT * FROM ${Tag.TABLE_NAME} WHERE ${Tag.NAME_COLUMN} = :name LIMIT 1")
    suspend fun findByName(name: String): Tag?

    @Query("SELECT * FROM ${Tag.TABLE_NAME} ORDER BY ${Tag.NAME_COLUMN} ASC")
    fun observeAll(): Flow<List<Tag>>
}