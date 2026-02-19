package com.pramod.validator.utils

import com.google.firebase.crashlytics.FirebaseCrashlytics
import android.util.Log

/**
 * Utility object for crash reporting and non-fatal error logging
 */
object CrashReporting {
    
    private val crashlytics = FirebaseCrashlytics.getInstance()
    
    /**
     * Log a non-fatal exception to Crashlytics
     */
    fun logException(throwable: Throwable, message: String? = null) {
        try {
            if (message != null) {
                crashlytics.log("Error: $message")
            }
            crashlytics.recordException(throwable)
            Log.e("CrashReporting", message ?: "Exception logged", throwable)
        } catch (e: Exception) {
            // Fallback to regular logging if Crashlytics fails
            Log.e("CrashReporting", "Failed to log to Crashlytics: ${e.message}", e)
        }
    }
    
    /**
     * Log a custom message to Crashlytics
     */
    fun log(message: String) {
        try {
            crashlytics.log(message)
            Log.d("CrashReporting", message)
        } catch (e: Exception) {
            Log.e("CrashReporting", "Failed to log to Crashlytics: ${e.message}", e)
        }
    }
    
    /**
     * Set a custom key-value pair for crash reports
     */
    fun setCustomKey(key: String, value: String) {
        try {
            crashlytics.setCustomKey(key, value)
        } catch (e: Exception) {
            Log.e("CrashReporting", "Failed to set custom key: ${e.message}", e)
        }
    }
    
    /**
     * Set user identifier for crash reports
     */
    fun setUserId(userId: String) {
        try {
            crashlytics.setUserId(userId)
        } catch (e: Exception) {
            Log.e("CrashReporting", "Failed to set user ID: ${e.message}", e)
        }
    }
}


