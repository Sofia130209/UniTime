package com.example.unitime.data.managers

import com.example.unitime.data.local.db.auth.UsersDb
import com.example.unitime.data.local.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class UserManager(private val usersDb: UsersDb) {
    private val usersDao = usersDb.getUsersDao()

    // * хеширование пароля (самый простой способ)
    fun hashPassword(password: String) : String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))

        return hashBytes.joinToString("") { byte -> "%02x".format(byte) }
    }
    // * проверка логина на уникальность
    suspend fun isLoginExists(login: String): Boolean = withContext(Dispatchers.IO) {
        val user = usersDao.getUserByLogin(login)

        return@withContext user != null
    }
    // * рег
    suspend fun regUser(user: User): Boolean = withContext(Dispatchers.IO) {
        if (isLoginExists(user.login)) {
            return@withContext false
        }

        val secureUser = user.copy(password = hashPassword(user.password))
        val resId = usersDao.insert(secureUser)
        return@withContext resId >= 0L
    }
    // * лог
    suspend fun logUser(user: User): Boolean = withContext(Dispatchers.IO) {
        val passHash = hashPassword(user.password)
        val foundUser = usersDao.checkUser(user.login, passHash)

        return@withContext foundUser != null
    }
}