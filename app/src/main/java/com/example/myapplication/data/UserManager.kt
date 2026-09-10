package com.example.myapplication.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.myapplication.data.firebase.FirebaseManager
import com.example.myapplication.data.model.DatabaseMode
import com.example.myapplication.data.model.User
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.mindrot.jbcrypt.BCrypt

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

data class GoogleUserData(
    val uid: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?
)

class UserManager(
    private val context: Context,
    private val logDao: LogDao,
    private val databaseManager: DatabaseManager
) {

    private val USER_ID_KEY = longPreferencesKey("user_id")
    private val DATABASE_MODE_KEY = stringPreferencesKey("database_mode")
    private val IS_GOOGLE_AUTH_KEY = booleanPreferencesKey("is_google_auth")
    private val GOOGLE_EMAIL_KEY = stringPreferencesKey("google_email")
    private val GOOGLE_NAME_KEY = stringPreferencesKey("google_name")
    private val GOOGLE_PHOTO_KEY = stringPreferencesKey("google_photo")
    private val FIREBASE_UID_KEY = stringPreferencesKey("firebase_uid")

    val loggedInUserId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[USER_ID_KEY]
    }

    val databaseMode: Flow<DatabaseMode> = context.dataStore.data.map { prefs ->
        val modeStr = prefs[DATABASE_MODE_KEY] ?: DatabaseMode.LOCAL.name
        try {
            DatabaseMode.valueOf(modeStr)
        } catch (e: Exception) {
            DatabaseMode.LOCAL
        }
    }

    val googleUser: Flow<GoogleUserData?> = context.dataStore.data.map { prefs ->
        val isGoogle = prefs[IS_GOOGLE_AUTH_KEY] ?: false
        val email = prefs[GOOGLE_EMAIL_KEY]
        val uid = prefs[FIREBASE_UID_KEY]
        if (isGoogle && !email.isNullOrBlank() && !uid.isNullOrBlank()) {
            GoogleUserData(
                uid = uid,
                email = email,
                displayName = prefs[GOOGLE_NAME_KEY],
                photoUrl = prefs[GOOGLE_PHOTO_KEY]
            )
        } else {
            null
        }
    }

    val firebaseUid: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[FIREBASE_UID_KEY] ?: ""
    }

    suspend fun setDatabaseMode(mode: DatabaseMode) {
        context.dataStore.edit { prefs ->
            prefs[DATABASE_MODE_KEY] = mode.name
        }
    }

    private fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    private fun checkPassword(password: String, hashed: String): Boolean {
        return try {
            BCrypt.checkpw(password, hashed)
        } catch (e: Exception) {
            android.util.Log.e("UserManager", "Password check failed", e)
            password == hashed
        }
    }

    suspend fun login(username: String, passwordPlain: String): Boolean {
        return try {
            val user = logDao.getUserByUsername(username)
            
            if (user != null && checkPassword(passwordPlain, user.passwordHash)) {
                context.dataStore.edit { prefs ->
                    prefs[USER_ID_KEY] = user.id
                    prefs[IS_GOOGLE_AUTH_KEY] = false
                    // Keep existing database mode if set, or default to LOCAL
                    if (!prefs.contains(DATABASE_MODE_KEY)) {
                        prefs[DATABASE_MODE_KEY] = DatabaseMode.LOCAL.name
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("UserManager", "Login failed", e)
            false
        }
    }

    suspend fun register(username: String, passwordPlain: String): Boolean {
        return try {
            val existingUser = logDao.getUserByUsername(username)
            if (existingUser != null) return false
            
            val user = User(username = username, passwordHash = hashPassword(passwordPlain))
            val userId = logDao.insertUser(user)
            
            if (userId > 0) {
                context.dataStore.edit { prefs ->
                    prefs[USER_ID_KEY] = userId
                    prefs[IS_GOOGLE_AUTH_KEY] = false
                    if (!prefs.contains(DATABASE_MODE_KEY)) {
                        prefs[DATABASE_MODE_KEY] = DatabaseMode.LOCAL.name
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("UserManager", "Registration failed", e)
            false
        }
    }

    suspend fun loginWithGoogle(
        idToken: String?,
        email: String,
        displayName: String?,
        photoUrl: String?,
        preferCloudDb: Boolean = true
    ): Boolean {
        return try {
            var finalUid = ""

            // 1. Try Firebase Auth with Google Credential if idToken is available
            val auth = FirebaseManager.getAuth()
            if (auth != null && !idToken.isNullOrBlank()) {
                try {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = auth.signInWithCredential(credential).await()
                    finalUid = authResult.user?.uid ?: ""
                } catch (e: Exception) {
                    android.util.Log.w("UserManager", "Firebase signInWithCredential failed, attempting anonymous/direct", e)
                }
            }

            // Fallback or direct UID generation if Firebase credential token wasn't used
            if (finalUid.isBlank()) {
                val currentFbUser = auth?.currentUser
                if (currentFbUser != null) {
                    finalUid = currentFbUser.uid
                } else if (auth != null) {
                    try {
                        val anonResult = auth.signInAnonymously().await()
                        finalUid = anonResult.user?.uid ?: ""
                    } catch (e: Exception) {
                        android.util.Log.w("UserManager", "Anonymous fallback failed", e)
                    }
                }
            }

            if (finalUid.isBlank()) {
                // Stable deterministic UID from email
                finalUid = "google_" + email.replace("@", "_").replace(".", "_")
            }

            // 2. Ensure user exists in local DB so foreign keys / local mode works seamlessly
            var localUser = logDao.getUserByUsername(email)
            val userId: Long
            if (localUser == null) {
                val newUser = User(
                    username = email,
                    passwordHash = hashPassword(finalUid.take(16))
                )
                userId = logDao.insertUser(newUser)
            } else {
                userId = localUser.id
            }

            // 3. Save into DataStore
            context.dataStore.edit { prefs ->
                prefs[USER_ID_KEY] = userId
                prefs[IS_GOOGLE_AUTH_KEY] = true
                prefs[GOOGLE_EMAIL_KEY] = email
                prefs[GOOGLE_NAME_KEY] = displayName ?: email.substringBefore("@")
                prefs[GOOGLE_PHOTO_KEY] = photoUrl ?: ""
                prefs[FIREBASE_UID_KEY] = finalUid
                if (preferCloudDb) {
                    prefs[DATABASE_MODE_KEY] = DatabaseMode.GOOGLE_CLOUD.name
                }
            }

            true
        } catch (e: Exception) {
            android.util.Log.e("UserManager", "loginWithGoogle failed", e)
            false
        }
    }

    suspend fun logout() {
        try {
            FirebaseManager.getAuth()?.signOut()
        } catch (e: Exception) {
            android.util.Log.w("UserManager", "Firebase signOut error", e)
        }
        context.dataStore.edit { prefs ->
            prefs.remove(USER_ID_KEY)
            prefs.remove(IS_GOOGLE_AUTH_KEY)
            prefs.remove(GOOGLE_EMAIL_KEY)
            prefs.remove(GOOGLE_NAME_KEY)
            prefs.remove(GOOGLE_PHOTO_KEY)
            prefs.remove(FIREBASE_UID_KEY)
        }
    }
}
