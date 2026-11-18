package com.example.unitime.data.local.db.auth

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.unitime.data.local.model.User

@Database(
    entities = [
        User::class,
    ],
    version = 2 // <--- БЫЛО 1, СТАЛО 2
)
abstract class UsersDb : RoomDatabase() {
    abstract fun getUsersDao(): UsersDao
}

@Dao
interface UsersDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)

    @Query("SELECT * FROM users WHERE login = :login LIMIT 1")
    suspend fun getUserByLogin(login: String): User?

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM users
            WHERE login = :login AND id != :userId
        )
    """)
    suspend fun isLoginTaken(login: String, userId: Long): Boolean

    @Query("SELECT * FROM users WHERE login = :login AND password_hash = :passwordHash LIMIT 1")
    suspend fun checkUser(login: String, passwordHash: String): User?
}