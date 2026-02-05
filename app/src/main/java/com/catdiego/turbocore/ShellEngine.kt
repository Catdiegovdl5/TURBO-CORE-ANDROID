package com.catdiego.turbocore

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.regex.Pattern

object ShellEngine {
    fun runCommand(command: String): String {
        if (!isAvailable()) return "Erro: Serviço Shizuku parado ou sem permissão!"
        return try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java, Array<String>::class.java, String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            process.waitFor()
            if (output.isEmpty()) "Sucesso" else output
        } catch (e: Exception) { "Erro: ${e.message}" }
    }

    fun getPhysicalResolution(): Pair<Int, Int>? {
        if (!isAvailable()) return null
        return try {
            val output = runCommand("wm size")
            // Pattern to match "Physical size: 720x1600"
            val matcher = Pattern.compile("Physical size: (\\d+)x(\\d+)").matcher(output)
            if (matcher.find()) {
                val width = matcher.group(1)?.toIntOrNull()
                val height = matcher.group(2)?.toIntOrNull()
                if (width != null && height != null) {
                    Pair(width, height)
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun applyGpuBoost() {
        if (!isAvailable()) return
        val paths = listOf(
            "/sys/class/devfreq/18500000.mali/governor",
            "/sys/class/devfreq/11840000.mali/governor",
            "/sys/kernel/gpu/gpu_governor",
            "/sys/devices/platform/18500000.mali/devfreq/18500000.mali/governor",
            "/proc/gpufreq/gpufreq_opp_freq"
        )

        // Try all paths "best effort"
        for (path in paths) {
            runCommand("echo performance > $path")
        }
    }

    fun isAvailable(): Boolean {
        return try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
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
                .map { it.packageName }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
