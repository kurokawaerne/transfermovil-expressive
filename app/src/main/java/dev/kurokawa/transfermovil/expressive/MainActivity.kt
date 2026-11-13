package dev.kurokawa.transfermovil.expressive

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.kurokawa.transfermovil.expressive.ui.TmApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Single dynamic-coloured system splash — installSplashScreen
        // before super, and NO Compose overlay splash (no double splash).
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val perms = buildList {
            add(Manifest.permission.CALL_PHONE)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.READ_SMS)
            add(Manifest.permission.RECEIVE_SMS)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (perms.isNotEmpty()) requestPermissions(perms.toTypedArray(), 1)
        val start = intent?.getStringExtra("open") ?: "home"
        setContent { TmApp(startScreen = start) }
    }
}
