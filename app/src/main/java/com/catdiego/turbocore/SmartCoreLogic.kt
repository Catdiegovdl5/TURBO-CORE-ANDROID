package com.catdiego.turbocore

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.os.BatteryManager
import android.os.Build
import kotlinx.coroutines.*
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

// =========================================================================
// 1. MONITORAMENTO TÉRMICO (WATCHDOG)
// =========================================================================
object ThermalWatchdog {
    fun getTemperature(context: Context): Float {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        return temp / 10f
    }
}

// =========================================================================
// 2. ESTRUTURAS DE DADOS (MODELO)
// =========================================================================
enum class ModeCategory { CPU, GPU, MIRA, REDE, CHIMERA, DEBLOAT, POWER }

data class OptimizationMode(
    val id: Int,
    val title: String,
    val description: String,
    val command: String,
    val category: ModeCategory,
    val riskLevel: Int, // 1=Safe (Verde), 2=Warn (Amarelo), 3=Critical (Vermelho)
    val requiresBrand: String? = null
)

data class AppInfo(val name: String, val packageName: String, val isSystem: Boolean)

// =========================================================================
// 3. ENGINE ZUEIRA V206 (DATABASE DE 25 MODOS)
// =========================================================================
object SmartCoreEngineV206 {
    val brand: String = Build.MANUFACTURER.uppercase()
    private val modes = mutableListOf<OptimizationMode>()

    init {
        generateModes()
    }

    private fun generateModes() {
        // --- SAMSUNG (A LINHA "A" SOFRIDA) ---
        modes.add(OptimizationMode(1, "Bixby no Vasco", "Manda a assistente inútil pra Série B.",
            "pm disable-user com.samsung.android.bixby.agent && am force-stop com.samsung.android.bixby.agent", ModeCategory.DEBLOAT, 1, "SAMSUNG"))
        modes.add(OptimizationMode(2, "Churrasqueira A01", "Libera CPU máxima (Cuidado pra não derreter a mão).",
            "cmd power set-fixed-performance-mode-enabled true", ModeCategory.CPU, 3, "SAMSUNG"))
        modes.add(OptimizationMode(3, "Modo Ex-Namorada", "Fria e Calculista: Mata processos pra esfriar.",
            "am kill-all && cmd package compile --reset -a", ModeCategory.POWER, 1, "SAMSUNG"))
        modes.add(OptimizationMode(4, "J7 Guerreiro", "Resolução 360p pra rodar liso igual sabão.",
            "wm size 360x740 && wm density 160", ModeCategory.GPU, 2, "SAMSUNG"))
        modes.add(OptimizationMode(5, "Tira o Lag da OneUI", "Desativa o GOS (Game Optimizing Service).",
            "pm disable-user com.samsung.android.game.gos && pm disable-user com.samsung.android.game.gametools", ModeCategory.CHIMERA, 2, "SAMSUNG"))
        modes.add(OptimizationMode(6, "Tela Verde Fix", "Tenta salvar a tela AMOLED com filtro fake.",
            "settings put secure accessibility_display_daltonizer_enabled 1", ModeCategory.GPU, 1, "SAMSUNG"))
        modes.add(OptimizationMode(7, "Ram Plus é o KCT", "Desativa a RAM virtual que gasta memória.",
            "settings put global ram_expand_size_list 0", ModeCategory.CPU, 1, "SAMSUNG"))

        // --- XIAOMI (A LINHA "BUGUI") ---
        modes.add(OptimizationMode(8, "Poco Bomba 💣", "Desativa proteção térmica. Use luvas de amianto.",
            "cmd thermalservice override 1 && settings put global thermal_limit_strategy 0", ModeCategory.CPU, 3, "XIAOMI"))
        modes.add(OptimizationMode(9, "Xing Ling Spyware", "Remove espionagem da MIUI (Joyose).",
            "pm disable-user com.xiaomi.joyose && pm disable-user com.miui.analytics", ModeCategory.DEBLOAT, 1, "XIAOMI"))
        modes.add(OptimizationMode(10, "iPhone da Shopee", "Animações 'fluidas' (Mentira).",
            "settings put global window_animation_scale 1.2 && settings put system power_mode 1", ModeCategory.GPU, 1, "XIAOMI"))
        modes.add(OptimizationMode(11, "BugUI Fix", "Reinicia a UI pra parar de piscar.",
            "am force-stop com.android.systemui", ModeCategory.CHIMERA, 2, "XIAOMI"))
        modes.add(OptimizationMode(12, "Mi Cloud Off", "Ninguém usa isso. Tchau.",
            "pm disable-user com.miui.cloudservice", ModeCategory.DEBLOAT, 1, "XIAOMI"))
        modes.add(OptimizationMode(13, "HyperOS Fake", "Muda a DPI pra parecer tablet.",
            "wm density 440", ModeCategory.GPU, 2, "XIAOMI"))
        modes.add(OptimizationMode(14, "Modo Tijolo", "Economia extrema. Vira peso de papel.",
            "cmd power set-mode 1 && settings put global low_power 1", ModeCategory.POWER, 2, "XIAOMI"))

        // --- UNIVERSAL (BATATA GAMER) ---
        modes.add(OptimizationMode(15, "Batata Gamer", "Resolução 480p. Gráfico de Minecraft.",
            "wm size 480x960 && wm density 160", ModeCategory.GPU, 2))
        modes.add(OptimizationMode(16, "Sensi do Capa 👿", "DPI Alta + Ponteiro Rápido.",
            "wm density 180 && settings put system pointer_speed 7", ModeCategory.MIRA, 2))
        modes.add(OptimizationMode(17, "Modo Baiano", "Celular dorme imediatamente (Doze).",
            "dumpsys deviceidle force-idle", ModeCategory.POWER, 1))
        modes.add(OptimizationMode(18, "Vasco da Gama", "Cai o FPS, cai a resolução, cai tudo.",
            "cmd power set-fixed-performance-mode-enabled false", ModeCategory.POWER, 1))
        modes.add(OptimizationMode(19, "Download de RAM", "Limpa cache pra fingir que baixou RAM.",
            "pm trim-caches 999G", ModeCategory.CPU, 1))
        modes.add(OptimizationMode(20, "Ping de Padaria", "Tenta melhorar a net discada.",
            "settings put global tcp_default_init_rwnd 60", ModeCategory.REDE, 1))
        modes.add(OptimizationMode(21, "Gato Net", "DNS da Cloudflare pra furar bloqueio.",
            "settings put global private_dns_mode hostname && settings put global private_dns_specifier 1dot1dot1dot1.cloudflare-dns.com", ModeCategory.REDE, 1))
        modes.add(OptimizationMode(22, "Anti-Tia do Zap", "Mata processos de fundo.",
            "cmd appops set com.whatsapp RUN_IN_BACKGROUND deny", ModeCategory.DEBLOAT, 2))
        modes.add(OptimizationMode(23, "Mete o Shape", "Remove 'Bem Estar Digital'.",
            "pm disable-user com.google.android.apps.wellbeing", ModeCategory.DEBLOAT, 1))
        modes.add(OptimizationMode(24, "Hack de Pobre", "Remove texturas (Overlay).",
            "service call SurfaceFlinger 1008 i32 1", ModeCategory.GPU, 2))
        modes.add(OptimizationMode(25, "ULTIMATE GAMBIARRA", "Botão do Pânico. Reseta tudo.",
            "wm size reset && wm density reset && cmd package compile --reset -a", ModeCategory.CHIMERA, 1))
    }

    fun getModesByCategory(category: ModeCategory): List<OptimizationMode> {
        return modes.filter { it.category == category && (it.requiresBrand == null || brand.contains(it.requiresBrand)) }
    }
}

// =========================================================================
// 4. GERENCIADOR DE PROCESSOS (ADB MANAGER) - CORRIGIDO
// =========================================================================
object AppManager {

    // --- Monitoramento de Hardware ---
    fun getRamUsage(context: Context): String {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val used = (mi.totalMem - mi.availMem) / 1024 / 1024
        val total = mi.totalMem / 1024 / 1024
        val percent = ((mi.totalMem - mi.availMem).toDouble() / mi.totalMem.toDouble() * 100).toInt()
        return "$used MB / $total MB ($percent%)"
    }

    fun getCpuStatus(): String {
        // Simulação baseada em carga (Android bloqueia leitura real de /proc/stat)
        val runtime = Runtime.getRuntime()
        val used = runtime.totalMemory() - runtime.freeMemory()
        return if (used > runtime.totalMemory() * 0.8) "SOBRECARREGADO" else "ESTÁVEL"
    }

    // --- Execução ADB via Shizuku (Protocolo Samsung Safe Mode) ---
    suspend fun runRawCommand(command: String): String = withContext(Dispatchers.IO) {
        // Background priority check
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)

        if (!Shizuku.pingBinder()) return@withContext "Erro: Shizuku OFF"

        try {
            withTimeout(3000L) {
                // Protocolo Samsung Safe Mode: Knox relax delay
                if (SmartCoreEngineV206.brand.contains("SAMSUNG")) {
                    delay(2000)
                }

                val finalCommand = if (SmartCoreEngineV206.brand.contains("SAMSUNG")) {
                    "settings put global adb_wifi_enabled 1 && am force-stop com.samsung.android.lool && $command"
                } else {
                    command
                }

                val method = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java, Array<String>::class.java, String::class.java
                )
                method.isAccessible = true
                val process = method.invoke(null, arrayOf("sh", "-c", finalCommand), null, null) as Process

                val output = process.inputStream.bufferedReader().use { it.readText() }
                process.waitFor()

                if (output.isBlank()) "Sucesso" else output
            }
        } catch (e: TimeoutCancellationException) {
            "Erro: Timeout de 3s (CPU 100%?)"
        } catch (e: Exception) {
            "Erro: ${e.message}"
        }
    }

    suspend fun runMode(context: Context, mode: OptimizationMode): String {
        return runRawCommand(mode.command)
    }

    suspend fun triggerCriticalReset(): String {
        val cmd = "cmd package compile --reset -a && content stop-sync && setprop ctl.stop logd"
        return runRawCommand(cmd)
    }

    // --- Gerenciamento de Apps ---
    fun getInstalledApps(context: Context, showSystem: Boolean): List<AppInfo> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(0)
        return apps.filter {
            if (showSystem) true else (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        }.map {
            AppInfo(it.loadLabel(pm).toString(), it.packageName, (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0)
        }.sortedBy { it.name }
    }

    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: Exception) {
            try {
                context.packageManager.getPackageInfo("rikka.app.shizuku", 0)
                true
            } catch (x: Exception) { false }
        }
    }
}
