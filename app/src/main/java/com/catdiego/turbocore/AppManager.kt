package com.catdiego.turbocore

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object AppManager {
    fun runCommand(command: String): String {
        if (!Shizuku.pingBinder()) return "Erro: Serviço Shizuku parado no sistema!"
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

    fun getDeviceDisplayName(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    fun getTotalRamGb(context: Context): Long {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        return memInfo.totalMem / (1024 * 1024 * 1024)
    }

    fun getOptimizationLevelSuggestion(context: Context): String {
        val ram = getTotalRamGb(context)
        val manufacturer = Build.MANUFACTURER.uppercase()

        return when {
            ram < 4 -> "Perfil Econômico (Dispositivo de Entrada)"
            ram > 8 -> "Perfil Ultra Performance"
            manufacturer.contains("SAMSUNG") -> "Otimização OneUI Pro"
            manufacturer.contains("XIAOMI") || manufacturer.contains("POCO") -> "Game Turbo Boost"
            else -> "Otimização de Sistema Padrão"
        }
    }

    fun getAutoOptimizationCommands(context: Context): List<String> {
        val commands = mutableListOf<String>()
        val manufacturer = Build.MANUFACTURER.uppercase()
        val ram = getTotalRamGb(context)

        // Universal optimizations
        commands.add("settings put global window_animation_scale 0.5")
        commands.add("settings put global transition_animation_scale 0.5")
        commands.add("settings put global animator_duration_scale 0.5")

        // Brand specific
        if (manufacturer.contains("SAMSUNG")) {
            commands.add("cmd power set-fixed-performance-mode-enabled true")
            commands.add("settings put global adaptive_battery_management 0")
            // V155: Desativar pacotes de log desnecessários
            commands.add("pm disable-user com.samsung.android.logcollector")
            commands.add("pm disable-user com.sec.android.app.logviewer")
            // Proportional size (V155: 0.75x simulation)
            commands.add("wm size 720x1600")
            commands.add("wm density 280")
        } else if (manufacturer.contains("XIAOMI") || manufacturer.contains("POCO")) {
            commands.add("cmd thermalservice override 1")
            commands.add("settings put system power_mode 1")
            // V155: Limpeza de cache de apps de segurança
            commands.add("pm clear com.miui.securitycenter")
            commands.add("pm trim-caches 128M")
        } else if (manufacturer.contains("MOTOROLA")) {
            commands.add("settings put global window_animation_scale 0.25")
            commands.add("setprop dalvik.vm.dex2oat-flags --compiler-filter=speed")
        }

        // RAM specific
        if (ram < 4) {
            commands.add("pm trim-caches 256M")
            commands.add("settings put global low_power 1")
            commands.add("wm size 540x1200")
            commands.add("wm density 210")
        } else if (ram > 8) {
            commands.add("cmd power set-fixed-performance-mode-enabled true")
            commands.add("wm size reset")
            commands.add("wm density reset")
        }

        return commands
    }
}
