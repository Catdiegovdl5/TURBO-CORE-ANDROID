package com.catdiego.turbocore

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.catdiego.turbocore.ui.theme.TurboCoreTheme
import com.catdiego.turbocore.ui.AppNavigation
import rikka.shizuku.Shizuku
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        // Global Crash Handler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleCrash(thread, throwable)
        }

        // Shizuku Initialization with Safety Net
        runCatching {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addRequestPermissionResultListener(this)
        }.onFailure { e ->
            e.printStackTrace()
            // Continue loading UI even if Shizuku fails (Safe Mode / View Mode)
            Toast.makeText(this, "Shizuku init failed: ${e.message}. App entering View Mode.", Toast.LENGTH_LONG).show()
        }

        try {
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
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback UI using standard Android Views if Compose fails completely
            setContentView(TextView(this).apply {
                text = "Erro Crítico: ${e.message}"
                setTextColor(android.graphics.Color.RED)
                textSize = 20f
            })
        }
    }

    // CORREÇÃO DA FUNÇÃO isShizukuInstalled (Modern Flags 2026)
    private fun isShizukuInstalled(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo("moe.shizuku.privileged.api", PackageManager.PackageInfoFlags.of(0))
            } else {
                context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            }
            true
        } catch (e: Exception) { false }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeRequestPermissionResultListener(this)
        }
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            // Permission granted
        } else {
            // Permission denied
        }
    }

    private fun handleCrash(thread: Thread, throwable: Throwable) {
        try {
            val crashLogFile = File(filesDir, "crash_log.txt")
            val writer = PrintWriter(FileWriter(crashLogFile, true))
            writer.println("Crash Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            writer.println("Thread: ${thread.name}")
            throwable.printStackTrace(writer)
            writer.close()
            Log.e("TurboCoreCrash", "Crash logged to ${crashLogFile.absolutePath}", throwable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Let the system handle the crash (kill process)
        android.os.Process.killProcess(android.os.Process.myPid())
        System.exit(1)
    }
}
