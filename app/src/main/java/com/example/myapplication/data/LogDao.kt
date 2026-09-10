package com.example.myapplication.data

import androidx.room.*
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.LogEntry
import com.example.myapplication.data.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Dao
interface LogDao {
    // --- Users ---
    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Long): User?

    @Insert
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User): Unit

    // --- Categories ---
    @Query("SELECT * FROM categories WHERE userId = :userId OR userId = 0 ORDER BY name ASC")
    fun getAllCategories(userId: Long): Flow<List<Category>>

    @Insert
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    // --- Logs ---
    @Query("""
        SELECT logs.*, categories.name as categoryName, categories.color as categoryColor 
        FROM logs 
        JOIN categories ON logs.categoryId = categories.id 
        WHERE logs.userId = :userId
        AND (:search = '' OR message LIKE '%' || :search || '%')
        AND (:level = '' OR level = :level)
        AND (:categoryId = 0 OR categoryId = :categoryId)
        AND (:startDate = 0 OR logs.createdAt >= :startDate)
        AND (:endDate = 0 OR logs.createdAt <= :endDate)
        AND (:pinned = 0 OR pinned = 1)
        AND (:hasFile = 0 OR attachmentPath IS NOT NULL)
        ORDER BY pinned DESC, logs.id DESC
    """)
    fun getFilteredLogs(
        userId: Long, 
        search: String, 
        level: String, 
        categoryId: Long,
        pinned: Boolean = false,
        hasFile: Boolean = false,
        startDate: Long = 0,
        endDate: Long = 0
    ): Flow<List<LogWithCategory>>

    @Insert
    suspend fun insertLog(log: LogEntry): Long

    @Update
    suspend fun updateLog(log: LogEntry)

    @Delete
    suspend fun deleteLog(log: LogEntry)

    @Query("UPDATE logs SET pinned = NOT pinned WHERE id = :id")
    suspend fun togglePin(id: Long)

    @Query("SELECT * FROM logs WHERE id = :id")
    suspend fun getLogById(id: Long): LogEntry?

    // --- Stats ---
    @Query("SELECT COUNT(*) FROM logs WHERE userId = :userId")
    fun getLogCount(userId: Long): Flow<Int>

    @Query("SELECT level, COUNT(*) as count FROM logs WHERE userId = :userId GROUP BY level")
    fun getLogCountsByLevel(userId: Long): Flow<List<LevelCount>>

    @Query("""
        SELECT categoryId, categories.name as categoryName, categories.color as categoryColor, COUNT(*) as count 
        FROM logs 
        JOIN categories ON logs.categoryId = categories.id 
        WHERE logs.userId = :userId
        GROUP BY categoryId
    """)
    fun getLogCountsByCategory(userId: Long): Flow<List<CategoryCount>>

    @Query("""
        SELECT date(createdAt / 1000, 'unixepoch') as day, COUNT(*) as count 
        FROM logs 
        WHERE userId = :userId
        GROUP BY day 
        ORDER BY day DESC 
        LIMIT 30
    """)
    fun getLogCountsByDay(userId: Long): Flow<List<DayCount>>
}

@Serializable
data class LogWithCategory(
    @Embedded val log: LogEntry,
    val categoryName: String,
    val categoryColor: String
)

@Serializable
data class LevelCount(
    val level: String,
    val count: Int
)

@Serializable
data class CategoryCount(
    val categoryId: Long,
    val categoryName: String,
    val categoryColor: String,
    val count: Int
)

@Serializable
data class DayCount(
    val day: String,
    val count: Int
)
