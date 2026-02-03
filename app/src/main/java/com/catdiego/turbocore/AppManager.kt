package com.catdiego.turbocore

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

enum class ModeCategory { GAMER, ECONOMIA, JOGOS, DISPLAY, SISTEMA, DEBLOAT }

data class AppInfo(val name: String, val packageName: String, val isSystem: Boolean)

data class OptimizationMode(
    val id: Int,
    val title: String,
    val description: String,
    val command: String,
    val category: ModeCategory,
    val riskLevel: Int,
    val requiresBrand: String? = null
)

object SmartCoreEngineV206 {
    val brand: String = Build.MANUFACTURER.uppercase()
    val model: String = Build.MODEL.lowercase()

    private val modes = mutableListOf<OptimizationMode>()

    init {
        generateModes()
    }

    fun getModesByCategory(category: ModeCategory): List<OptimizationMode> {
        return modes.filter {
            it.category == category && (it.requiresBrand == null || brand.contains(it.requiresBrand))
        }
    }

    fun getModeById(id: Int): OptimizationMode? = modes.find { it.id == id }

    fun getBrandColor(): Long = when {
        brand.contains("SAMSUNG") -> 0xFF2196F3
        brand.contains("XIAOMI") || brand.contains("POCO") -> 0xFFFF5722
        else -> 0xFF00E5FF
    }

    private fun generateModes() {
        // --- ABA 1: GAMER (FUSED) ---
        modes.add(OptimizationMode(1, "MODO DEUS", "MAX: Fixed Perf + 540p + Zero Latency + GPU Opt.", "cmd power set-fixed-performance-mode-enabled true && wm size 540x1200 && wm density 210 && settings put global touch_latency_mode 1 && settings put system pointer_speed 7 && settings put global window_animation_scale 0 && settings put global transition_animation_scale 0 && settings put global animator_duration_scale 0", ModeCategory.GAMER, 3))
        modes.add(OptimizationMode(2, "ULTIMATE TURBO", "SPEED: Compilation + renice + DNS Game.", "cmd package compile -m speed -a && settings put global private_dns_mode hostname && settings put global private_dns_specifier 1dot1dot1dot1.cloudflare-dns.com", ModeCategory.GAMER, 2))
        modes.add(OptimizationMode(3, "FPS BOOST", "VISUAL: 720p + SkiaVK + Anim 0x.", "wm size 720x1600 && wm density 280 && settings put global debug.hwui.renderer skiavk && settings put global window_animation_scale 0 && settings put global transition_animation_scale 0 && settings put global animator_duration_scale 0", ModeCategory.GAMER, 2))

        // --- ABA 2: ECONOMIA (FUSED) ---
        modes.add(OptimizationMode(10, "MODO FANTASMA", "EXTREME: 360p + GMS Suspend + Low Power.", "settings put global low_power 1 && pm suspend com.google.android.gms && wm size 360x800 && settings put global window_animation_scale 0", ModeCategory.ECONOMIA, 3))
        modes.add(OptimizationMode(11, "ULTRA SAVER", "LIGHT: Anim 0x + Wifi Scan Off + Low Power.", "settings put global low_power 1 && settings put global wifi_scan_always_enabled 0 && settings put global mobile_data_always_on 0 && settings put global window_animation_scale 0", ModeCategory.ECONOMIA, 2))

        // --- ABA 3: JOGOS (SPECIFIC) ---
        modes.add(OptimizationMode(20, "FREE FIRE MAX", "DPI 180 + Compile + renice.", "wm density 180 && cmd package compile -m speed -f com.dts.freefiremax && renice -n -20 -p \$(pidof com.dts.freefiremax || echo 0)", ModeCategory.JOGOS, 2))
        modes.add(OptimizationMode(21, "ROBLOX TURBO", "SkiaVK + Compile + 540p.", "cmd package compile -m speed -f com.roblox.client && settings put global debug.hwui.renderer skiavk && wm size 540x1200 && wm density 210", ModeCategory.JOGOS, 2))

        // --- ABA 4: DISPLAY ---
        modes.add(OptimizationMode(41, "720p Balanced", "720x1600 / 280dpi.", "wm size 720x1600 && wm density 280", ModeCategory.DISPLAY, 2))
        modes.add(OptimizationMode(42, "540p Performance", "540x1200 / 210dpi.", "wm size 540x1200 && wm density 210", ModeCategory.DISPLAY, 3))
        for (dpi in 320..500 step 40) {
            modes.add(OptimizationMode(50 + (dpi/10), "DPI $dpi", "Densidade granular.", "wm density $dpi", ModeCategory.DISPLAY, 2))
        }

        // --- ABA 5: SISTEMA ---
        modes.add(OptimizationMode(80, "SkiaVK (Vulkan)", "HWUI Backend.", "settings put global debug.hwui.renderer skiavk", ModeCategory.SISTEMA, 1))
        modes.add(OptimizationMode(81, "Zero Latency", "Touch bypass.", "settings put global touch_latency_mode 1", ModeCategory.SISTEMA, 1))
        modes.add(OptimizationMode(82, "DNS Cloudflare", "DNS Game.", "settings put global private_dns_mode hostname && settings put global private_dns_specifier 1dot1dot1dot1.cloudflare-dns.com", ModeCategory.SISTEMA, 1))
        modes.add(OptimizationMode(83, "Fixed Performance", "Sustained Perf.", "cmd power set-fixed-performance-mode-enabled true", ModeCategory.SISTEMA, 2))

        // --- ABA 6: DEBLOAT ---
        val bloatlist = mapOf(
            "SAMSUNG" to listOf("com.samsung.android.game.gametools", "com.samsung.android.game.gos"),
            "XIAOMI" to listOf("com.xiaomi.joyose", "com.miui.powerkeeper")
        )
        bloatlist.forEach { (b, list) ->
            list.forEachIndexed { i, p ->
                modes.add(OptimizationMode(160 + i + (if(b=="XIAOMI") 10 else 0), "Kill $b $i", "Desativa $p.", "pm disable-user $p", ModeCategory.DEBLOAT, 1, b))
            }
        }
    }
}

object AppManager {
    suspend fun runMode(context: Context, mode: OptimizationMode): String = withContext(Dispatchers.IO) {
        if (!Shizuku.pingBinder()) return@withContext "Erro: Shizuku OFF"

        val temp = ThermalWatchdog.getTemperature(context)
        if (mode.riskLevel >= 3 && temp > 38.0f) return@withContext "BLOQUEIO TÉRMICO!"

        if (ThermalWatchdog.isOverheating(context)) {
            runDirectCommand("cmd package compile --reset -a")
            return@withContext "EMERGÊNCIA TÉRMICA!"
        }

        if (mode.command.startsWith("wm size") && SmartCoreEngineV206.brand.contains("XIAOMI")) {
            return@withContext "BLOQUEIO: wm size instável em Xiaomi/HyperOS"
        }

        val finalCommand = buildFinalCommand(mode)

        try {
            withTimeout(5000L) {
                val method = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java, Array<String>::class.java, String::class.java
                )
                method.isAccessible = true
                val process = method.invoke(null, arrayOf("sh", "-c", finalCommand), null, null) as Process

                val error = process.errorStream.bufferedReader().use { it.readText() }
                process.waitFor()

                if (error.isNotEmpty()) "Falha: $error" else "Sucesso"
            }
        } catch (e: TimeoutCancellationException) {
            "Erro: Timeout!"
        } catch (e: Exception) {
            "Erro: ${e.message}"
        }
    }

    private suspend fun buildFinalCommand(mode: OptimizationMode): String {
        return if (mode.command == "ADRENALINE_UI") {
            val pkg = getForegroundApp()
            "cmd package compile -m speed-profile -f $pkg"
        } else {
            mode.command
        }
    }

    private suspend fun getForegroundApp(): String = withContext(Dispatchers.IO) {
        try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java, Array<String>::class.java, String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", "dumpsys window windows | grep -E 'mCurrentFocus'"), null, null) as Process

            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            val match = Regex("u0 ([^/]+)").find(output)
            match?.groupValues?.get(1) ?: "com.dts.freefireth"
        } catch (e: Exception) { "com.dts.freefireth" }
    }

    suspend fun runRawCommand(command: String): String = withContext(Dispatchers.IO) {
        if (!Shizuku.pingBinder()) return@withContext "Erro: Shizuku OFF"
        try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java, Array<String>::class.java, String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val error = process.errorStream.bufferedReader().use { it.readText() }
            process.waitFor()
            if (error.isNotEmpty()) "Erro: $error" else "Sucesso"
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
            } catch (e: Exception) { continue }
        }
        return false
    }

    fun getInstalledApps(context: Context, showSystem: Boolean): List<AppInfo> {
        val pm = context.packageManager
        val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        }

        return apps.filter {
            val isSystem = (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (showSystem) true else !isSystem
        }.map {
            AppInfo(
                name = it.loadLabel(pm).toString(),
                packageName = it.packageName,
                isSystem = (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            )
        }.sortedBy { it.name }
    }

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
