package com.catdiego.turbocore

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.catdiego.turbocore.ui.theme.TurboCoreTheme
import com.catdiego.turbocore.ui.AppNavigation
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity(), Shizuku.OnRequestPermissionResultListener {

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        if (Shizuku.isPreV11()) {
            // Pre-v11 handling if necessary
        } else {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                if (Shizuku.shouldShowRequestPermissionRationale()) {
                    // Show rationale if needed
                }
                Shizuku.requestPermission(0)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register Shizuku listeners
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addRequestPermissionResultListener(this)

        setContent {
            TurboCoreTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeRequestPermissionResultListener(this)
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            // Permission granted
        } else {
            // Permission denied
        }
    }
}
