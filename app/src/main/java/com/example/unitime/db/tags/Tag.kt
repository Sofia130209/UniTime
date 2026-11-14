package com.example.unitime.db.tags

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = Tag.TABLE_NAME,
    indices = [Index(value = [Tag.NAME_COLUMN], unique = true)]
)
data class Tag(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID_COLUMN) val id: Long = 0,
    @ColumnInfo(name = NAME_COLUMN) val name: String
) {
    companion object {
        const val TABLE_NAME = "tags"
        const val ID_COLUMN = "id"
        const val NAME_COLUMN = "name"
    }
}