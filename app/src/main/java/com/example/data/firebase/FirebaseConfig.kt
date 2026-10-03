package com.example.data.firebase

/**
 * Firebase Configuration Placeholder architecture.
 * When Firebase is configured in the future, these credentials will connect
 * the cloud Firestore sync engine.
 * The application works completely offline with Room local persistence.
 */
data class FirebaseConfig(
    val apiKey: String = "",
    val authDomain: String = "",
    val projectId: String = "",
    val storageBucket: String = "",
    val messagingSenderId: String = "",
    val appId: String = ""
) {
    val isConfigured: Boolean
        get() = apiKey.isNotBlank() && projectId.isNotBlank() && appId.isNotBlank()
}

object FirebaseSyncManager {
    var config: FirebaseConfig = FirebaseConfig(
        apiKey = "",
        authDomain = "",
        projectId = "",
        storageBucket = "",
        messagingSenderId = "",
        appId = ""
    )

    fun isCloudSyncActive(): Boolean = config.isConfigured
}
