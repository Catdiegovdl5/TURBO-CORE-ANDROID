package com.catdiego.turbocore

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ThermalWatchdog {
    private var lastTemp: Float = 0f

    fun getTemperature(context: Context): Float {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        lastTemp = temp / 10f
        return lastTemp
    }

    fun isOverheating(context: Context): Boolean {
        return getTemperature(context) > 39.0f
    }
}

object SmartCoreEngineV205 {
    val brand: String = Build.MANUFACTURER.uppercase()
    val model: String = Build.MODEL.lowercase()

    fun isLowEnd(): Boolean {
        return model.contains("c40") || model.contains("a01") || model.contains("core") || model.contains("a07")
    }

    fun getBrandColor(): Long {
        return when {
            brand.contains("SAMSUNG") -> 0xFF2196F3 // Blue
            brand.contains("XIAOMI") || brand.contains("POCO") -> 0xFFFF5722 // Orange
            else -> 0xFF00E5FF // Cyan default
        }
    }

    // TABELA DE MODOS POR MARCA (V205)
    fun getSpecificCommands(): List<Pair<String, String>> {
        return when {
            brand.contains("SAMSUNG") -> listOf(
                "MIRA LEVE PRO" to "settings put system pointer_speed 7 && settings put system touch_sensitivity 1",
                "GAME BOOSTER FIX" to "am force-stop com.samsung.android.game.gametools && am force-stop com.samsung.android.game.gamehome",
                "ULTRA GRAPHICS (VULKAN)" to "settings put global debug.hwui.renderer skiavk && settings put global sys.use_fifo_ui 1",
                "ONE UI PERF" to "settings put system game_performance 1",
                "DEX SPEED (SAFE)" to "cmd package compile -m speed-profile -f com.dts.freefireth",
                "RAM PLUS OFF" to "settings put global ram_expand_size_list 0"
            )
            brand.contains("XIAOMI") || brand.contains("POCO") -> listOf(
                "BYPASS JOYOSE" to "am force-stop com.xiaomi.joyose && pm suspend com.xiaomi.joyose",
                "PERFORMANCE MODE" to "settings put system power_mode 1",
                "THERMAL OVERRIDE" to "settings put global thermal_limit_strategy 0 && cmd thermalservice override 1",
                "DEX SPEED (SAFE)" to "cmd package compile -m speed-profile -f com.dts.freefireth",
                "LIMPAR SEGURANÇA" to "pm clear com.miui.securitycenter"
            )
            else -> listOf(
                "MAX PERFORMANCE" to "cmd power set-fixed-performance-mode-enabled true",
                "DEX SPEED (AOT)" to "cmd package compile -m speed -a"
            )
        }
    }

    // MODOS UNIVERSAIS (GAVETA GERAL - V205)
    fun getUniversalCommands(): List<Pair<String, String>> {
        return listOf(
            "FLUIDEZ 0.5x" to "settings put global window_animation_scale 0.5 && settings put global transition_animation_scale 0.5 && settings put global animator_duration_scale 0.5",
            "LIMPEZA DE RAM" to "am kill-all && pm trim-caches 1024M",
            "LATÊNCIA ZERO" to "settings put global touch_latency_mode 1",
            "SUSPENDER GMS (GHOST)" to "pm suspend com.google.android.gms"
        )
    }

    fun wrapCommand(command: String): String {
        val finalCommands = mutableListOf<String>()

        if (brand.contains("SAMSUNG")) {
            // Samsung permite alterar a sensibilidade de toque livremente
            finalCommands.add("settings put system pointer_speed 7")
            finalCommands.add("settings put system touch_sensitivity 1")
        }

        // V205: Trava Xiaomi - Bloqueia set-fixed-performance
        var sanitizedCommand = command
        if (brand.contains("XIAOMI") || brand.contains("POCO")) {
            if (sanitizedCommand.contains("set-fixed-performance-mode-enabled true")) {
                sanitizedCommand = sanitizedCommand.replace("cmd power set-fixed-performance-mode-enabled true", "echo 'Comando bloqueado no Xiaomi por segurança'")
            }
            // Xiaomi Bypasses
            finalCommands.add("am force-stop com.miui.powerkeeper")
        }

        finalCommands.add(sanitizedCommand)
        return finalCommands.joinToString(" && ")
    }
}

object AppManager {
    suspend fun runCommand(context: Context, command: String): String = withContext(Dispatchers.IO) {
        if (!Shizuku.pingBinder()) return@withContext "Erro: Serviço Shizuku parado no sistema!"

        if (ThermalWatchdog.isOverheating(context)) {
            val resetCmd = "cmd package compile --reset -a"
            runDirectCommand(resetCmd)
            return@withContext "AVISO: Temperatura Crítica (${ThermalWatchdog.getTemperature(context)}°C). Sistema Resetado!"
        }

        val isSamsungLowEnd = SmartCoreEngineV205.brand.contains("SAMSUNG") &&
                              SmartCoreEngineV205.isLowEnd()

        val safeCommand = if (isSamsungLowEnd && command.contains("wm size") && !command.contains("reset")) {
            command.replace(Regex("wm size \\d+x\\d+"), "echo 'wm size bloqueado'")
        } else {
            command
        }

        try {
            withTimeout(3000L) {
                // Remoção de reflexão desnecessária: newProcess é público na v13.1.5
                val process = Shizuku.newProcess(arrayOf("sh", "-c", safeCommand), null, null)
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = reader.readText()
                process.waitFor()
                if (output.isEmpty()) "Sucesso" else output
            }
        } catch (e: TimeoutCancellationException) {
            "Erro: Timeout!"
        } catch (e: Exception) {
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

    fun getDeviceDisplayName(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
    }

    // Helper para comandos diretos sem timeout/verificações extras (usado no Thermal Guard)
    private fun runDirectCommand(command: String) {
        try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java, Array<String>::class.java, String::class.java
            )
            method.isAccessible = true
            method.invoke(null, arrayOf("sh", "-c", command), null, null)
        } catch (e: Exception) {}
    }
}
