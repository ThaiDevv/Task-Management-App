package com.team.taskmanagementapp

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.initialize

/** Debug-build App Check setup. The debug token is generated locally, never hard-coded. */
internal object AiAppCheckInitializer {
    fun initialize(application: Application) {
        Firebase.initialize(context = application)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
        // Request a token at startup so this debug installation's secret appears in Logcat.
        FirebaseAppCheck.getInstance().getAppCheckToken(false)
    }
}
