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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
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
    private val USERNAME_KEY = stringPreferencesKey("username")
    private val DATABASE_MODE_KEY = stringPreferencesKey("database_mode")
    private val IS_GOOGLE_AUTH_KEY = booleanPreferencesKey("is_google_auth")
    private val GOOGLE_EMAIL_KEY = stringPreferencesKey("google_email")
    private val GOOGLE_NAME_KEY = stringPreferencesKey("google_name")
    private val GOOGLE_PHOTO_KEY = stringPreferencesKey("google_photo")
    private val FIREBASE_UID_KEY = stringPreferencesKey("firebase_uid")

    val loggedInUserId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[USER_ID_KEY]
    }

    val currentUsername: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USERNAME_KEY]
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
        val email = prefs[GOOGLE_EMAIL_KEY]
        val uid = prefs[FIREBASE_UID_KEY]
        if (!email.isNullOrBlank() && !uid.isNullOrBlank()) {
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
        val savedUid = prefs[FIREBASE_UID_KEY]
        if (!savedUid.isNullOrBlank()) {
            savedUid
        } else {
            val localUserId = prefs[USER_ID_KEY] ?: 1L
            "user_$localUserId"
        }
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
            if (hashed.isBlank()) false else BCrypt.checkpw(password, hashed)
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
                    prefs[USERNAME_KEY] = user.username
                    
                    if (!user.googleEmail.isNullOrBlank()) {
                        prefs[IS_GOOGLE_AUTH_KEY] = true
                        prefs[GOOGLE_EMAIL_KEY] = user.googleEmail
                        prefs[GOOGLE_NAME_KEY] = user.googleDisplayName ?: user.username
                        prefs[FIREBASE_UID_KEY] = user.googleUid ?: ("google_" + user.googleEmail.replace("@", "_").replace(".", "_"))
                    } else {
                        prefs[IS_GOOGLE_AUTH_KEY] = false
                        prefs.remove(GOOGLE_EMAIL_KEY)
                        prefs.remove(GOOGLE_NAME_KEY)
                        prefs.remove(GOOGLE_PHOTO_KEY)
                    }

                    if (!prefs.contains(DATABASE_MODE_KEY)) {
                        prefs[DATABASE_MODE_KEY] = if (!user.googleEmail.isNullOrBlank()) {
                            DatabaseMode.GOOGLE_CLOUD.name
                        } else {
                            DatabaseMode.LOCAL.name
                        }
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
                    prefs[USERNAME_KEY] = username
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
        preferCloudDb: Boolean = true,
        customUsername: String? = null
    ): Pair<Boolean, String?> {
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
                    android.util.Log.w("UserManager", "Firebase signInWithCredential failed, attempting direct UID", e)
                }
            }

            if (finalUid.isBlank()) {
                finalUid = "google_" + email.replace("@", "_").replace(".", "_")
            }

            // 2. Check if an account is already linked to this Google account
            var user: User? = logDao.getUserByGoogleAccount(email, finalUid) ?: logDao.getUserByGoogleEmail(email)

            if (user == null) {
                // If custom username requested, try to link or create with it
                val desiredUsername = customUsername?.trim()?.takeIf { it.isNotBlank() }
                if (desiredUsername != null) {
                    val existingUser = logDao.getUserByUsername(desiredUsername)
                    if (existingUser != null) {
                        // If this existing user is already linked to ANOTHER Google email
                        if (!existingUser.googleEmail.isNullOrBlank() && !existingUser.googleEmail.equals(email, ignoreCase = true)) {
                            return Pair(false, "Username '$desiredUsername' is already linked to a different Google account.")
                        }
                        // Link this Google account to the existing username!
                        val updated = existingUser.copy(
                            googleEmail = email,
                            googleUid = finalUid,
                            googleDisplayName = displayName ?: existingUser.username
                        )
                        logDao.updateUser(updated)
                        user = updated
                    } else {
                        // Create brand new user with requested username
                        val newUser = User(
                            username = desiredUsername,
                            passwordHash = "",
                            googleEmail = email,
                            googleUid = finalUid,
                            googleDisplayName = displayName ?: desiredUsername
                        )
                        val newId = logDao.insertUser(newUser)
                        user = newUser.copy(id = newId)
                    }
                } else {
                    // Check if a user with email handle already exists
                    var candidateUsername = email.substringBefore("@").replace(".", "_")
                    val existingUser = logDao.getUserByUsername(candidateUsername)
                    if (existingUser != null) {
                        if (existingUser.googleEmail.isNullOrBlank() || existingUser.googleEmail.equals(email, ignoreCase = true)) {
                            // Link to this user
                            val updated = existingUser.copy(
                                googleEmail = email,
                                googleUid = finalUid,
                                googleDisplayName = displayName ?: existingUser.username
                            )
                            logDao.updateUser(updated)
                            user = updated
                        } else {
                            // Suffix with random numbers
                            candidateUsername = "${candidateUsername}_${(100..999).random()}"
                            val newUser = User(
                                username = candidateUsername,
                                passwordHash = "",
                                googleEmail = email,
                                googleUid = finalUid,
                                googleDisplayName = displayName ?: candidateUsername
                            )
                            val newId = logDao.insertUser(newUser)
                            user = newUser.copy(id = newId)
                        }
                    } else {
                        val newUser = User(
                            username = candidateUsername,
                            passwordHash = "",
                            googleEmail = email,
                            googleUid = finalUid,
                            googleDisplayName = displayName ?: candidateUsername
                        )
                        val newId = logDao.insertUser(newUser)
                        user = newUser.copy(id = newId)
                    }
                }
            } else {
                // Ensure google info is fully synchronized in DB
                if (user.googleEmail != email || user.googleUid != finalUid) {
                    val updated = user.copy(
                        googleEmail = email,
                        googleUid = finalUid,
                        googleDisplayName = displayName ?: user.googleDisplayName ?: user.username
                    )
                    logDao.updateUser(updated)
                    user = updated
                }
            }

            // 3. Save into DataStore
            context.dataStore.edit { prefs ->
                prefs[USER_ID_KEY] = user.id
                prefs[USERNAME_KEY] = user.username
                prefs[IS_GOOGLE_AUTH_KEY] = true
                prefs[GOOGLE_EMAIL_KEY] = email
                prefs[GOOGLE_NAME_KEY] = displayName ?: user.username
                prefs[GOOGLE_PHOTO_KEY] = photoUrl ?: ""
                prefs[FIREBASE_UID_KEY] = finalUid
                if (preferCloudDb) {
                    prefs[DATABASE_MODE_KEY] = DatabaseMode.GOOGLE_CLOUD.name
                }
            }

            Pair(true, user.username)
        } catch (e: Exception) {
            android.util.Log.e("UserManager", "loginWithGoogle failed", e)
            Pair(false, e.message ?: "Google sign-in failed")
        }
    }

    suspend fun linkGoogleAccount(
        idToken: String?,
        email: String,
        displayName: String?,
        photoUrl: String?,
        preferCloudDb: Boolean = true
    ): Pair<Boolean, String?> {
        return try {
            val currentUserId = context.dataStore.data.map { it[USER_ID_KEY] }.first()
                ?: return Pair(false, "No active user logged in.")

            // Check if this Google email is already linked to ANOTHER user
            val existing = logDao.getUserByGoogleEmail(email)
            if (existing != null && existing.id != currentUserId) {
                return Pair(false, "This Google account is already linked to username: ${existing.username}")
            }

            var finalUid = ""
            val auth = FirebaseManager.getAuth()
            if (auth != null && !idToken.isNullOrBlank()) {
                try {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = auth.signInWithCredential(credential).await()
                    finalUid = authResult.user?.uid ?: ""
                } catch (e: Exception) {
                    android.util.Log.w("UserManager", "Firebase signInWithCredential failed in linkGoogleAccount", e)
                }
            }

            if (finalUid.isBlank()) {
                finalUid = "google_" + email.replace("@", "_").replace(".", "_")
            }

            val user = logDao.getUserById(currentUserId)
                ?: return Pair(false, "Current user not found.")

            val updatedUser = user.copy(
                googleEmail = email,
                googleUid = finalUid,
                googleDisplayName = displayName ?: user.username
            )
            logDao.updateUser(updatedUser)

            context.dataStore.edit { prefs ->
                prefs[IS_GOOGLE_AUTH_KEY] = true
                prefs[GOOGLE_EMAIL_KEY] = email
                prefs[GOOGLE_NAME_KEY] = displayName ?: user.username
                prefs[GOOGLE_PHOTO_KEY] = photoUrl ?: ""
                prefs[FIREBASE_UID_KEY] = finalUid
                if (preferCloudDb) {
                    prefs[DATABASE_MODE_KEY] = DatabaseMode.GOOGLE_CLOUD.name
                }
            }
            Pair(true, null)
        } catch (e: Exception) {
            android.util.Log.e("UserManager", "linkGoogleAccount failed", e)
            Pair(false, e.message ?: "Failed to link Google account")
        }
    }

    suspend fun unlinkGoogleAccount(): Boolean {
        return try {
            val currentUserId = context.dataStore.data.map { it[USER_ID_KEY] }.first()
            if (currentUserId != null) {
                val user = logDao.getUserById(currentUserId)
                if (user != null) {
                    logDao.updateUser(
                        user.copy(
                            googleEmail = null,
                            googleUid = null,
                            googleDisplayName = null
                        )
                    )
                }
            }
            try {
                FirebaseManager.getAuth()?.signOut()
            } catch (e: Exception) {
                android.util.Log.w("UserManager", "Firebase signOut error", e)
            }
            context.dataStore.edit { prefs ->
                prefs[IS_GOOGLE_AUTH_KEY] = false
                prefs.remove(GOOGLE_EMAIL_KEY)
                prefs.remove(GOOGLE_NAME_KEY)
                prefs.remove(GOOGLE_PHOTO_KEY)
                prefs[DATABASE_MODE_KEY] = DatabaseMode.LOCAL.name
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("UserManager", "unlinkGoogleAccount failed", e)
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
            prefs.remove(USERNAME_KEY)
            prefs.remove(IS_GOOGLE_AUTH_KEY)
            prefs.remove(GOOGLE_EMAIL_KEY)
            prefs.remove(GOOGLE_NAME_KEY)
            prefs.remove(GOOGLE_PHOTO_KEY)
            prefs.remove(FIREBASE_UID_KEY)
        }
    }
}
