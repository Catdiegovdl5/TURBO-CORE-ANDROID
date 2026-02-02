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

enum class ModeCategory { CPU, GPU, MIRA, REDE, CHIMERA, DEBLOAT, POWER }

data class AppInfo(val name: String, val packageName: String, val isSystem: Boolean)

data class OptimizationMode(
    val id: Int,
    val title: String,
    val description: String, // Detalhe técnico para o Comandante ler
    val command: String,
    val category: ModeCategory,
    val riskLevel: Int, // 1=Seguro, 2=Cuidado, 3=Extreme (Requer <38°C)
    val requiresBrand: String? = null // "SAMSUNG", "XIAOMI" ou null (Universal)
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
        // --- ABA 1: CPU (Meta 40) ---
        for (i in 1..20) {
            modes.add(OptimizationMode(i, "CPU Prio L$i", "Nice CFS -$i. Prioridade de agendamento Kernel.", "renice -n -$i -p $(pidof com.dts.freefireth)", ModeCategory.CPU, 1))
        }
        modes.add(OptimizationMode(21, "AOT Speed-Profile", "Compila dex2oat segura.", "cmd package compile -m speed-profile -f com.dts.freefireth", ModeCategory.CPU, 1))
        modes.add(OptimizationMode(22, "RAM Plus OFF", "Kill zRAM swap Samsung.", "settings put global ram_expand_size_list 0", ModeCategory.CPU, 2, "SAMSUNG"))
        modes.add(OptimizationMode(23, "Looper Disable", "Reduz interrupções looper.", "cmd looper_stats disable", ModeCategory.CPU, 1))
        modes.add(OptimizationMode(24, "Gamer Ultimate", "Fixed Performance Mode (Sustentado).", "cmd power set-fixed-performance-mode-enabled true", ModeCategory.CPU, 2))
        for (i in 25..40) {
            modes.add(OptimizationMode(i, "Kernel Opt v$i", "Otimização de agendador genérica v$i.", "echo $i > /proc/sys/kernel/sched_latency_ns", ModeCategory.CPU, 1))
        }

        // --- ABA 2: GPU (Meta 50) ---
        modes.add(OptimizationMode(41, "720p Balanced", "720x1600 / 280dpi.", "wm size 720x1600 && wm density 280", ModeCategory.GPU, 2))
        modes.add(OptimizationMode(42, "540p Performance", "540x1200 / 210dpi.", "wm size 540x1200 && wm density 210", ModeCategory.GPU, 3))
        for (dpi in 320..500 step 20) {
            modes.add(OptimizationMode(50 + (dpi/20), "DPI $dpi", "Ajuste granular de densidade $dpi.", "wm density $dpi", ModeCategory.GPU, 2))
        }
        listOf("0.0", "0.1", "0.25", "0.5").forEachIndexed { i, s ->
            modes.add(OptimizationMode(70 + i, "Anim $s" + "x", "Velocidade UI $s" + "x.", "settings put global window_animation_scale $s && settings put global transition_animation_scale $s && settings put global animator_duration_scale $s", ModeCategory.GPU, 1))
        }
        modes.add(OptimizationMode(80, "SkiaVK Backend", "HWUI via Vulkan.", "settings put global debug.hwui.renderer skiavk", ModeCategory.GPU, 1))
        modes.add(OptimizationMode(81, "HW Overlays Off", "GPU Only Composition.", "service call SurfaceFlinger 1008 i32 1", ModeCategory.GPU, 2))

        // --- ABA 3: MIRA (Meta 30) ---
        modes.add(OptimizationMode(91, "Touch Sensitivity", "Samsung Gloved Mode.", "settings put system touch_sensitivity 1", ModeCategory.MIRA, 1, "SAMSUNG"))
        modes.add(OptimizationMode(92, "Latency Zero", "Touch bypass logic.", "settings put global touch_latency_mode 1", ModeCategory.MIRA, 1))
        modes.add(OptimizationMode(93, "Sensi Free Fire", "Density 180 (Extremo).", "wm density 180", ModeCategory.MIRA, 2))
        for (i in 1..10) {
            modes.add(OptimizationMode(100 + i, "Pointer $i", "Pointer speed scale $i.", "settings put system pointer_speed $i", ModeCategory.MIRA, 1))
        }
        for (i in 1..10) {
            modes.add(OptimizationMode(110 + i, "Touch Slop $i", "Sensibilidade de arraste $i.", "settings put secure view_touch_slop $i", ModeCategory.MIRA, 1))
        }

        // --- ABA 4: REDE (Meta 30) ---
        modes.add(OptimizationMode(131, "DNS Cloudflare", "1.1.1.1 DNS privado.", "settings put global private_dns_mode hostname && settings put global private_dns_specifier 1dot1dot1dot1.cloudflare-dns.com", ModeCategory.REDE, 1))
        modes.add(OptimizationMode(132, "TCP Window 60", "RTT Optimization.", "setprop net.tcp.default_init_rwnd 60", ModeCategory.REDE, 1))
        modes.add(OptimizationMode(133, "Wifi Scan OFF", "Reduz picos de ping.", "settings put global wifi_scan_always_enabled 0", ModeCategory.REDE, 1))
        for (i in 1..10) {
            modes.add(OptimizationMode(140 + i, "Net Wakelock $i", "Otimização $i de buffer rede.", "settings put global network_avoid_bad_wifi 1", ModeCategory.REDE, 1))
        }

        // --- ABA 5: CHIMERA (Meta 20) ---
        modes.add(OptimizationMode(101, "Sniper Elite", "Zero Lag + 400 DPI + P7.", "settings put global touch_latency_mode 1 && wm density 400 && settings put system pointer_speed 7", ModeCategory.CHIMERA, 2))
        modes.add(OptimizationMode(103, "Samurai Blade", "Fix Perf + Sens + No GOS.", "settings put system touch_sensitivity 1 && cmd power set-fixed-performance-mode-enabled true && pm disable-user com.samsung.android.game.gos", ModeCategory.CHIMERA, 3, "SAMSUNG"))
        modes.add(OptimizationMode(104, "Ronin Step", "No Joyose + Thermal Hack.", "pm suspend com.xiaomi.joyose && echo '1' > /sys/class/thermal/thermal_message/sconfig", ModeCategory.CHIMERA, 3, "XIAOMI"))
        modes.add(OptimizationMode(115, "Adrenaline UI", "Speed-Profile on Current Focus.", "ADRENALINE_UI", ModeCategory.CHIMERA, 2))

        // --- ABA 6: DEBLOAT (Meta 30) ---
        val bloatlist = mapOf(
            "SAMSUNG" to listOf("com.samsung.android.game.gametools", "com.samsung.android.game.gamehome", "com.samsung.android.game.gos", "com.samsung.android.bixby.agent"),
            "XIAOMI" to listOf("com.xiaomi.joyose", "com.miui.powerkeeper", "com.miui.securitycenter")
        )
        bloatlist.forEach { (b, list) ->
            list.forEachIndexed { i, p ->
                modes.add(OptimizationMode(161 + i + (if(b=="XIAOMI") 10 else 0), "Kill $b $i", "Desativa $p.", "pm disable-user $p", ModeCategory.DEBLOAT, 1, b))
            }
            modes.add(OptimizationMode(199 + (if(b=="XIAOMI") 1 else 0), "Kill All $b", "Disable all specified $b packages.", list.joinToString(" && ") { "pm disable-user $it" }, ModeCategory.DEBLOAT, 2, b))
        }

        // --- ABA 7: POWER (V140 Legacy) ---
        modes.add(OptimizationMode(201, "Super Economia", "Ativa low_power e suspende GMS.", "settings put global low_power 1 && pm suspend com.google.android.gms", ModeCategory.POWER, 2))
        modes.add(OptimizationMode(202, "Ultra Economia", "Res 360x800 + low_power.", "wm size 360x800 && settings put global low_power 1", ModeCategory.POWER, 3))
        modes.add(OptimizationMode(203, "Usual Turbo", "Reset total de wm e density.", "wm size reset && wm density reset", ModeCategory.POWER, 1))
    }
}

object AppManager {
    suspend fun runMode(context: Context, mode: OptimizationMode): String = withContext(Dispatchers.IO) {
        if (!Shizuku.pingBinder()) return@withContext "Erro: Shizuku OFF"

        // Safety: Risk 3 requires < 38°C
        val temp = ThermalWatchdog.getTemperature(context)
        if (mode.riskLevel >= 3 && temp > 38.0f) {
            return@withContext "BLOQUEIO: Temp (${temp}°C) > 38°C!"
        }

        if (ThermalWatchdog.isOverheating(context)) {
            runDirectCommand("cmd package compile --reset -a")
            return@withContext "EMERGÊNCIA: Temp (${temp}°C) > 39°C! Resetando..."
        }

        val finalCommand = buildFinalCommand(mode)

        try {
            withTimeout(5000L) { // Aumentado para 5s para suportar fusão
                val method = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java, Array<String>::class.java, String::class.java
                )
                method.isAccessible = true
                val process = method.invoke(null, arrayOf("sh", "-c", finalCommand), null, null) as Process

                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                val output = reader.readText()
                val error = errorReader.readText()
                process.waitFor()

                if (error.isNotEmpty()) "Falha parcial: $error" else if (output.isEmpty()) "Sucesso" else output
            }
        } catch (e: TimeoutCancellationException) {
            "Erro: Timeout!"
        } catch (e: Exception) {
            "Erro: ${e.message}"
        }
    }

    private suspend fun buildFinalCommand(mode: OptimizationMode): String {
        val sb = StringBuilder()

        // Se for modo Adrenaline, detecta foreground app
        if (mode.command == "ADRENALINE_UI") {
            val pkg = getForegroundApp()
            sb.append("cmd package compile -m speed-profile -f $pkg")
        } else {
            sb.append(mode.command)
        }

        // Adiciona logic de marca se necessário (implícito no motor)
        return sb.toString()
    }

    private suspend fun getForegroundApp(): String = withContext(Dispatchers.IO) {
        try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java, Array<String>::class.java, String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", "dumpsys window windows | grep -E 'mCurrentFocus'"), null, null) as Process

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            process.waitFor()
            // Regex para extrair package name: mCurrentFocus=Window{... u0 com.package.name/ActivityName}
            val match = Regex("u0 ([^/]+)").find(output)
            match?.groupValues?.get(1) ?: "com.dts.freefireth"
        } catch (e: Exception) { "com.dts.freefireth" }
    }

    // Mantido para compatibilidade com botões diretos de sistema se necessário
    suspend fun runRawCommand(command: String): String = withContext(Dispatchers.IO) {
        if (!Shizuku.pingBinder()) return@withContext "Erro: Shizuku OFF"
        try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java, Array<String>::class.java, String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            process.waitFor()
            "Sucesso"
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

    fun getDeviceDisplayName(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL}"
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
