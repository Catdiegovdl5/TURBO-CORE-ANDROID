package com.catdiego.turbocore.manager

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

object ShizukuManager {

    private const val SHIZUKU_PACKAGE = "rikka.app.shizuku"
    private const val SHIZUKU_PRIVILEGED = "moe.shizuku.privileged.api"
    private const val GITHUB_RELEASE = "https://github.com/RikkaApps/Shizuku/releases"
    const val REQUEST_CODE = 1001 // Public for Activity

    private val _statusFlow = MutableStateFlow("Inicializando...")
    val statusFlow = _statusFlow.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isMonitoring = false

    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true

        scope.launch {
            while (isActive) {
                checkStatus()
                delay(3000) // Heartbeat every 3s
            }
        }
    }

    fun forceCheck() {
        scope.launch {
            _statusFlow.value = "Verificando..."
            checkStatus()
        }
    }

    private fun checkStatus() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    _statusFlow.value = "Conectado"
                } else {
                    _statusFlow.value = "Permissão Necessária"
                }
            } else {
                _statusFlow.value = "Offline (Inicie o Shizuku)"
            }
        } catch (e: Exception) {
            _statusFlow.value = "Erro: ${e.message}"
        }
    }

    fun isShizukuInstalled(context: Context): Boolean {
        val packages = listOf(SHIZUKU_PACKAGE, SHIZUKU_PRIVILEGED)
        val pm = context.packageManager
        return packages.any { pkg ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0L))
                } else {
                    pm.getPackageInfo(pkg, 0)
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    fun handleShizukuButtonClick(context: Context) {
        if (!isShizukuInstalled(context)) {
            openShizukuDownload(context)
        } else {
            launchShizukuApp(context)
        }
    }

    private fun openShizukuDownload(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_RELEASE))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) { }
    }

    fun launchShizukuApp(context: Context) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
                ?: context.packageManager.getLaunchIntentForPackage(SHIZUKU_PRIVILEGED)

            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
            }
        } catch (e: Exception) {
            openShizukuDownload(context)
        }
    }

    // Listener simplificado apenas para log/update imediato
    fun getStickyListener(): Shizuku.OnBinderReceivedListener {
        return Shizuku.OnBinderReceivedListener {
            _statusFlow.value = "Binder Detectado!"
            checkStatus()
        }
    }
}
