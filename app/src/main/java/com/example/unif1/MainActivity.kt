package com.example.unif1

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.example.unif1.ui.UniF1App
import com.example.unif1.ui.theme.UniF1Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        val appCheckProviderFactory = if (BuildConfig.DEBUG) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }

        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(appCheckProviderFactory)
        Log.d("AppCheckSetup", "App Check provider initialized")

        if (BuildConfig.DEBUG) {
            Log.d("AppCheckSetup", "Checking for App Check Debug Token...")
            Log.d("AppCheckSetup", "LOOK IN LOGCAT FOR: 'Firebase App Check debug token:'")
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // We attempt to get a token just to trigger the provider to initialize and log its secret UUID
                    FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
                    Log.d("AppCheckStatus", "App Check token exchange successful!")
                } catch (e: Exception) {
                    Log.e("AppCheckStatus", "App Check exchange failed (this is expected until you register the secret token in the console)")
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            UniF1Theme {
                UniF1App()
            }
        }
    }
}
