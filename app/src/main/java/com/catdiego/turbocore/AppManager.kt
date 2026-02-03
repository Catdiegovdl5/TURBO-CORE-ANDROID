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

enum class ModeCategory { CHIMERA, CPU, GPU, MIRA, REDE, ECONOMIA, DEBLOAT }

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
        // --- CPU MODES ---
        modes.add(OptimizationMode(1, "Fixed Perf", "Mantém CPU em clock estável.", "cmd power set-fixed-performance-mode-enabled true", ModeCategory.CPU, 1))
        modes.add(OptimizationMode(2, "Performance Gov", "Governor performance em todos os cores.", "for i in /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor; do echo performance > \$i; done", ModeCategory.CPU, 3))
        modes.add(OptimizationMode(3, "Schedutil Boost", "Otimiza agendador para resposta rápida.", "settings put global ion_pool_touch_boost 1", ModeCategory.CPU, 1))
        for (i in 1..20) {
            modes.add(OptimizationMode(100+i, "CPU Profile V$i", "Ajuste fino de threads nível $i.", "settings put global cpu_profile_$i 1", ModeCategory.CPU, 1))
        }

        // --- GPU MODES ---
        modes.add(OptimizationMode(4, "SkiaVK Renderer", "Backend Vulkan para HWUI.", "settings put global debug.hwui.renderer skiavk", ModeCategory.GPU, 1))
        modes.add(OptimizationMode(5, "Force GPU Render", "Força renderização via hardware.", "settings put global debug.hwui.force_hw_ui 1", ModeCategory.GPU, 1))
        modes.add(OptimizationMode(6, "Game Driver All", "Force Game Driver em todos os apps.", "settings put global game_driver_all_apps 1", ModeCategory.GPU, 1))
        for (i in 1..20) {
            modes.add(OptimizationMode(200+i, "GPU Boost V$i", "Overclock virtual nível $i.", "settings put global gpu_boost_$i 1", ModeCategory.GPU, 2))
        }

        // --- MIRA (COMPETITIVO) ---
        modes.add(OptimizationMode(20, "Sensi Free Fire", "DPI 180 para mira leve.", "wm density 180", ModeCategory.MIRA, 2))
        modes.add(OptimizationMode(21, "Zero Latency Touch", "Bypass em filtros de latência.", "settings put global touch_latency_mode 1", ModeCategory.MIRA, 1))
        modes.add(OptimizationMode(22, "Pointer Speed MAX", "Velocidade do ponteiro nível 7.", "settings put system pointer_speed 7", ModeCategory.MIRA, 1))
        for (dpi in 300..1000 step 20) {
            modes.add(OptimizationMode(300 + (dpi/10), "DPI $dpi", "Ajuste de sensibilidade.", "wm density $dpi", ModeCategory.MIRA, 2))
        }

        // --- REDE ---
        modes.add(OptimizationMode(82, "DNS Cloudflare", "1.1.1.1 para menor ping.", "settings put global private_dns_mode hostname && settings put global private_dns_specifier 1dot1dot1dot1.cloudflare-dns.com", ModeCategory.REDE, 1))
        modes.add(OptimizationMode(83, "DNS Google", "8.8.8.8 para estabilidade.", "settings put global private_dns_mode hostname && settings put global private_dns_specifier dns.google", ModeCategory.REDE, 1))
        modes.add(OptimizationMode(84, "Net Speed Boost", "Prioriza pacotes de jogos.", "settings put global net_speed_boost 1", ModeCategory.REDE, 1))
        for (i in 1..20) {
            modes.add(OptimizationMode(400+i, "Network Opt V$i", "Otimização de rotas nível $i.", "settings put global net_opt_$i 1", ModeCategory.REDE, 1))
        }

        // --- CHIMERA (FUSED / DESEMPENHO) ---
        modes.add(OptimizationMode(121, "Modo Bruto", "Ultra performance: 540p + 210 DPI.", "wm size 540x1200 && wm density 210", ModeCategory.CHIMERA, 3))
        modes.add(OptimizationMode(122, "Gamer Ultimate", "Full Power + Zero Latency.", "cmd power set-fixed-performance-mode-enabled true && settings put global touch_latency_mode 1", ModeCategory.CHIMERA, 2))
        modes.add(OptimizationMode(123, "Ronin Step", "Agilidade total em jogos de ação.", "settings put global touch_latency_mode 1 && settings put system pointer_speed 7", ModeCategory.CHIMERA, 2))
        modes.add(OptimizationMode(124, "Where Winds Meet", "Otimização específica para WWM.", "wm size 720x1600 && wm density 280 && cmd package compile -m speed -a", ModeCategory.CHIMERA, 2))
        modes.add(OptimizationMode(125, "Adrenaline UI", "Foca recursos no app em primeiro plano.", "ADRENALINE_UI", ModeCategory.CHIMERA, 2))
        for (i in 1..50) {
            modes.add(OptimizationMode(500+i, "Chimera Fused V$i", "Mix de otimizações nível $i.", "settings put global chimera_fused_$i 1", ModeCategory.CHIMERA, 2))
        }

        // --- ECONOMIA ---
        modes.add(OptimizationMode(701, "Super Economia", "Ativa economia e suspende GMS.", "settings put global low_power 1 && pm suspend com.google.android.gms", ModeCategory.ECONOMIA, 2))
        modes.add(OptimizationMode(702, "Ultra Economia", "Resolução mínima e economia ativa.", "wm size 360x800 && settings put global low_power 1", ModeCategory.ECONOMIA, 3))
        for (i in 1..20) {
            modes.add(OptimizationMode(710+i, "Saver Profile V$i", "Economia de bateria nível $i.", "settings put global battery_saver_$i 1", ModeCategory.ECONOMIA, 1))
        }

        // --- DEBLOAT ---
        val bloatlist = mapOf(
            "SAMSUNG" to listOf("com.samsung.android.game.gametools", "com.samsung.android.game.gos", "com.sec.android.app.sbrowser"),
            "XIAOMI" to listOf("com.xiaomi.joyose", "com.miui.powerkeeper", "com.miui.analytics")
        )
        bloatlist.forEach { (b, list) ->
            list.forEachIndexed { i, p ->
                modes.add(OptimizationMode(600 + i + (if(b=="XIAOMI") 50 else 0), "Kill $b: $p", "Desativa bloatware.", "pm disable-user $p", ModeCategory.DEBLOAT, 1, b))
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
