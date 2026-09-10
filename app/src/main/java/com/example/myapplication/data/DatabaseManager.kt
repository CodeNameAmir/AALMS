package com.example.myapplication.data

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow

class DatabaseManager(
    private val context: Context,
    private val roomDao: LogDao
) {
    val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun triggerRefresh() {
        refreshTrigger.tryEmit(Unit)
    }
}
