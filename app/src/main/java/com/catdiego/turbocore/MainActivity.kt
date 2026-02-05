package com.catdiego.turbocore

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    private val REQUEST_CODE = 1001

    // Global state for permission status
    private var shizukuStatus by mutableStateOf("Verificando...")

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        shizukuStatus = if (grantResult == PackageManager.PERMISSION_GRANTED) "Conectado" else "Permissão Negada"
    }

    private val binderListener = Shizuku.OnBinderReceivedListener {
        checkAndRequestShizukuPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Shizuku Listeners
        runCatching { Shizuku.addRequestPermissionResultListener(permissionListener) }
        runCatching { Shizuku.addBinderReceivedListener(binderListener) }

        // Initial Check
        if (Shizuku.pingBinder()) {
            checkAndRequestShizukuPermission()
        } else {
             shizukuStatus = "Shizuku Parado"
        }

        setContent {
            // Auto-request permission on startup if Binder is alive
             LaunchedEffect(Unit) {
                delay(1000)
                if (Shizuku.pingBinder()) {
                     if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                         Shizuku.requestPermission(REQUEST_CODE)
                     } else {
                         shizukuStatus = "Conectado"
                     }
                }
            }

            InnovationHubDashboard(
                shizukuStatus = shizukuStatus,
                onConnectShizuku = {
                     if (Shizuku.pingBinder()) {
                        checkAndRequestShizukuPermission()
                     } else {
                        launchShizukuApp(this@MainActivity)
                     }
                }
            )
        }
    }

    private fun checkAndRequestShizukuPermission() {
        try {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                shizukuStatus = "Conectado"
            } else {
                Shizuku.requestPermission(REQUEST_CODE)
            }
        } catch (e: Exception) {
            shizukuStatus = "Erro Shizuku: ${e.message}"
            launchShizukuApp(this)
        }
    }

    private fun launchShizukuApp(context: Context) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage("rikka.app.shizuku")
                ?: context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                val githubIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RikkaApps/Shizuku/releases"))
                context.startActivity(githubIntent)
            }
        } catch (e: Exception) {
            shizukuStatus = "Erro de I/O: Reinstale o Shizuku"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { Shizuku.removeRequestPermissionResultListener(permissionListener) }
        runCatching { Shizuku.removeBinderReceivedListener(binderListener) }
    }
}
