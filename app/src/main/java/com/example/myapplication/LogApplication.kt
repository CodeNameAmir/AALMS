package com.example.myapplication

import android.app.Application
import com.example.myapplication.data.DatabaseManager
import com.example.myapplication.data.LogDatabase
import com.example.myapplication.data.LogRepository
import com.example.myapplication.data.UserManager
import com.example.myapplication.data.firebase.FirebaseManager
import com.example.myapplication.data.firebase.FirestoreLogService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class LogApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { LogDatabase.getDatabase(this, applicationScope) }
    val databaseManager by lazy { DatabaseManager(this, database.logDao()) }
    val firestoreLogService by lazy { FirestoreLogService() }
    val userManager by lazy { UserManager(this, database.logDao(), databaseManager) }
    val repository by lazy { LogRepository(database.logDao(), databaseManager, firestoreLogService, userManager) }

    override fun onCreate() {
        super.onCreate()
        FirebaseManager.initialize(this)
    }
}
