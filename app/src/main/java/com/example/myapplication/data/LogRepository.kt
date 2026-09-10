package com.example.myapplication.data

import com.example.myapplication.data.firebase.FirestoreLogService
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.DatabaseMode
import com.example.myapplication.data.model.LogEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class LogRepository(
    val logDao: LogDao,
    val databaseManager: DatabaseManager,
    val firestoreLogService: FirestoreLogService,
    val userManager: UserManager
) {

    val databaseMode: Flow<DatabaseMode> = userManager.databaseMode

    fun getAllCategories(userId: Long): Flow<List<Category>> {
        return combine(userManager.databaseMode, userManager.firebaseUid) { mode, uid ->
            Pair(mode, uid)
        }.flatMapLatest { (mode, uid) ->
            if (mode == DatabaseMode.GOOGLE_CLOUD) {
                firestoreLogService.getAllCategories(userId, uid)
            } else {
                logDao.getAllCategories(userId)
            }
        }
    }

    fun getTotalLogCount(userId: Long): Flow<Int> {
        return combine(userManager.databaseMode, userManager.firebaseUid) { mode, uid ->
            Pair(mode, uid)
        }.flatMapLatest { (mode, uid) ->
            if (mode == DatabaseMode.GOOGLE_CLOUD) {
                firestoreLogService.getTotalLogCount(userId, uid)
            } else {
                logDao.getLogCount(userId)
            }
        }
    }

    fun getLogCountsByLevel(userId: Long): Flow<List<LevelCount>> {
        return combine(userManager.databaseMode, userManager.firebaseUid) { mode, uid ->
            Pair(mode, uid)
        }.flatMapLatest { (mode, uid) ->
            if (mode == DatabaseMode.GOOGLE_CLOUD) {
                firestoreLogService.getLogCountsByLevel(userId, uid)
            } else {
                logDao.getLogCountsByLevel(userId)
            }
        }
    }

    fun getLogCountsByCategory(userId: Long): Flow<List<CategoryCount>> {
        return combine(userManager.databaseMode, userManager.firebaseUid) { mode, uid ->
            Pair(mode, uid)
        }.flatMapLatest { (mode, uid) ->
            if (mode == DatabaseMode.GOOGLE_CLOUD) {
                firestoreLogService.getLogCountsByCategory(userId, uid)
            } else {
                logDao.getLogCountsByCategory(userId)
            }
        }
    }

    fun getLogCountsByDay(userId: Long): Flow<List<DayCount>> {
        return combine(userManager.databaseMode, userManager.firebaseUid) { mode, uid ->
            Pair(mode, uid)
        }.flatMapLatest { (mode, uid) ->
            if (mode == DatabaseMode.GOOGLE_CLOUD) {
                firestoreLogService.getLogCountsByDay(userId, uid)
            } else {
                logDao.getLogCountsByDay(userId)
            }
        }
    }

    fun getFilteredLogs(
        userId: Long, 
        search: String, 
        level: String, 
        categoryId: Long, 
        pinned: Boolean = false,
        hasFile: Boolean = false,
        startDate: Long = 0, 
        endDate: Long = 0
    ): Flow<List<LogWithCategory>> {
        return combine(userManager.databaseMode, userManager.firebaseUid) { mode, uid ->
            Pair(mode, uid)
        }.flatMapLatest { (mode, uid) ->
            if (mode == DatabaseMode.GOOGLE_CLOUD) {
                firestoreLogService.getFilteredLogs(
                    userId, uid, search, level, categoryId, pinned, hasFile, startDate, endDate
                )
            } else {
                logDao.getFilteredLogs(userId, search, level, categoryId, pinned, hasFile, startDate, endDate)
            }
        }
    }

    suspend fun insertLog(log: LogEntry) {
        val mode = userManager.databaseMode.first()
        val uid = userManager.firebaseUid.first()
        if (mode == DatabaseMode.GOOGLE_CLOUD) {
            val categories = firestoreLogService.getAllCategories(log.userId, uid).first()
            val cat = categories.find { it.id == log.categoryId }
            val catName = cat?.name ?: "General"
            val catColor = cat?.color ?: "#3b82f6"
            firestoreLogService.insertLog(log, catName, catColor, log.userId, uid)
        } else {
            logDao.insertLog(log)
        }
    }

    suspend fun insertLogAndGetId(log: LogEntry): Long {
        val mode = userManager.databaseMode.first()
        val uid = userManager.firebaseUid.first()
        return if (mode == DatabaseMode.GOOGLE_CLOUD) {
            val categories = firestoreLogService.getAllCategories(log.userId, uid).first()
            val cat = categories.find { it.id == log.categoryId }
            val catName = cat?.name ?: "General"
            val catColor = cat?.color ?: "#3b82f6"
            firestoreLogService.insertLog(log, catName, catColor, log.userId, uid)
        } else {
            logDao.insertLog(log)
        }
    }

    suspend fun updateLog(log: LogEntry) {
        val mode = userManager.databaseMode.first()
        val uid = userManager.firebaseUid.first()
        if (mode == DatabaseMode.GOOGLE_CLOUD) {
            val categories = firestoreLogService.getAllCategories(log.userId, uid).first()
            val cat = categories.find { it.id == log.categoryId }
            val catName = cat?.name ?: "General"
            val catColor = cat?.color ?: "#3b82f6"
            firestoreLogService.updateLog(log, catName, catColor, log.userId, uid)
        } else {
            logDao.updateLog(log)
        }
    }

    suspend fun deleteLog(log: LogEntry) {
        val mode = userManager.databaseMode.first()
        val uid = userManager.firebaseUid.first()
        if (mode == DatabaseMode.GOOGLE_CLOUD) {
            firestoreLogService.deleteLogById(log.id, log.userId, uid)
        } else {
            logDao.deleteLog(log)
        }
    }

    suspend fun deleteLogById(id: Long) {
        val mode = userManager.databaseMode.first()
        val userId = userManager.loggedInUserId.first() ?: 0L
        val uid = userManager.firebaseUid.first()
        if (mode == DatabaseMode.GOOGLE_CLOUD) {
            firestoreLogService.deleteLogById(id, userId, uid)
        } else {
            logDao.getLogById(id)?.let { logDao.deleteLog(it) }
        }
    }

    suspend fun getLogById(id: Long, userId: Long): LogEntry? {
        val mode = userManager.databaseMode.first()
        val uid = userManager.firebaseUid.first()
        return if (mode == DatabaseMode.GOOGLE_CLOUD) {
            firestoreLogService.getLogById(id, userId, uid)
        } else {
            logDao.getLogById(id)
        }
    }

    suspend fun getCategoryById(id: Long, userId: Long): Category? {
        val mode = userManager.databaseMode.first()
        val uid = userManager.firebaseUid.first()
        return if (mode == DatabaseMode.GOOGLE_CLOUD) {
            firestoreLogService.getCategoryById(id, userId, uid)
        } else {
            logDao.getCategoryById(id)
        }
    }

    suspend fun togglePin(id: Long) {
        val mode = userManager.databaseMode.first()
        val userId = userManager.loggedInUserId.first() ?: 0L
        val uid = userManager.firebaseUid.first()
        if (mode == DatabaseMode.GOOGLE_CLOUD) {
            firestoreLogService.togglePin(id, userId, uid)
        } else {
            logDao.togglePin(id)
        }
    }

    suspend fun insertCategory(category: Category) {
        val mode = userManager.databaseMode.first()
        val uid = userManager.firebaseUid.first()
        if (mode == DatabaseMode.GOOGLE_CLOUD) {
            firestoreLogService.insertCategory(category, category.userId, uid)
        } else {
            logDao.insertCategory(category)
        }
    }

    suspend fun insertCategoryAndGetId(category: Category): Long {
        val mode = userManager.databaseMode.first()
        val uid = userManager.firebaseUid.first()
        return if (mode == DatabaseMode.GOOGLE_CLOUD) {
            firestoreLogService.insertCategory(category, category.userId, uid)
        } else {
            logDao.insertCategory(category)
        }
    }

    suspend fun updateCategory(category: Category) {
        val mode = userManager.databaseMode.first()
        val uid = userManager.firebaseUid.first()
        if (mode == DatabaseMode.GOOGLE_CLOUD) {
            firestoreLogService.updateCategory(category, category.userId, uid)
        } else {
            logDao.updateCategory(category)
        }
    }

    suspend fun deleteCategory(category: Category) {
        val mode = userManager.databaseMode.first()
        val uid = userManager.firebaseUid.first()
        if (mode == DatabaseMode.GOOGLE_CLOUD) {
            firestoreLogService.deleteCategory(category, category.userId, uid)
        } else {
            logDao.deleteCategory(category)
        }
    }

    suspend fun switchDatabaseMode(mode: DatabaseMode) {
        userManager.setDatabaseMode(mode)
    }

    suspend fun syncLocalToGoogleCloud(userId: Long): Int {
        val uid = userManager.firebaseUid.first()
        val localLogs = logDao.getFilteredLogs(userId, "", "", 0).first()
        val localCategories = logDao.getAllCategories(userId).first()

        for (cat in localCategories) {
            firestoreLogService.insertCategory(cat, userId, uid)
        }

        for (item in localLogs) {
            firestoreLogService.insertLog(
                log = item.log,
                categoryName = item.categoryName,
                categoryColor = item.categoryColor,
                localUserId = userId,
                firebaseUid = uid
            )
        }
        return localLogs.size
    }

    suspend fun syncGoogleCloudToLocal(userId: Long): Int {
        val uid = userManager.firebaseUid.first()
        val cloudLogs = firestoreLogService.getFilteredLogs(userId, uid, "", "", 0L).first()
        val cloudCategories = firestoreLogService.getAllCategories(userId, uid).first()

        for (cat in cloudCategories) {
            val existing = logDao.getCategoryById(cat.id)
            if (existing == null) {
                logDao.insertCategory(cat.copy(userId = userId))
            } else {
                logDao.updateCategory(cat.copy(userId = userId))
            }
        }

        for (item in cloudLogs) {
            val existingLog = logDao.getLogById(item.log.id)
            if (existingLog == null) {
                logDao.insertLog(item.log.copy(userId = userId))
            } else {
                logDao.updateLog(item.log.copy(userId = userId))
            }
        }
        return cloudLogs.size
    }

    // --- Mode-explicit methods (for Web Sessions and direct callers) ---

    fun getFilteredLogsForMode(
        userId: Long,
        mode: DatabaseMode,
        search: String = "",
        level: String = "",
        categoryId: Long = 0L,
        pinned: Boolean = false,
        hasFile: Boolean = false,
        startDate: Long = 0L,
        endDate: Long = 0L
    ): Flow<List<LogWithCategory>> {
        return if (mode == DatabaseMode.GOOGLE_CLOUD) {
            userManager.firebaseUid.flatMapLatest { uid ->
                firestoreLogService.getFilteredLogs(
                    userId, uid, search, level, categoryId, pinned, hasFile, startDate, endDate
                )
            }
        } else {
            logDao.getFilteredLogs(userId, search, level, categoryId, pinned, hasFile, startDate, endDate)
        }
    }

    fun getAllCategoriesForMode(userId: Long, mode: DatabaseMode): Flow<List<Category>> {
        return if (mode == DatabaseMode.GOOGLE_CLOUD) {
            userManager.firebaseUid.flatMapLatest { uid ->
                firestoreLogService.getAllCategories(userId, uid)
            }
        } else {
            logDao.getAllCategories(userId)
        }
    }

    fun getTotalLogCountForMode(userId: Long, mode: DatabaseMode): Flow<Int> {
        return if (mode == DatabaseMode.GOOGLE_CLOUD) {
            userManager.firebaseUid.flatMapLatest { uid ->
                firestoreLogService.getTotalLogCount(userId, uid)
            }
        } else {
            logDao.getLogCount(userId)
        }
    }

    fun getLogCountsByLevelForMode(userId: Long, mode: DatabaseMode): Flow<List<LevelCount>> {
        return if (mode == DatabaseMode.GOOGLE_CLOUD) {
            userManager.firebaseUid.flatMapLatest { uid ->
                firestoreLogService.getLogCountsByLevel(userId, uid)
            }
        } else {
            logDao.getLogCountsByLevel(userId)
        }
    }

    fun getLogCountsByCategoryForMode(userId: Long, mode: DatabaseMode): Flow<List<CategoryCount>> {
        return if (mode == DatabaseMode.GOOGLE_CLOUD) {
            userManager.firebaseUid.flatMapLatest { uid ->
                firestoreLogService.getLogCountsByCategory(userId, uid)
            }
        } else {
            logDao.getLogCountsByCategory(userId)
        }
    }

    fun getLogCountsByDayForMode(userId: Long, mode: DatabaseMode): Flow<List<DayCount>> {
        return if (mode == DatabaseMode.GOOGLE_CLOUD) {
            userManager.firebaseUid.flatMapLatest { uid ->
                firestoreLogService.getLogCountsByDay(userId, uid)
            }
        } else {
            logDao.getLogCountsByDay(userId)
        }
    }

    suspend fun insertLogForMode(log: LogEntry, mode: DatabaseMode): Long {
        val uid = userManager.firebaseUid.first()
        return if (mode == DatabaseMode.GOOGLE_CLOUD) {
            val categories = firestoreLogService.getAllCategories(log.userId, uid).first()
            val cat = categories.find { it.id == log.categoryId }
            val catName = cat?.name ?: "General"
            val catColor = cat?.color ?: "#3b82f6"
            firestoreLogService.insertLog(log, catName, catColor, log.userId, uid)
        } else {
            logDao.insertLog(log)
        }
    }

    suspend fun updateLogForMode(log: LogEntry, mode: DatabaseMode) {
        val uid = userManager.firebaseUid.first()
        if (mode == DatabaseMode.GOOGLE_CLOUD) {
            val categories = firestoreLogService.getAllCategories(log.userId, uid).first()
            val cat = categories.find { it.id == log.categoryId }
            val catName = cat?.name ?: "General"
            val catColor = cat?.color ?: "#3b82f6"
            firestoreLogService.updateLog(log, catName, catColor, log.userId, uid)
        } else {
            logDao.updateLog(log)
        }
    }

    suspend fun deleteLogByIdForMode(id: Long, userId: Long, mode: DatabaseMode) {
        val uid = userManager.firebaseUid.first()
        if (mode == DatabaseMode.GOOGLE_CLOUD) {
            firestoreLogService.deleteLogById(id, userId, uid)
        } else {
            logDao.getLogById(id)?.let { logDao.deleteLog(it) }
        }
    }

    suspend fun getLogByIdForMode(id: Long, userId: Long, mode: DatabaseMode): LogEntry? {
        val uid = userManager.firebaseUid.first()
        return if (mode == DatabaseMode.GOOGLE_CLOUD) {
            firestoreLogService.getLogById(id, userId, uid)
        } else {
            logDao.getLogById(id)
        }
    }

    suspend fun getCategoryByIdForMode(id: Long, userId: Long, mode: DatabaseMode): Category? {
        val uid = userManager.firebaseUid.first()
        return if (mode == DatabaseMode.GOOGLE_CLOUD) {
            firestoreLogService.getCategoryById(id, userId, uid)
        } else {
            logDao.getCategoryById(id)
        }
    }

    suspend fun togglePinForMode(id: Long, userId: Long, mode: DatabaseMode) {
        val uid = userManager.firebaseUid.first()
        if (mode == DatabaseMode.GOOGLE_CLOUD) {
            firestoreLogService.togglePin(id, userId, uid)
        } else {
            logDao.togglePin(id)
        }
    }

    suspend fun insertCategoryForMode(category: Category, mode: DatabaseMode): Long {
        val uid = userManager.firebaseUid.first()
        return if (mode == DatabaseMode.GOOGLE_CLOUD) {
            firestoreLogService.insertCategory(category, category.userId, uid)
        } else {
            logDao.insertCategory(category)
        }
    }

    suspend fun deleteCategoryForMode(category: Category, mode: DatabaseMode) {
        val uid = userManager.firebaseUid.first()
        if (mode == DatabaseMode.GOOGLE_CLOUD) {
            firestoreLogService.deleteCategory(category, category.userId, uid)
        } else {
            logDao.deleteCategory(category)
        }
    }
}
