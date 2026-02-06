package com.catdiego.turbocore.manager

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.MutableState
import rikka.shizuku.Shizuku

object ShizukuManager {

    private const val SHIZUKU_PACKAGE = "rikka.app.shizuku"
    private const val SHIZUKU_PRIVILEGED = "moe.shizuku.privileged.api"
    private const val GITHUB_RELEASE = "https://github.com/RikkaApps/Shizuku/releases"

    /**
     * Verifica se Shizuku está instalado no sistema
     */
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

    /**
     * Trata o clique inteligente:
     * - Se não instalado → Download
     * - Se instalado → Abre o app
     */
    fun handleShizukuButtonClick(context: Context) {
        if (!isShizukuInstalled(context)) {
            openShizukuDownload(context)
        } else {
            launchShizukuApp(context)
        }
    }

    /**
     * Abre o GitHub para download do Shizuku
     */
    private fun openShizukuDownload(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_RELEASE))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback se falhar
        }
    }

    /**
     * Abre o app Shizuku (com fallback para privileged)
     */
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

    /**
     * Verificação automática com reconnect
     */
    fun autoConnectShizuku(context: Context, statusState: MutableState<String>) {
        Thread {
            repeat(5) { attempt ->
                try {
                    if (Shizuku.pingBinder()) {
                        statusState.value = "Conectando..."
                        Shizuku.requestPermission(1001)
                        return@repeat
                    }
                } catch (e: Exception) {
                    // Continua tentando
                }
                Thread.sleep(500) // Espera 500ms entre tentativas
            }
        }.start()
    }

    /**
     * Listener para reconectar quando Shizuku volta online
     */
    fun setupAutoReconnect(context: Context, statusState: MutableState<String>): Shizuku.OnBinderReceivedListener {
        return Shizuku.OnBinderReceivedListener {
            statusState.value = "Shizuku Detectado! Reconectando..."
            autoConnectShizuku(context, statusState)
        }
    }
}
