package com.example.myapplication.ui.logs

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.LogRepository
import com.example.myapplication.data.LogWithCategory
import com.example.myapplication.data.UserManager
import com.example.myapplication.data.model.Category
import com.example.myapplication.data.model.LogEntry
import com.example.myapplication.util.ImportExportUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class LogViewModel(
    private val repository: LogRepository,
    private val userManager: UserManager
) : ViewModel() {

    private val _userId = userManager.loggedInUserId.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val userId: StateFlow<Long?> = _userId

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedLevel = MutableStateFlow("")
    val selectedLevel: StateFlow<String> = _selectedLevel

    private val _selectedCategoryId = MutableStateFlow(0L)
    val selectedCategoryId: StateFlow<Long> = _selectedCategoryId

    private val _filterPinned = MutableStateFlow(false)
    val filterPinned: StateFlow<Boolean> = _filterPinned

    private val _filterHasFile = MutableStateFlow(false)
    val filterHasFile: StateFlow<Boolean> = _filterHasFile

    val allCategories: StateFlow<List<Category>> = _userId.flatMapLatest { id ->
        if (id != null) repository.getAllCategories(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredLogs: StateFlow<List<LogWithCategory>> = combine(
        _userId, _searchQuery, _selectedLevel, _selectedCategoryId, _filterPinned, _filterHasFile
    ) { args: Array<Any?> ->
        val id = args[0] as? Long
        val query = args[1] as String
        val level = args[2] as String
        val categoryId = args[3] as Long
        val pinned = args[4] as Boolean
        val hasFile = args[5] as Boolean

        if (id == null) null else FilterData(id, query, level, categoryId, pinned, hasFile)
    }.flatMapLatest { data ->
        if (data == null) flowOf(emptyList())
        else repository.getFilteredLogs(
            data.userId, 
            data.query, 
            data.level, 
            data.categoryId,
            data.pinned,
            data.hasFile
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class FilterData(
        val userId: Long,
        val query: String,
        val level: String,
        val categoryId: Long,
        val pinned: Boolean,
        val hasFile: Boolean
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onLevelSelected(level: String) {
        _selectedLevel.value = level
    }

    fun onCategorySelected(categoryId: Long) {
        _selectedCategoryId.value = categoryId
    }

    fun onFilterPinnedChanged(pinned: Boolean) {
        _filterPinned.value = pinned
    }

    fun onFilterHasFileChanged(hasFile: Boolean) {
        _filterHasFile.value = hasFile
    }

    val databaseMode: StateFlow<com.example.myapplication.data.model.DatabaseMode> = userManager.databaseMode.stateIn(
        viewModelScope, SharingStarted.Eagerly, com.example.myapplication.data.model.DatabaseMode.LOCAL
    )
    val googleUser = userManager.googleUser.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun switchDatabaseMode(mode: com.example.myapplication.data.model.DatabaseMode) {
        viewModelScope.launch {
            repository.switchDatabaseMode(mode)
        }
    }

    fun syncLocalToCloud(context: Context) {
        viewModelScope.launch {
            val userId = _userId.value ?: return@launch
            try {
                val count = repository.syncLocalToGoogleCloud(userId)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Uploaded $count logs to Google Cloud", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Sync error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun syncCloudToLocal(context: Context) {
        viewModelScope.launch {
            val userId = _userId.value ?: return@launch
            try {
                val count = repository.syncGoogleCloudToLocal(userId)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Downloaded $count logs from Google Cloud", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Sync error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedLevel.value = ""
        _selectedCategoryId.value = 0L
        _filterPinned.value = false
        _filterHasFile.value = false
    }

    fun insertLog(log: LogEntry) = viewModelScope.launch {
        repository.insertLog(log)
    }

    fun updateLog(log: LogEntry) = viewModelScope.launch {
        repository.updateLog(log)
    }

    fun deleteLog(log: LogEntry) = viewModelScope.launch {
        repository.deleteLog(log)
    }

    fun togglePin(id: Long) = viewModelScope.launch {
        repository.togglePin(id)
    }

    fun insertCategory(category: Category) = viewModelScope.launch {
        repository.insertCategory(category)
    }

    fun deleteCategory(category: Category) = viewModelScope.launch {
        repository.deleteCategory(category)
    }

    fun logout() = viewModelScope.launch {
        userManager.logout()
    }

    fun exportLogs(context: Context, uri: Uri, format: String) {
        viewModelScope.launch {
            val logs = filteredLogs.value
            withContext(Dispatchers.IO) {
                if (format == "json") {
                    ImportExportUtils.exportToJson(context, uri, logs)
                } else {
                    ImportExportUtils.exportToCsv(context, uri, logs)
                }
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Export complete", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun importLogs(context: Context, uri: Uri, format: String = "json") {
        viewModelScope.launch {
            val userId = _userId.value ?: 0L
            val imported = withContext(Dispatchers.IO) {
                if (format == "json") {
                    ImportExportUtils.importFromJson(context, uri)
                } else {
                    ImportExportUtils.importFromCsv(context, uri)
                }
            }
            if (imported != null) {
                val categories = allCategories.value
                imported.forEach { extLog ->
                    // Resolve category
                    var catId = 0L
                    
                    // 1. Try match by ID if it's one of user's categories
                    if (extLog.categoryId != null && (categories.any { it.id == extLog.categoryId })) {
                        catId = extLog.categoryId
                    }
                    
                    // 2. Try match by name
                    if (catId == 0L && extLog.categoryName != null) {
                        val match = categories.find { it.name.equals(extLog.categoryName, ignoreCase = true) }
                        catId = if (match != null) {
                            match.id
                        } else {
                            // Create new category if it doesn't exist
                            val newCat = Category(
                                name = extLog.categoryName,
                                color = extLog.categoryColor ?: "#3b82f6",
                                userId = userId
                            )
                            repository.insertCategoryAndGetId(newCat)
                        }
                    }
                    
                    // 3. Fallback to General (usually id 1 or first available)
                    if (catId == 0L) {
                        catId = categories.find { it.name.lowercase() == "general" }?.id 
                            ?: categories.firstOrNull()?.id 
                            ?: 1L
                    }

                    val timestamp = ImportExportUtils.parseTimestamp(extLog.createdAt)
                    
                    val log = LogEntry(
                        message = extLog.message,
                        level = extLog.level,
                        categoryId = catId,
                        userId = userId,
                        pinned = extLog.pinned,
                        createdAt = timestamp,
                        updatedAt = ImportExportUtils.parseTimestamp(extLog.updatedAt),
                        attachmentName = extLog.attachmentName,
                        attachmentPath = extLog.attachmentPath,
                        attachmentSize = extLog.attachmentSize
                    )
                    repository.insertLog(log)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Imported ${imported.size} logs", Toast.LENGTH_SHORT).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

class LogViewModelFactory(
    private val repository: LogRepository,
    private val userManager: UserManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LogViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LogViewModel(repository, userManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
