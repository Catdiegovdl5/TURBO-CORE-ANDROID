package com.catdiego.turbocore

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

object AppManager {
    suspend fun runCommand(command: String): String = withContext(Dispatchers.IO) {
        if (!Shizuku.pingBinder()) return@withContext "Erro: Serviço Shizuku parado no sistema!"
        var process: Process? = null
        try {
            withTimeout(3000) {
                val method = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java, Array<String>::class.java, String::class.java
                )
                method.isAccessible = true
                process = method.invoke(null, arrayOf("sh", "-c", "$command 2>&1"), null, null) as Process
                val p = process!!
                val reader = BufferedReader(InputStreamReader(p.inputStream))
                val output = reader.readText()
                p.waitFor()
                if (output.isEmpty()) "Sucesso" else output
            }
        } catch (e: Exception) {
            process?.destroy()
            "Erro: ${e.message}"
        }
    }

    fun isShizukuInstalled(context: Context): Boolean {
        val packages = listOf("moe.shizuku.privileged.api", "rikka.app.shizuku")
        val pm = context.packageManager
        for (pkg in packages) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0L))
                } else {
                    pm.getPackageInfo(pkg, 0)
                }
                return true
            } catch (e: Exception) {
                continue
            }
        }
        return false
    }

    /**
     * Returns a list of installed non-system apps.
     * Optimized for RAM by filtering system apps (V140 requirement).
     */
    fun getFilteredApps(context: Context): List<String> {
        return try {
            val pm = context.packageManager
            val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                pm.getInstalledApplications(0)
            }
            apps.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                .map { it.loadLabel(pm).toString() }
        } catch (e: Exception) {
            listOf("Erro ao carregar apps")
        }
    }
}
