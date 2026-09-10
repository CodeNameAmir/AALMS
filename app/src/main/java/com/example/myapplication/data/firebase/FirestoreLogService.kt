package com.example.myapplication.data.firebase

import android.util.Log
import com.example.myapplication.data.CategoryCount
import com.example.myapplication.data.DayCount
import com.example.myapplication.data.LevelCount
import com.example.myapplication.data.LogWithCategory
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.LogEntry
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class FirestoreLogService {
    private val TAG = "FirestoreLogService"

    private fun getFirestore(): FirebaseFirestore? = FirebaseManager.getFirestore()

    private fun getUserDocPath(firebaseUid: String, localUserId: Long): String {
        return if (firebaseUid.isNotBlank()) {
            "users/$firebaseUid"
        } else {
            "users/user_$localUserId"
        }
    }

    fun getAllCategories(localUserId: Long, firebaseUid: String): Flow<List<Category>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            trySend(getDefaultCategories(localUserId))
            awaitClose { }
            return@callbackFlow
        }

        val userPath = getUserDocPath(firebaseUid, localUserId)
        val categoriesCol = firestore.collection("$userPath/categories")

        val listener: ListenerRegistration = categoriesCol.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Categories snapshot error", error)
                trySend(getDefaultCategories(localUserId))
                return@addSnapshotListener
            }

            if (snapshot == null || snapshot.isEmpty) {
                // Auto seed default categories
                seedDefaultCategories(localUserId, firebaseUid)
                trySend(getDefaultCategories(localUserId))
            } else {
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L
                        val name = doc.getString("name") ?: "General"
                        val color = doc.getString("color") ?: "#3b82f6"
                        val uId = doc.getLong("userId") ?: localUserId
                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        Category(id = id, name = name, color = color, userId = uId, createdAt = createdAt)
                    } catch (e: Exception) {
                        null
                    }
                }.sortedBy { it.name }
                trySend(list)
            }
        }

        awaitClose { listener.remove() }
    }

    private fun getDefaultCategories(userId: Long): List<Category> {
        return listOf(
            Category(id = 1L, name = "General", color = "#3b82f6", userId = userId),
            Category(id = 2L, name = "Network", color = "#10b981", userId = userId),
            Category(id = 3L, name = "Database", color = "#8b5cf6", userId = userId),
            Category(id = 4L, name = "UI", color = "#ec4899", userId = userId),
            Category(id = 5L, name = "Security", color = "#ef4444", userId = userId),
            Category(id = 6L, name = "Server", color = "#f59e0b", userId = userId)
        )
    }

    private fun seedDefaultCategories(localUserId: Long, firebaseUid: String) {
        val firestore = getFirestore() ?: return
        val userPath = getUserDocPath(firebaseUid, localUserId)
        val categoriesCol = firestore.collection("$userPath/categories")

        val defaults = getDefaultCategories(localUserId)
        for (cat in defaults) {
            val map = hashMapOf(
                "id" to cat.id,
                "name" to cat.name,
                "color" to cat.color,
                "userId" to cat.userId,
                "createdAt" to cat.createdAt
            )
            categoriesCol.document(cat.id.toString()).set(map, SetOptions.merge())
        }
    }

    fun getFilteredLogs(
        localUserId: Long,
        firebaseUid: String,
        search: String,
        level: String,
        categoryId: Long,
        pinned: Boolean = false,
        hasFile: Boolean = false,
        startDate: Long = 0,
        endDate: Long = 0
    ): Flow<List<LogWithCategory>> = callbackFlow {
        val firestore = getFirestore()
        if (firestore == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val userPath = getUserDocPath(firebaseUid, localUserId)
        val logsCol = firestore.collection("$userPath/logs")

        val listener: ListenerRegistration = logsCol.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Logs snapshot error", error)
                trySend(emptyList())
                return@addSnapshotListener
            }

            if (snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            val items = snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L
                    val message = doc.getString("message") ?: ""
                    val lvl = doc.getString("level") ?: "INFO"
                    val catId = doc.getLong("categoryId") ?: 1L
                    val catName = doc.getString("categoryName") ?: "General"
                    val catColor = doc.getString("categoryColor") ?: "#3b82f6"
                    val isPinned = doc.getBoolean("pinned") ?: false
                    val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    val updatedAt = doc.getLong("updatedAt") ?: createdAt
                    val attName = doc.getString("attachmentName")
                    val attPath = doc.getString("attachmentPath")
                    val attSize = doc.getLong("attachmentSize")
                    val uId = doc.getLong("userId") ?: localUserId

                    val logEntry = LogEntry(
                        id = id,
                        message = message,
                        level = lvl,
                        categoryId = catId,
                        userId = uId,
                        pinned = isPinned,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        attachmentName = attName,
                        attachmentPath = attPath,
                        attachmentSize = attSize
                    )

                    LogWithCategory(
                        log = logEntry,
                        categoryName = catName,
                        categoryColor = catColor
                    )
                } catch (e: Exception) {
                    null
                }
            }

            // Filter in memory for maximum flexibility & responsiveness
            val filtered = items.filter { item ->
                val matchesSearch = search.isBlank() || item.log.message.contains(search, ignoreCase = true)
                val matchesLevel = level.isBlank() || item.log.level.equals(level, ignoreCase = true)
                val matchesCat = categoryId == 0L || item.log.categoryId == categoryId
                val matchesPinned = !pinned || item.log.pinned
                val matchesFile = !hasFile || !item.log.attachmentPath.isNullOrBlank()
                val matchesStart = startDate == 0L || item.log.createdAt >= startDate
                val matchesEnd = endDate == 0L || item.log.createdAt <= endDate

                matchesSearch && matchesLevel && matchesCat && matchesPinned && matchesFile && matchesStart && matchesEnd
            }.sortedWith(
                compareByDescending<LogWithCategory> { it.log.pinned }
                    .thenByDescending { it.log.createdAt }
            )

            trySend(filtered)
        }

        awaitClose { listener.remove() }
    }

    suspend fun insertLog(log: LogEntry, categoryName: String, categoryColor: String, localUserId: Long, firebaseUid: String): Long {
        val firestore = getFirestore() ?: return log.id
        val userPath = getUserDocPath(firebaseUid, localUserId)
        val finalId = if (log.id <= 0L) System.currentTimeMillis() else log.id

        val docData = hashMapOf(
            "id" to finalId,
            "message" to log.message,
            "level" to log.level,
            "categoryId" to log.categoryId,
            "categoryName" to categoryName,
            "categoryColor" to categoryColor,
            "userId" to localUserId,
            "firebaseUid" to firebaseUid,
            "pinned" to log.pinned,
            "createdAt" to log.createdAt,
            "updatedAt" to log.updatedAt,
            "attachmentName" to log.attachmentName,
            "attachmentPath" to log.attachmentPath,
            "attachmentSize" to log.attachmentSize
        )

        firestore.collection("$userPath/logs").document(finalId.toString()).set(docData, SetOptions.merge()).await()
        return finalId
    }

    suspend fun updateLog(log: LogEntry, categoryName: String, categoryColor: String, localUserId: Long, firebaseUid: String) {
        val firestore = getFirestore() ?: return
        val userPath = getUserDocPath(firebaseUid, localUserId)

        val docData = hashMapOf(
            "id" to log.id,
            "message" to log.message,
            "level" to log.level,
            "categoryId" to log.categoryId,
            "categoryName" to categoryName,
            "categoryColor" to categoryColor,
            "pinned" to log.pinned,
            "updatedAt" to System.currentTimeMillis(),
            "attachmentName" to log.attachmentName,
            "attachmentPath" to log.attachmentPath,
            "attachmentSize" to log.attachmentSize
        )

        firestore.collection("$userPath/logs").document(log.id.toString()).update(docData as Map<String, Any>).await()
    }

    suspend fun deleteLogById(id: Long, localUserId: Long, firebaseUid: String) {
        val firestore = getFirestore() ?: return
        val userPath = getUserDocPath(firebaseUid, localUserId)
        firestore.collection("$userPath/logs").document(id.toString()).delete().await()
    }

    suspend fun getLogById(id: Long, localUserId: Long, firebaseUid: String): LogEntry? {
        val firestore = getFirestore() ?: return null
        val userPath = getUserDocPath(firebaseUid, localUserId)
        val doc = firestore.collection("$userPath/logs").document(id.toString()).get().await()
        if (!doc.exists()) return null
        return try {
            val logId = doc.getLong("id") ?: doc.id.toLongOrNull() ?: id
            val message = doc.getString("message") ?: ""
            val lvl = doc.getString("level") ?: "INFO"
            val catId = doc.getLong("categoryId") ?: 1L
            val isPinned = doc.getBoolean("pinned") ?: false
            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
            val updatedAt = doc.getLong("updatedAt") ?: createdAt
            val attName = doc.getString("attachmentName")
            val attPath = doc.getString("attachmentPath")
            val attSize = doc.getLong("attachmentSize")
            val uId = doc.getLong("userId") ?: localUserId

            LogEntry(
                id = logId,
                message = message,
                level = lvl,
                categoryId = catId,
                userId = uId,
                pinned = isPinned,
                createdAt = createdAt,
                updatedAt = updatedAt,
                attachmentName = attName,
                attachmentPath = attPath,
                attachmentSize = attSize
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCategoryById(id: Long, localUserId: Long, firebaseUid: String): Category? {
        val firestore = getFirestore() ?: return null
        val userPath = getUserDocPath(firebaseUid, localUserId)
        val doc = firestore.collection("$userPath/categories").document(id.toString()).get().await()
        if (!doc.exists()) return null
        return try {
            val catId = doc.getLong("id") ?: doc.id.toLongOrNull() ?: id
            val name = doc.getString("name") ?: "General"
            val color = doc.getString("color") ?: "#3b82f6"
            val uId = doc.getLong("userId") ?: localUserId
            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
            Category(id = catId, name = name, color = color, userId = uId, createdAt = createdAt)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun togglePin(id: Long, localUserId: Long, firebaseUid: String) {
        val firestore = getFirestore() ?: return
        val userPath = getUserDocPath(firebaseUid, localUserId)
        val docRef = firestore.collection("$userPath/logs").document(id.toString())
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            val currentPinned = snapshot.getBoolean("pinned") ?: false
            docRef.update("pinned", !currentPinned).await()
        }
    }

    suspend fun insertCategory(category: Category, localUserId: Long, firebaseUid: String): Long {
        val firestore = getFirestore() ?: return category.id
        val userPath = getUserDocPath(firebaseUid, localUserId)
        val finalId = if (category.id <= 0L) System.currentTimeMillis() else category.id

        val docData = hashMapOf(
            "id" to finalId,
            "name" to category.name,
            "color" to category.color,
            "userId" to localUserId,
            "firebaseUid" to firebaseUid,
            "createdAt" to category.createdAt
        )

        firestore.collection("$userPath/categories").document(finalId.toString()).set(docData, SetOptions.merge()).await()
        return finalId
    }

    suspend fun updateCategory(category: Category, localUserId: Long, firebaseUid: String) {
        val firestore = getFirestore() ?: return
        val userPath = getUserDocPath(firebaseUid, localUserId)
        val docData = hashMapOf(
            "name" to category.name,
            "color" to category.color
        )
        firestore.collection("$userPath/categories").document(category.id.toString()).update(docData as Map<String, Any>).await()
    }

    suspend fun deleteCategory(category: Category, localUserId: Long, firebaseUid: String) {
        val firestore = getFirestore() ?: return
        val userPath = getUserDocPath(firebaseUid, localUserId)
        firestore.collection("$userPath/categories").document(category.id.toString()).delete().await()
    }

    fun getTotalLogCount(localUserId: Long, firebaseUid: String): Flow<Int> {
        return getFilteredLogs(localUserId, firebaseUid, "", "", 0L).map { it.size }
    }

    fun getLogCountsByLevel(localUserId: Long, firebaseUid: String): Flow<List<LevelCount>> {
        return getFilteredLogs(localUserId, firebaseUid, "", "", 0L).map { logs ->
            logs.groupBy { it.log.level }
                .map { (lvl, list) -> LevelCount(level = lvl, count = list.size) }
        }
    }

    fun getLogCountsByCategory(localUserId: Long, firebaseUid: String): Flow<List<CategoryCount>> {
        return getFilteredLogs(localUserId, firebaseUid, "", "", 0L).map { logs ->
            logs.groupBy { it.log.categoryId }
                .map { (catId, list) ->
                    val first = list.first()
                    CategoryCount(
                        categoryId = catId,
                        categoryName = first.categoryName,
                        categoryColor = first.categoryColor,
                        count = list.size
                    )
                }
        }
    }

    fun getLogCountsByDay(localUserId: Long, firebaseUid: String): Flow<List<DayCount>> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
        return getFilteredLogs(localUserId, firebaseUid, "", "", 0L).map { logs ->
            logs.groupBy { sdf.format(Date(it.log.createdAt)) }
                .map { (day, list) -> DayCount(day = day, count = list.size) }
                .sortedByDescending { it.day }
                .take(30)
        }
    }
}
