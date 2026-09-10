package com.example.myapplication.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.CategoryCount
import com.example.myapplication.data.DayCount
import com.example.myapplication.data.LevelCount
import com.example.myapplication.data.LogRepository
import com.example.myapplication.data.UserManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(
    private val repository: LogRepository,
    private val userManager: UserManager
) : ViewModel() {

    private val _userId = userManager.loggedInUserId.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val databaseMode: StateFlow<com.example.myapplication.data.model.DatabaseMode> = userManager.databaseMode.stateIn(
        viewModelScope, SharingStarted.Eagerly, com.example.myapplication.data.model.DatabaseMode.LOCAL
    )

    val totalLogs: StateFlow<Int> = _userId.flatMapLatest { id ->
        if (id != null) repository.getTotalLogCount(id) else flowOf(0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val logCountsByLevel: StateFlow<List<LevelCount>> = _userId.flatMapLatest { id ->
        if (id != null) repository.getLogCountsByLevel(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logCountsByCategory: StateFlow<List<CategoryCount>> = _userId.flatMapLatest { id ->
        if (id != null) repository.getLogCountsByCategory(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logCountsByDay: StateFlow<List<DayCount>> = _userId.flatMapLatest { id ->
        if (id != null) repository.getLogCountsByDay(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class StatsViewModelFactory(
    private val repository: LogRepository,
    private val userManager: UserManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatsViewModel(repository, userManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
