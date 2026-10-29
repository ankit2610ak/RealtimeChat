package com.realtimechat

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class RealtimeChatApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "FirebaseApp initialized successfully on Application startup.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize FirebaseApp on Application startup", e)
        }
    }

    companion object {
        private const val TAG = "RealtimeChatApp"
    }
}
