package com.example.myapplication.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:591222241040:android:3e22a3b8f3db4683a370a629a6a8a78d")
                    .setApiKey("AIzaSyB_591222241040_LogManagerKey_FirebaseInit")
                    .setProjectId("ais-q25k6lbejnvpeps3bp4ix5")
                    .setStorageBucket("ais-q25k6lbejnvpeps3bp4ix5.appspot.com")
                    .build()
                FirebaseApp.initializeApp(context, options)
                Log.d(TAG, "FirebaseApp successfully initialized")
            } else {
                Log.d(TAG, "FirebaseApp already initialized by provider")
            }
            
            try {
                val firestore = FirebaseFirestore.getInstance()
                val settings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build()
                firestore.firestoreSettings = settings
            } catch (e: Exception) {
                Log.w(TAG, "Could not set Firestore persistence settings", e)
            }
            
            isInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FirebaseApp", e)
        }
    }

    fun getAuth(): FirebaseAuth? {
        return try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseAuth not available", e)
            null
        }
    }

    fun getFirestore(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseFirestore not available", e)
            null
        }
    }

    fun isReady(): Boolean = isInitialized
}
