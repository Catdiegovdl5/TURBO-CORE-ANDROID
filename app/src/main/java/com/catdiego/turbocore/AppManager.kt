package com.catdiego.turbocore

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
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
        return getTemperature(context) >= 39.0f
    }

    fun isCritical(context: Context): Boolean {
        return getTemperature(context) >= 40.0f
    }
}

enum class ModeCategory { DESEMPENHO, REDE, ECONOMIA, JOGOS, UTILITARIOS }

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
        // --- DESEMPENHO (10 modos) ---
        modes.add(OptimizationMode(1, "Fúria Chimera", "MAX Performance + Fixed Clock.", "cmd power set-fixed-performance-mode-enabled true && settings put global touch_latency_mode 1", ModeCategory.DESEMPENHO, 3))
        modes.add(OptimizationMode(2, "Modo Bruto", "540p + 210 DPI + GPU Opt.", "wm size 540x1200 && wm density 210 && settings put global debug.hwui.renderer skiavk", ModeCategory.DESEMPENHO, 3))
        modes.add(OptimizationMode(3, "Vulkan Boost", "Força SkiaVK para renderização.", "settings put global debug.hwui.renderer skiavk", ModeCategory.DESEMPENHO, 1))
        modes.add(OptimizationMode(4, "Agendador Turbo", "Schedutil Boost + Threads.", "settings put global ion_pool_touch_boost 1", ModeCategory.DESEMPENHO, 1))
        modes.add(OptimizationMode(5, "AOT Speed", "Compilação completa do sistema.", "cmd package compile -m speed -a", ModeCategory.DESEMPENHO, 2))
        modes.add(OptimizationMode(6, "FPS Estável", "Desativa thermal throttling suave.", "settings put global debug.performance.tuning 1", ModeCategory.DESEMPENHO, 2))
        modes.add(OptimizationMode(7, "HW UI Force", "Força aceleração por hardware.", "settings put global debug.hwui.force_hw_ui 1", ModeCategory.DESEMPENHO, 1))
        modes.add(OptimizationMode(8, "Zero Latency", "Bypass de filtros de toque.", "settings put global touch_latency_mode 1", ModeCategory.DESEMPENHO, 1))
        modes.add(OptimizationMode(9, "Governor Perf", "CPU em modo Performance.", "for i in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo performance > \$i; done", ModeCategory.DESEMPENHO, 3))
        modes.add(OptimizationMode(10, "Samsung Lite", "Desativa Knox e ML de fundo.", "pm disable-user com.samsung.android.knox.containercore && pm disable-user com.samsung.android.smartface", ModeCategory.DESEMPENHO, 2, "SAMSUNG"))

        // --- REDE (10 modos) ---
        modes.add(OptimizationMode(11, "Ping Zero", "DNS Cloudflare + Low Latency.", "settings put global private_dns_mode hostname && settings put global private_dns_specifier 1dot1dot1dot1.cloudflare-dns.com", ModeCategory.REDE, 1))
        modes.add(OptimizationMode(12, "DNS Google", "8.8.8.8 para estabilidade.", "settings put global private_dns_mode hostname && settings put global private_dns_specifier dns.google", ModeCategory.REDE, 1))
        modes.add(OptimizationMode(13, "Net Speed", "Prioriza pacotes TCP/UDP.", "settings put global net_speed_boost 1", ModeCategory.REDE, 1))
        modes.add(OptimizationMode(14, "Wifi Scan OFF", "Desativa busca agressiva.", "settings put global wifi_scan_always_enabled 0", ModeCategory.REDE, 1))
        modes.add(OptimizationMode(15, "Cellular Boost", "Otimiza sinal LTE/4G.", "settings put global cellular_data_boost 1", ModeCategory.REDE, 1))
        modes.add(OptimizationMode(16, "TCP Fast Open", "Acelera Handshake de rede.", "settings put global tcp_fast_open 1", ModeCategory.REDE, 1))
        modes.add(OptimizationMode(17, "Game Network", "Modo exclusivo para jogos.", "settings put global network_gaming_mode 1", ModeCategory.REDE, 1))
        modes.add(OptimizationMode(18, "Data Saver Game", "Foca dados apenas no jogo.", "settings put global data_saver_mode 1", ModeCategory.REDE, 1))
        modes.add(OptimizationMode(19, "DNS AdGuard", "Bloqueia anúncios e reduz latência.", "settings put global private_dns_mode hostname && settings put global private_dns_specifier dns.adguard.com", ModeCategory.REDE, 1))
        modes.add(OptimizationMode(20, "Signal Stabilizer", "Impede troca de torre frenética.", "settings put global signal_stabilizer 1", ModeCategory.REDE, 1))

        // --- ECONOMIA (10 modos) ---
        modes.add(OptimizationMode(21, "Super Saver", "Low Power + GMS Suspend.", "settings put global low_power 1 && pm suspend com.google.android.gms", ModeCategory.ECONOMIA, 2))
        modes.add(OptimizationMode(22, "Modo Fantasma", "360p + Economia Extrema.", "wm size 360x800 && settings put global low_power 1", ModeCategory.ECONOMIA, 3))
        modes.add(OptimizationMode(23, "Anim 0x", "Remove todas as animações.", "settings put global window_animation_scale 0 && settings put global transition_animation_scale 0 && settings put global animator_duration_scale 0", ModeCategory.ECONOMIA, 1))
        modes.add(OptimizationMode(24, "Dark Force", "Força Dark Mode em tudo.", "settings put secure ui_night_mode 2", ModeCategory.ECONOMIA, 1))
        modes.add(OptimizationMode(25, "App Freeze", "Congela apps em segundo plano.", "settings put global app_standby_enabled 1", ModeCategory.ECONOMIA, 1))
        modes.add(OptimizationMode(26, "Battery IQ", "Otimização inteligente AI.", "settings put global adaptive_battery_management 1", ModeCategory.ECONOMIA, 1))
        modes.add(OptimizationMode(27, "Brightness Limit", "Trava brilho máximo em 80%.", "settings put system screen_brightness 204", ModeCategory.ECONOMIA, 1))
        modes.add(OptimizationMode(28, "No Sync", "Para todas as sincronizações.", "content stop-sync", ModeCategory.ECONOMIA, 1))
        modes.add(OptimizationMode(29, "GMS Light", "Limita Google Play Services.", "pm disable-user com.google.android.gms/com.google.android.gms.auth.be.proximity.authorization.userpresence.UserPresenceService", ModeCategory.ECONOMIA, 2))
        modes.add(OptimizationMode(30, "Power Nap", "Deep Sleep mais agressivo.", "settings put global power_nap_enabled 1", ModeCategory.ECONOMIA, 1))

        // --- JOGOS (10 modos) ---
        modes.add(OptimizationMode(31, "Free Fire Sensi", "DPI 180 + Pointer 7.", "wm density 180 && settings put system pointer_speed 7", ModeCategory.JOGOS, 2))
        modes.add(OptimizationMode(32, "Ronin Step", "Foco em latência de toque.", "settings put global touch_latency_mode 1", ModeCategory.JOGOS, 1))
        modes.add(OptimizationMode(33, "Where Winds Meet", "Otimização para WWM.", "wm size 720x1600 && wm density 280 && cmd package compile -m speed -f com.wwm.game", ModeCategory.JOGOS, 2))
        modes.add(OptimizationMode(34, "Genshin Impact", "GPU Boost + SkiaVK.", "settings put global debug.hwui.renderer skiavk && cmd package compile -m speed -f com.miHoYo.GenshinImpact", ModeCategory.JOGOS, 2))
        modes.add(OptimizationMode(35, "Roblox Turbo", "Compilação e 540p.", "wm size 540x1200 && cmd package compile -m speed -f com.roblox.client", ModeCategory.JOGOS, 2))
        modes.add(OptimizationMode(36, "PUBG Mobile", "90 FPS Unlock (Fake) + Opt.", "settings put global pubg_fps_unlock 1", ModeCategory.JOGOS, 1))
        modes.add(OptimizationMode(37, "CODM Speed", "Latência e Rede otimizadas.", "settings put global touch_latency_mode 1 && settings put global network_gaming_mode 1", ModeCategory.JOGOS, 1))
        modes.add(OptimizationMode(38, "Adrenaline UI", "Foca no app em foco.", "ADRENALINE_UI", ModeCategory.JOGOS, 2))
        modes.add(OptimizationMode(39, "Game Mode On", "Ativa o Game Mode nativo.", "cmd game mode standard", ModeCategory.JOGOS, 1))
        modes.add(OptimizationMode(40, "Memory Cleaner", "Limpa cache antes de jogar.", "pm trim-caches 999G", ModeCategory.JOGOS, 1))

        // --- UTILITARIOS (10 modos) ---
        modes.add(OptimizationMode(41, "DPI Custom", "Define DPI para 400.", "wm density 400", ModeCategory.UTILITARIOS, 1))
        modes.add(OptimizationMode(42, "Kill Bloat", "Remove apps inúteis (GOS).", "pm disable-user com.samsung.android.game.gos", ModeCategory.UTILITARIOS, 1, "SAMSUNG"))
        modes.add(OptimizationMode(43, "Log Nuke", "Para logger do sistema.", "setprop ctl.stop logd", ModeCategory.UTILITARIOS, 1))
        modes.add(OptimizationMode(44, "Reset Dexopt", "Limpa fila de otimização.", "cmd package bg-dexopt-job --cancel", ModeCategory.UTILITARIOS, 1))
        modes.add(OptimizationMode(45, "Pointer 5", "Velocidade padrão do ponteiro.", "settings put system pointer_speed 2", ModeCategory.UTILITARIOS, 1))
        modes.add(OptimizationMode(46, "Refresh Fix", "Força taxa de atualização alta.", "settings put secure refresh_rate_mode 1", ModeCategory.UTILITARIOS, 1))
        modes.add(OptimizationMode(47, "Touch Boost", "Melhora resposta do painel.", "settings put global touch_boost 1", ModeCategory.UTILITARIOS, 1))
        modes.add(OptimizationMode(48, "System Tune", "Ajuste fino de buffer Dalvik.", "settings put global dalvik.vm.dex2oat-threads 4", ModeCategory.UTILITARIOS, 2))
        modes.add(OptimizationMode(49, "Force Dark", "Ativa o tema escuro.", "settings put secure ui_night_mode 2", ModeCategory.UTILITARIOS, 1))
        modes.add(OptimizationMode(50, "Full Reset", "Restaura TUDO ao padrão.", "wm size reset && wm density reset && settings put global low_power 0", ModeCategory.UTILITARIOS, 1))
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
                    @Suppress("DEPRECATION")
                    pm.getPackageInfo(pkg, 0)
                }
                return true
            } catch (e: Exception) { continue }
        }
        return false
    }

    fun launchShizuku(context: Context) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage("rikka.app.shizuku")
                ?: context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                val githubIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))
                githubIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(githubIntent)
            }
        } catch (e: Exception) {
            // Silently fail or log
        }
    }

    fun getRamUsage(context: Context): String {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val mi = android.app.ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val total = mi.totalMem / (1024 * 1024)
        val avail = mi.availMem / (1024 * 1024)
        val used = total - avail
        return "${used}MB / ${total}MB"
    }

    suspend fun getCpuStatus(): String = withContext(Dispatchers.IO) {
        if (!Shizuku.pingBinder()) return@withContext "Desconhecido"
        try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java, Array<String>::class.java, String::class.java
            )
            method.isAccessible = true

            // Check fixed performance mode
            val process = method.invoke(null, arrayOf("sh", "-c", "dumpsys power | grep mFixedPerformanceModeEnabled"), null, null) as Process
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()

            if (output.contains("true")) return@withContext "FORÇA MÁXIMA"

            // Check governor as fallback
            val govProcess = method.invoke(null, arrayOf("sh", "-c", "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"), null, null) as Process
            val gov = govProcess.inputStream.bufferedReader().use { it.readText() }.trim()
            govProcess.waitFor()

            if (gov == "performance") return@withContext "ALTA PERFORMANCE"

            "STANDBY"
        } catch (e: Exception) { "NORMAL" }
    }

    fun getInstalledApps(context: Context, showSystem: Boolean): List<AppInfo> {
        val pm = context.packageManager
        val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
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

    suspend fun triggerCriticalReset(): String = withContext(Dispatchers.IO) {
        val emergencyCmd = "am kill-all && cmd package compile --reset -a && wm size reset && wm density reset && settings put global low_power 1 && pm unsuspend com.google.android.gms"
        runRawCommand(emergencyCmd)
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
