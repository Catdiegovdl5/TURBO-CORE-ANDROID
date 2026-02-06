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
    private const val REQUEST_CODE = 1001

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

    fun autoConnectShizuku(context: Context, statusState: MutableState<String>) {
        Thread {
            var connected = false
            repeat(10) { attempt -> // Aumentado para 10 tentativas
                try {
                    if (Shizuku.pingBinder()) {
                        connected = true
                        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                            statusState.value = "Conectado"
                        } else {
                            statusState.value = "Permissão Necessária"
                            Shizuku.requestPermission(REQUEST_CODE)
                        }
                        return@repeat
                    } else {
                        statusState.value = "Tentando conectar... ($attempt)"
                    }
                } catch (e: Exception) {
                    statusState.value = "Erro: ${e.message}"
                }
                Thread.sleep(1000) // Aumentado para 1s
            }
            if (!connected) {
                statusState.value = "Offline (Clique p/ Iniciar)"
            }
        }.start()
    }

    fun setupAutoReconnect(context: Context, statusState: MutableState<String>): Shizuku.OnBinderReceivedListener {
        return Shizuku.OnBinderReceivedListener {
            statusState.value = "Binder Detectado!"
            autoConnectShizuku(context, statusState)
        }
    }
}
