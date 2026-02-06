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

    fun autoConnectShizuku(context: Context, statusState: MutableState<String>, logger: (String) -> Unit = {}) {
        Thread {
            var connected = false
            logger("Iniciando autoConnect...")
            repeat(10) { attempt ->
                try {
                    val ping = Shizuku.pingBinder()
                    logger("Ping ($attempt): $ping")

                    if (ping) {
                        connected = true
                        val permission = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                        logger("Permissão: $permission")

                        if (permission) {
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
                    logger("Exception: ${e.message}")
                }
                Thread.sleep(1000)
            }
            if (!connected) {
                statusState.value = "Offline (Clique p/ Iniciar)"
                logger("Falha na conexão: Binder não respondeu.")
            }
        }.start()
    }

    fun setupAutoReconnect(context: Context, statusState: MutableState<String>, logger: (String) -> Unit): Shizuku.OnBinderReceivedListener {
        return Shizuku.OnBinderReceivedListener {
            statusState.value = "Binder Detectado!"
            logger("Listener: Binder Recebido!")
            autoConnectShizuku(context, statusState, logger)
        }
    }
}
