package com.catdiego.turbocore

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.*
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.*
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

// =========================================================================
// 1. HEALTH MANAGER (WATCHDOG 2.0 & SENSORS)
// =========================================================================
object HealthManager {
    var coolingUntil: Long = 0
    var useNativeThermal: Boolean = false
    private var lastTemp: Float = 0f
    private var lastTime: Long = 0
    var tempTrend: Float = 0f // delta T / delta t

    fun getRamPieData(context: Context): List<Float> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val total = mi.totalMem.toFloat()
        val avail = mi.availMem.toFloat()
        val used = total - avail
        // Retorna [Usado %, Livre %]
        return listOf(used / total, avail / total)
    }

    suspend fun applySmartMode(context: Context): String {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val totalGB = mi.totalMem / 1024 / 1024 / 1024

        return if (totalGB < 2) {
            PerformanceManager.runRawCommand("wm size 720x1280 && wm density 280")
        } else if (totalGB > 6) {
            PerformanceManager.applyDexSpeed("com.dts.freefireth")
        } else {
            PerformanceManager.runRawCommand("wm size reset && wm density reset")
        }
    }

    fun getTemperature(context: Context): Float {
        val currentTime = System.currentTimeMillis()

        // Tenta ler do sistema de arquivos (Thermal Zone) - V250
        var celsius = readSysThermal()
        if (celsius > 0) {
            useNativeThermal = true
        } else {
            // Fallback: Bateria
            useNativeThermal = false
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            celsius = temp / 10f
        }

        // Calculate trend (Predictive Cooling)
        if (lastTime > 0 && currentTime > lastTime) {
            val dt = (currentTime - lastTime) / 1000f // seconds
            val dT = celsius - lastTemp
            tempTrend = dT / dt // Celsius per second
        }
        lastTemp = celsius
        lastTime = currentTime

        if (celsius >= 40f) {
            coolingUntil = System.currentTimeMillis() + (3 * 60 * 1000)
        }

        return celsius
    }

    private fun readSysThermal(): Float {
        return try {
            val paths = arrayOf(
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/thermal/thermal_zone1/temp"
            )
            for (path in paths) {
                val file = java.io.File(path)
                if (file.exists()) {
                    val tempStr = file.readText().trim()
                    val temp = tempStr.toFloatOrNull() ?: 0f
                    if (temp > 1000) return temp / 1000f // millicelsius
                    if (temp > 0) return temp
                }
            }
            0f
        } catch (e: Exception) { 0f }
    }

    fun isCoolingDown(): Boolean {
        return System.currentTimeMillis() < coolingUntil
    }
}

class ThermalMonitorWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val temp = HealthManager.getTemperature(applicationContext)
        if (temp >= 40) {
            PerformanceManager.triggerCriticalReset()
        }
        Result.success()
    }
}

class ShizukuKeeperService : Service() {
    private val CHANNEL_ID = "shizuku_keeper"
    private val NOTIFICATION_ID = 99
    private var job: kotlinx.coroutines.Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Turbo Core: Always-On")
            .setContentText("Conexão Shizuku mantida para otimização.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        job?.cancel()
        job = kotlinx.coroutines.MainScope().launch {
            while(isActive) {
                Shizuku.pingBinder()
                delay(60000)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null
}

// =========================================================================
// 1.5 NETWORK MANAGER (V250 PING SHIELD)
// =========================================================================
object NetworkManager {
    var lastLatency: Int = 0

    fun getPing(context: Context): Int {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork
        val capabilities = cm.getNetworkCapabilities(activeNetwork)
        return if (capabilities != null) {
            // Heurística baseada em tecnologia de rede
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 15
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> 45
                else -> 100
            }
        } else 999
    }

    suspend fun optimizePing(): String {
        val cmd = "settings put global tcp_default_init_rwnd 10 && cmd netpolicy set restrict-background false && settings put global private_dns_mode hostname && settings put global private_dns_specifier 1dot1dot1dot1.cloudflare-dns.com"
        return PerformanceManager.runRawCommand(cmd)
    }

    suspend fun measureLatency(): Int {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val address = java.net.InetAddress.getByName("8.8.8.8")
                if (address.isReachable(2000)) {
                    val latency = (System.currentTimeMillis() - startTime).toInt()
                    lastLatency = latency
                    latency
                } else {
                    999
                }
            } catch (e: Exception) {
                999
            }
        }
    }
}

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: android.widget.TextView? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            return START_NOT_STICKY
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = android.widget.TextView(this).apply {
            text = "Temp: --°C"
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#80000000"))
            setPadding(20, 10, 20, 10)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
            y = 100
        }

        windowManager?.addView(overlayView, params)

        kotlinx.coroutines.MainScope().launch {
            while(true) {
                overlayView?.text = "Temp: ${HealthManager.getTemperature(this@OverlayService)}°C"
                delay(5000)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { windowManager?.removeView(it) }
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null
}

class AutomatedReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = level * 100 / scale.toFloat()

            if (batteryPct <= 15) {
                kotlinx.coroutines.GlobalScope.launch {
                    PerformanceManager.runRawCommand("settings put global low_power 1 && wm size reset")
                }
            }
        }
    }
}

class NativeThermalManager(private val context: Context) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun registerThermalListener(onSevereThermal: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            powerManager.addThermalStatusListener(context.mainExecutor) { status ->
                if (status >= PowerManager.THERMAL_STATUS_SEVERE) {
                    onSevereThermal()
                }
            }
        }
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
// 2.5 CONFIGURATION MANAGER (V250 SHARED CONFIGS)
// =========================================================================
object ConfigurationManager {
    fun exportSensiConfig(dpi: Int, speed: Int): String {
        return "{\"dpi\": $dpi, \"speed\": $speed, \"version\": \"V250\"}"
    }

    fun importSensiConfig(json: String): Pair<Int, Int> {
        return try {
            val dpi = json.substringAfter("\"dpi\":").substringBefore(",").trim().toInt()
            val speed = json.substringAfter("\"speed\":").substringBefore(",").trim().toInt()
            Pair(dpi, speed)
        } catch (e: Exception) {
            Pair(160, 7)
        }
    }
}

// =========================================================================
// 3. ENGINE ZUEIRA V206 (DATABASE DE 25 MODOS)
// =========================================================================
object SmartCoreEngineV250 {
    val brand: String = Build.MANUFACTURER.uppercase()
    private val modes = mutableListOf<OptimizationMode>()

    init {
        generateModes()
    }

    private fun generateModes() {
        // --- SAMSUNG (PRODUCT V250) ---
        modes.add(OptimizationMode(1, "Bixby no Vasco", "Manda a assistente inútil pra Série B.",
            "pm disable-user com.samsung.android.bixby.agent && am force-stop com.samsung.android.bixby.agent", ModeCategory.DEBLOAT, 1, "SAMSUNG"))
        modes.add(OptimizationMode(2, "Balanced FF Priority", "Prioridade total ao Free Fire via AppOps.",
            "cmd appops set com.dts.freefireth TOP_APP_OPS allow && cmd activity set-debug-app -w com.dts.freefireth", ModeCategory.CPU, 1, "SAMSUNG"))
        modes.add(OptimizationMode(3, "Modo Ex-Namorada", "Fria e Calculista: Mata processos pra esfriar.",
            "am kill-all && cmd package compile --reset -a", ModeCategory.POWER, 1, "SAMSUNG"))
        modes.add(OptimizationMode(4, "J7 Guerreiro", "Resolução 360p pra rodar liso igual sabão.",
            "wm size 360x740 && wm density 160", ModeCategory.GPU, 2, "SAMSUNG"))
        modes.add(OptimizationMode(5, "Tira o Lag da OneUI", "GOS Bypass + Drivers de Alto Desempenho.",
            "pm disable-user com.samsung.android.game.gos && settings put global game_driver_all_apps 1 && settings put global game_driver_opt_in 1 && setprop persist.sys.use_dali_system 0", ModeCategory.CHIMERA, 2, "SAMSUNG"))
        modes.add(OptimizationMode(6, "Tela Verde Fix", "Tenta salvar a tela AMOLED com filtro fake.",
            "settings put secure accessibility_display_daltonizer_enabled 1", ModeCategory.GPU, 1, "SAMSUNG"))
        modes.add(OptimizationMode(7, "Ram Plus é o KCT", "Desativa a RAM virtual que gasta memória.",
            "settings put global ram_expand_size_list 0", ModeCategory.CPU, 1, "SAMSUNG"))

        // --- XIAOMI (BALANCED V250) ---
        modes.add(OptimizationMode(8, "Poco Seguro", "Otimização térmica equilibrada.",
            "cmd thermalservice override 0 && settings put global thermal_limit_strategy 1", ModeCategory.CPU, 1, "XIAOMI"))
        modes.add(OptimizationMode(9, "Xing Ling Spyware", "Remove espionagem da MIUI (Joyose).",
            "pm disable-user com.xiaomi.joyose && pm disable-user com.miui.analytics", ModeCategory.DEBLOAT, 1, "XIAOMI"))
        modes.add(OptimizationMode(10, "iPhone da Shopee", "Animações 'fluidas' (Mentira).",
            "settings put global window_animation_scale 1.0 && settings put system power_mode 1", ModeCategory.GPU, 1, "XIAOMI"))
        modes.add(OptimizationMode(11, "BugUI Fix", "Reinicia a UI pra parar de piscar.",
            "am force-stop com.android.systemui", ModeCategory.CHIMERA, 2, "XIAOMI"))
        modes.add(OptimizationMode(12, "Mi Cloud Off", "Ninguém usa isso. Tchau.",
            "pm disable-user com.miui.cloudservice", ModeCategory.DEBLOAT, 1, "XIAOMI"))
        modes.add(OptimizationMode(13, "HyperOS Fake", "Muda a DPI pra parecer tablet.",
            "wm density 440", ModeCategory.GPU, 2, "XIAOMI"))
        modes.add(OptimizationMode(14, "Modo Tijolo", "Economia extrema. Vira peso de papel.",
            "cmd power set-mode 1 && settings put global low_power 1", ModeCategory.POWER, 2, "XIAOMI"))

        // --- UNIVERSAL (BALANCED V250) ---
        modes.add(OptimizationMode(15, "Batata Gamer", "Resolução 480p. Gráfico de Minecraft.",
            "wm size 480x960 && wm density 160", ModeCategory.GPU, 2))
        modes.add(OptimizationMode(16, "Sensi do Capa 👿", "DPI Alta + Ponteiro Rápido.",
            "wm density 180 && settings put system pointer_speed 7", ModeCategory.MIRA, 2))
        modes.add(OptimizationMode(17, "Modo Baiano", "Celular dorme imediatamente (Doze).",
            "dumpsys deviceidle force-idle", ModeCategory.POWER, 1))
        modes.add(OptimizationMode(18, "Vasco da Gama", "Cai o FPS, cai a resolução, cai tudo.",
            "cmd power set-fixed-performance-mode-enabled false", ModeCategory.POWER, 1))
        modes.add(OptimizationMode(19, "Otimizar FF Profile", "Otimiza apenas o perfil do Free Fire.",
            "cmd package compile -m speed-profile -f com.dts.freefireth", ModeCategory.CPU, 1))
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
            "wm size reset && wm density reset && cmd package compile --reset -a && cmd power set-fixed-performance-mode-enabled false", ModeCategory.CHIMERA, 1))

        // --- PREMIUM V250 ---
        modes.add(OptimizationMode(26, "Escudo de Ping", "Otimiza DNS e pacotes de rede.",
            "cmd netpolicy set restrict-background false && settings put global private_dns_specifier 1.1.1.1", ModeCategory.REDE, 1))
        modes.add(OptimizationMode(27, "Congelador de Apps", "Suspende apps que acordam sozinhos.",
            "pm suspend com.facebook.katana && pm suspend com.instagram.android", ModeCategory.DEBLOAT, 2))
        modes.add(OptimizationMode(28, "Perfil Adaptativo", "Ajusta CPU conforme a conexão.",
            "cmd power set-mode 0", ModeCategory.POWER, 1))
        modes.add(OptimizationMode(29, "Limpeza Inteligente", "Limpa cache de apps inativos.",
            "pm trim-caches 4096M", ModeCategory.CPU, 1))
        modes.add(OptimizationMode(30, "Modo Streamer", "Foco em GPU e bloqueio de avisos.",
            "settings put global notification_bubble 0 && cmd device_config put runtime_native_boot priority_sp_rel_to_sched 1", ModeCategory.GPU, 1))
    }

    fun getModesByCategory(category: ModeCategory): List<OptimizationMode> {
        return modes.filter { it.category == category && (it.requiresBrand == null || brand.contains(it.requiresBrand)) }
    }
}

// =========================================================================
// 4. PERFORMANCE MANAGER (ADB & GAME MODE API)
// =========================================================================
object PerformanceManager {
    fun setGameMode(context: Context, packageName: String, mode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val gameManager = context.getSystemService(Context.GAME_SERVICE) as GameManager
            // mode: 1 (Standard), 2 (Performance), 3 (Battery)
            // Note: Needs signature or system permission, but we try via API if available
        }
    }

    suspend fun applyDexSpeed(packageName: String): String {
        return runRawCommand("cmd package compile -m speed-profile -f $packageName")
    }

    suspend fun setAdaptivePriority(packageName: String): String {
        return runRawCommand("cmd appops set $packageName TOP_APP_OPS allow")
    }

    private var binderListener: Shizuku.OnBinderReceivedListener? = null

    fun registerBinderListener(onReceived: () -> Unit) {
        binderListener = Shizuku.OnBinderReceivedListener {
            onReceived()
        }
        Shizuku.addBinderReceivedListener(binderListener!!)
    }

    fun unregisterBinderListener() {
        binderListener?.let { Shizuku.removeBinderReceivedListener(it) }
    }

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
        return if (used > runtime.totalMemory() * 0.8) "CRÍTICO" else "ESTÁVEL"
    }

    // --- Execução ADB via Shizuku (Protocolo Samsung Safe Mode) ---
    suspend fun runRawCommand(command: String): String = withContext(Dispatchers.IO) {
        // Background priority check
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)

        if (!Shizuku.pingBinder()) {
            // Attempt to force re-bind check
            delay(1000)
            if (!Shizuku.pingBinder()) return@withContext "Erro: Shizuku OFF"
        }

        // V250 Architecture: Throttling Lockout via HealthManager
        if (HealthManager.isCoolingDown() && (command.contains("speed") || command.contains("allow") || command.contains("set-debug-app") || command.contains("game_driver"))) {
            return@withContext "BLOQUEIO TÉRMICO: Aguarde resfriamento (3 min)."
        }

        try {
            withTimeout(3000L) {
                // Protocolo Samsung Safe Mode: Knox relax delay
                if (SmartCoreEngineV250.brand.contains("SAMSUNG")) {
                    delay(2000)
                }

                val finalCommand = if (SmartCoreEngineV250.brand.contains("SAMSUNG")) {
                    "settings put global adb_wifi_enabled 1 && am force-stop com.samsung.android.lool && $command"
                } else {
                    command
                }

                val method = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java, Array<String>::class.java, String::class.java
                )
                method.isAccessible = true
                val process = method.invoke(null, arrayOf("sh", "-c", finalCommand), null, null) as java.lang.Process

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
        val cmd = "wm size reset && wm density reset && cmd package compile --reset -a && content stop-sync && setprop ctl.stop logd && cmd power set-fixed-performance-mode-enabled false && pm unsuspend com.google.android.gms"
        return runRawCommand(cmd)
    }

    suspend fun clearKernelLogs(): String {
        return runRawCommand("logcat -c && dmesg -c")
    }

    suspend fun getZramStatus(): String {
        return runRawCommand("cat /proc/swaps")
    }

    // --- Gerenciamento de Apps ---
    fun isUsageStatsPermissionGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getForegroundAppNative(context: Context): String {
        if (!isUsageStatsPermissionGranted(context)) return "Sem Permissão de Uso"

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time)

        return if (stats != null && stats.isNotEmpty()) {
            val sortedStats = stats.sortedByDescending { it.lastTimeUsed }
            sortedStats[0].packageName
        } else {
            "Desconhecido"
        }
    }

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

    fun detectBloatware(context: Context): List<String> {
        val bloatPackages = listOf(
            "com.samsung.android.bixby.agent",
            "com.samsung.android.game.gos",
            "com.facebook.katana",
            "com.xiaomi.joyose",
            "com.miui.analytics",
            "com.google.android.apps.wellbeing"
        )
        val pm = context.packageManager
        return bloatPackages.filter {
            try { pm.getPackageInfo(it, 0); true } catch (e: Exception) { false }
        }
    }
}
