package com.team.taskmanagementapp

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize

/** Production builds attest the app with Play Integrity, never the debug provider. */
internal object AiAppCheckInitializer {
    fun initialize(application: Application) {
        Firebase.initialize(context = application)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
    }
}
