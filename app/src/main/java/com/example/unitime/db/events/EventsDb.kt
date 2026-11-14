package com.example.unitime.db.events

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.example.unitime.db.tags.Tag

@Database(
    entities = [
        Event::class,
        EventTagCrossRef::class,
        Tag::class
    ],
    version = 4
)
abstract class EventsDb : RoomDatabase() {
    abstract fun getEventsDao(): EventsDao
    abstract fun getTagDao(): TagDao
}

@Dao
interface EventsDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: Event): Long

    @Update suspend fun update(event: Event)
    @Delete suspend fun delete(event: Event)

    @Query("SELECT * FROM ${Event.TABLE_NAME} WHERE ${Event.ID_COLUMN} = :id")
    suspend fun getById(id: Long): Event?

    @Transaction
    @Query("""
        SELECT * FROM ${Event.TABLE_NAME}
        WHERE ${Event.ID_COLUMN} = :id
    """)
    suspend fun getWithTags(id: Long): EventWithTags?

    @Transaction
    @Query("SELECT * FROM ${Event.TABLE_NAME} ORDER BY ${Event.ACTIVITY_DATE} ASC")
    fun observeAllWithTags(): Flow<List<EventWithTags>>

    @Transaction
    @Query("""
        SELECT * FROM ${Event.TABLE_NAME}
        WHERE ${Event.ACTIVITY_DATE} >= :now
        ORDER BY ${Event.ACTIVITY_DATE} ASC
    """)
    fun observeUpcomingWithTags(now: Long = System.currentTimeMillis()): Flow<List<EventWithTags>>

    // * «активности с конкретным тегом»
    @Transaction
    @Query("""
        SELECT e.* FROM ${Event.TABLE_NAME} e
        INNER JOIN ${EventTagCrossRef.TABLE_NAME} r
            ON r.${EventTagCrossRef.EVENT_ID_COLUMN} = e.${Event.ID_COLUMN}
        WHERE r.${EventTagCrossRef.TAG_ID_COLUMN} = :tagId
        ORDER BY e.${Event.ACTIVITY_DATE} ASC
    """)
    fun observeByTag(tagId: Long): Flow<List<EventWithTags>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefs(refs: List<EventTagCrossRef>)

    @Query("DELETE FROM ${EventTagCrossRef.TABLE_NAME} WHERE ${EventTagCrossRef.EVENT_ID_COLUMN} = :eventId")
    suspend fun deleteCrossRefsForEvent(eventId: Long)

    @Transaction
    suspend fun insertEventWithTags(event: Event, tagIds: List<Long>): Long {
        val eventId = insert(event)
        if (tagIds.isNotEmpty()) {
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
    fun observeUpcoming(
        now: Long = System.currentTimeMillis()
    ): Flow<List<EventWithTags>>
}

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: Tag): Long // * вернёт -1, если такой name уже есть

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(tags: List<Tag>): List<Long>

    @Query("SELECT * FROM ${Tag.TABLE_NAME} WHERE ${Tag.NAME_COLUMN} = :name LIMIT 1")
    suspend fun findByName(name: String): Tag?

    @Query("SELECT * FROM ${Tag.TABLE_NAME} ORDER BY ${Tag.NAME_COLUMN} ASC")
    fun observeAll(): Flow<List<Tag>>
}