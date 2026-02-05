package com.catdiego.turbocore

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.app.ActivityManager
import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.net.InetSocketAddress
import java.net.Socket
import java.util.regex.Pattern

data class DashboardUiState(
    val temperature: Float = 0f,
    val ramUsage: String = "-- / --",
    val ramPercent: Float = 0f,
    val zramUsage: String = "ZRAM: --",
    val ping: Long = 0L,
    val currentMa: Int = 0,
    val logText: String = "Sistema pronto. Aguardando comandos...",
    val isCritical: Boolean = false,
    val pollingRate: Long = 5000L,
    val isShizukuReady: Boolean = false,
    val userMessage: String? = null,
    val showSafetyDialog: Boolean = false,
    val selectedProfile: Profile? = null,
    val pendingResolution: String? = null,
    val isFpsOverlayEnabled: Boolean = false,
    val myGames: Set<String> = emptySet(),
    val isGameActive: Boolean = false
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var monitoringJob: Job? = null
    private val prefs: SharedPreferences = application.getSharedPreferences("turbo_core_prefs", Context.MODE_PRIVATE)
    private var lastThermalNotificationTime: Long = 0L
    private var gameModeExitTimer: Job? = null
    private val GAME_MODE_NOTIFICATION_ID = 2002

    // Adaptive Polling Constants
    private val INTERVAL_NORMAL = 5000L
    private val INTERVAL_CRITICAL = 15000L
    private val TEMP_CRITICAL_THRESHOLD = 40.0f
    private val THERMAL_SHUTDOWN_THRESHOLD = 45.0f

    val profiles = listOf(
        Profile("Eco", 0.7f, null, 1, false, Color.Green),
        Profile("Balanceado", 1.0f, null, 0, false, Color(0xFF2196F3)), // Blue
        Profile("Turbo", 0.8f, 440, null, true, Color.Red)
    )

    // Shizuku Listeners
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _uiState.update { it.copy(isShizukuReady = false, userMessage = "Shizuku desconectado!") }
    }

    init {
        runCatching {
             Shizuku.addBinderDeadListener(binderDeadListener)
             if (ShellEngine.isAvailable()) {
                 _uiState.update { it.copy(isShizukuReady = true) }
             }
        }

        // Load active profile
        val savedProfileName = prefs.getString("active_profile_name", "Balanceado")
        val savedProfile = profiles.find { it.name == savedProfileName }
        if (savedProfile != null) {
            _uiState.update { it.copy(selectedProfile = savedProfile) }
        }

        // Load FPS overlay state
        val fpsEnabled = prefs.getBoolean("fps_overlay_enabled", false)
        if (fpsEnabled) {
             _uiState.update { it.copy(isFpsOverlayEnabled = true) }
        }

        // Load Games
        val games = prefs.getStringSet("my_games", emptySet()) ?: emptySet()
        _uiState.update { it.copy(myGames = games) }

        // Watchdog Init Check
        if (prefs.getBoolean("is_dirty", false)) {
            _uiState.update { it.copy(showSafetyDialog = true) }
        }

        createNotificationChannels()
    }

    override fun onCleared() {
        super.onCleared()
        runCatching {
            Shizuku.removeBinderDeadListener(binderDeadListener)
        }
    }

    fun updateShizukuStatus(isReady: Boolean) {
        _uiState.update { it.copy(isShizukuReady = isReady) }
    }

    fun confirmSafety() {
        prefs.edit().putBoolean("is_dirty", false).apply()
        _uiState.update { it.copy(showSafetyDialog = false) }
    }

    fun toggleFpsOverlay(enabled: Boolean) {
        val context = getApplication<Application>()
        if (enabled) {
            if (Settings.canDrawOverlays(context)) {
                context.startService(Intent(context, FpsOverlayService::class.java))
                _uiState.update { it.copy(isFpsOverlayEnabled = true) }
                prefs.edit().putBoolean("fps_overlay_enabled", true).apply()
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                _uiState.update { it.copy(userMessage = "Permita a sobreposição e tente novamente.") }
            }
        } else {
            context.stopService(Intent(context, FpsOverlayService::class.java))
            _uiState.update { it.copy(isFpsOverlayEnabled = false) }
            prefs.edit().putBoolean("fps_overlay_enabled", false).apply()
        }
    }

    fun addGame(packageName: String) {
        val current = _uiState.value.myGames.toMutableSet()
        current.add(packageName)
        _uiState.update { it.copy(myGames = current) }
        prefs.edit().putStringSet("my_games", current).apply()
    }

    fun removeGame(packageName: String) {
        val current = _uiState.value.myGames.toMutableSet()
        current.remove(packageName)
        _uiState.update { it.copy(myGames = current) }
        prefs.edit().putStringSet("my_games", current).apply()
    }

    fun applyProfile(profile: Profile) {
        _uiState.update { it.copy(selectedProfile = profile) }
        prefs.edit().putString("active_profile_name", profile.name).apply()

        viewModelScope.launch {
            val sb = StringBuilder()
            var calculatedResString: String? = null

            if (profile.scale != 1.0f || profile.dpi != null) {
                prefs.edit().putBoolean("is_dirty", true).apply()
                val physicalRes = withContext(Dispatchers.IO) { ShellEngine.getPhysicalResolution() }

                if (physicalRes != null && profile.scale != 1.0f) {
                    val newWidth = (physicalRes.first * profile.scale).toInt()
                    val newHeight = (physicalRes.second * profile.scale).toInt()
                    if (newWidth > 200 && newHeight > 200) {
                        sb.append("wm size ${newWidth}x${newHeight}")
                        calculatedResString = "${newWidth}x${newHeight}"
                    } else {
                        if (profile.name == "Turbo") sb.append("wm size 540x1200")
                        else if (profile.name == "Eco") sb.append("wm size 480x1066")
                        else sb.append("wm size reset")
                    }
                } else if (profile.scale == 1.0f) {
                     sb.append("wm size reset")
                } else {
                    if (profile.name == "Turbo") sb.append("wm size 540x1200")
                    else if (profile.name == "Eco") sb.append("wm size 480x1066")
                    else sb.append("wm size reset")
                }

                if (profile.dpi != null) sb.append(" && wm density ${profile.dpi}")
                else sb.append(" && wm density reset")

                _uiState.update { it.copy(showSafetyDialog = true, pendingResolution = calculatedResString) }
            } else {
                sb.append("wm size reset && wm density reset")
            }

            if (profile.powerMode != null) sb.append(" && settings put global low_power ${profile.powerMode}")
            if (profile.trimRam) sb.append(" && cmd activity trim-caches 20")
            if (profile.name == "Turbo") sb.append(" && cmd activity kill-all")

            // Kernel Tweaks (Swappiness)
            if (profile.name == "Turbo") {
                sb.append(" && echo 10 > /proc/sys/vm/swappiness")
            } else {
                sb.append(" && echo 60 > /proc/sys/vm/swappiness")
            }

            runOptimization(sb.toString(), "Perfil: ${profile.name}")
        }
    }

    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return

        monitoringJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                updateMetrics()
                val currentDelay = if (_uiState.value.isCritical) INTERVAL_CRITICAL else INTERVAL_NORMAL
                _uiState.update { it.copy(pollingRate = currentDelay) }
                delay(currentDelay)
            }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    private suspend fun updateMetrics() {
        val context = getApplication<Application>()
        try {
            // App Detection
            if (AppDetector.hasPermission(context)) {
                val topPackage = AppDetector.getTopPackage(context)
                val games = _uiState.value.myGames

                if (topPackage != null && games.contains(topPackage)) {
                    // Game Detected!
                    if (!_uiState.value.isGameActive) {
                        _uiState.update { it.copy(isGameActive = true) }
                        gameModeExitTimer?.cancel()
                        val turbo = profiles.find { it.name == "Turbo" }
                        if (turbo != null && _uiState.value.selectedProfile?.name != "Turbo") {
                            withContext(Dispatchers.Main) {
                                applyProfile(turbo)
                                showGameModeNotification(context, topPackage)
                            }
                        }
                    } else {
                        gameModeExitTimer?.cancel()
                    }
                } else if (_uiState.value.isGameActive) {
                    // Left game, start hysteresis
                    if (gameModeExitTimer?.isActive != true) {
                        gameModeExitTimer = viewModelScope.launch {
                            delay(30000) // 30s Hysteresis
                            _uiState.update { it.copy(isGameActive = false) }
                            withContext(Dispatchers.Main) { cancelGameModeNotification(context) }
                            val balanced = profiles.find { it.name == "Balanceado" }
                            if (balanced != null) {
                                withContext(Dispatchers.Main) { applyProfile(balanced) }
                            }
                        }
                    }
                }
            }

            // Sensors
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val tempC = tempRaw / 10.0f

            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)

            val totalMemBytes = memInfo.totalMem
            val usedMemBytes = totalMemBytes - memInfo.availMem
            val usedMemGB = usedMemBytes.toFloat() / (1024 * 1024 * 1024)
            val totalMemGB = totalMemBytes.toFloat() / (1024 * 1024 * 1024)
            val ramPercent = (usedMemBytes.toFloat() / totalMemBytes.toFloat()) * 100
            val ramStr = String.format("%.1fGB / %.1fGB", usedMemGB, totalMemGB)

            val isCritical = tempC >= TEMP_CRITICAL_THRESHOLD

            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val currentMicroA = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            val currentMa = currentMicroA / 1000

            val pingMs = measurePing()

            // ZRAM Telemetry
            var zramStr = "ZRAM: --"
            if (!isCritical) { // Only read shell if not critical to save CPU
                val zramInfo = getZramInfo()
                if (zramInfo != null) {
                    zramStr = "ZRAM: ${zramInfo.first}%"
                }
            }

            _uiState.update {
                it.copy(
                    temperature = tempC,
                    ramUsage = ramStr,
                    ramPercent = ramPercent,
                    zramUsage = zramStr,
                    ping = pingMs,
                    currentMa = currentMa,
                    isCritical = isCritical
                )
            }

            // Thermal Watchdog 2.0 with Hysteresis (5 minutes)
            if (tempC >= THERMAL_SHUTDOWN_THRESHOLD) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastThermalNotificationTime > 5 * 60 * 1000) {
                    val ecoProfile = profiles.find { it.name == "Eco" }
                    if (ecoProfile != null && _uiState.value.selectedProfile?.name != "Eco") {
                        withContext(Dispatchers.Main) {
                            applyProfile(ecoProfile)
                            showThermalNotification(context, tempC)
                            _uiState.update { it.copy(userMessage = "Superaquecimento! Modo Eco ativado.") }
                            lastThermalNotificationTime = currentTime
                        }
                    }
                }
            }

        } catch (e: Exception) {
            _uiState.update { it.copy(logText = "Erro ao ler sensores: ${e.message}") }
        }
    }

    private suspend fun getZramInfo(): Pair<Int, Long>? {
        return withContext(Dispatchers.IO) {
            try {
                val output = ShellEngine.runCommand("cat /proc/meminfo")
                if (output.startsWith("Erro")) return@withContext null

                var swapTotal = 0L
                var swapFree = 0L

                output.lineSequence().forEach { line ->
                    if (line.startsWith("SwapTotal:")) {
                        swapTotal = line.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
                    } else if (line.startsWith("SwapFree:")) {
                        swapFree = line.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
                    }
                }

                if (swapTotal > 0) {
                    val used = swapTotal - swapFree
                    val percent = ((used.toFloat() / swapTotal.toFloat()) * 100).toInt()
                    Pair(percent, used)
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun measurePing(): Long {
         return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val socket = Socket()
                socket.connect(InetSocketAddress("8.8.8.8", 53), 2000)
                socket.close()
                System.currentTimeMillis() - startTime
            } catch (e: Exception) {
                -1L
            }
        }
    }

    fun runOptimization(command: String, description: String) {
        viewModelScope.launch {
            if (command.contains("wm size") || command.contains("wm density")) {
                prefs.edit().putBoolean("is_dirty", true).apply()
            }

            _uiState.update { it.copy(logText = "Executando: $description...") }

            try {
                 val output = withContext(Dispatchers.IO) {
                    ShellEngine.runCommand(command)
                }

                if (output.startsWith("Erro")) {
                    _uiState.update { it.copy(
                        logText = "[$description] FALHA: $output",
                        userMessage = "O motor Shizuku parou. Reinicie o serviço."
                    ) }
                } else {
                    _uiState.update { it.copy(logText = "[$description]: $output") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    logText = "[$description] EXCEÇÃO: ${e.message}",
                    userMessage = "O motor Shizuku parou. Reinicie o serviço."
                ) }
            }
        }
    }

    fun performWatchdogReset() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                ShellEngine.runCommand("wm size reset && wm density reset && echo 60 > /proc/sys/vm/swappiness")
            }
            prefs.edit().putBoolean("is_dirty", false).apply()
            prefs.edit().remove("active_profile_name").apply()

            val balanced = profiles.find { it.name == "Balanceado" }
            _uiState.update { it.copy(
                logText = "Recuperação automática executada com sucesso.",
                showSafetyDialog = false,
                selectedProfile = balanced
            ) }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Thermal Channel
            val thermalChannel = NotificationChannel("THERMAL_ALERTS", "Avisos Térmicos", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notificações de proteção contra superaquecimento"
            }
            notificationManager.createNotificationChannel(thermalChannel)

            // Game Mode Channel
            val gameChannel = NotificationChannel("GAME_MODE", "Modo Jogo", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Notificação persistente de jogo ativo"
            }
            notificationManager.createNotificationChannel(gameChannel)
        }
    }

    private fun showThermalNotification(context: Context, temp: Float) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val builder = NotificationCompat.Builder(context, "THERMAL_ALERTS")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("ALERTA TÉRMICO: ${temp}°C")
            .setContentText("Modo Eco forçado para proteger o hardware.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(1001, builder.build())
        }
    }

    private fun showGameModeNotification(context: Context, packageName: String) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val builder = NotificationCompat.Builder(context, "GAME_MODE")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Modo Turbo Ativo")
            .setContentText("Jogo detectado: $packageName")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        with(NotificationManagerCompat.from(context)) {
            notify(GAME_MODE_NOTIFICATION_ID, builder.build())
        }
    }

    private fun cancelGameModeNotification(context: Context) {
        with(NotificationManagerCompat.from(context)) {
            cancel(GAME_MODE_NOTIFICATION_ID)
        }
    }
}
