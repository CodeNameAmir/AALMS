package com.example.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.LogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Category::class, LogEntry::class, com.example.myapplication.data.model.User::class], version = 2, exportSchema = false)
abstract class LogDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var INSTANCE: LogDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): LogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LogDatabase::class.java,
                    "log_database"
                )
                    .fallbackToDestructiveMigration() // Simplified for development
                    .addCallback(LogDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class LogDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.logDao())
                }
            }
        }

        suspend fun populateDatabase(logDao: LogDao) {
            // Seed default categories like in the Go version
            val defaults = listOf(
                Category(name = "General", color = "#3b82f6"),
                Category(name = "System", color = "#10b981"),
                Category(name = "Download", color = "#f59e0b"),
                Category(name = "API", color = "#8b5cf6"),
                Category(name = "Temp", color = "#ef4444")
            )
            for (category in defaults) {
                logDao.insertCategory(category)
            }
        }
    }
}
