package com.catdiego.turbocore

import android.content.Context
import android.content.SharedPreferences
import android.app.ActivityManager
import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
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

data class DashboardUiState(
    val temperature: Float = 0f,
    val ramUsage: String = "-- / --",
    val ramPercent: Float = 0f,
    val ping: Long = 0L,
    val logText: String = "Sistema pronto. Aguardando comandos...",
    val isCritical: Boolean = false,
    val pollingRate: Long = 5000L,
    val isShizukuReady: Boolean = false,
    val userMessage: String? = null,
    val showSafetyDialog: Boolean = false,
    val selectedProfile: Profile? = null,
    val pendingResolution: String? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var monitoringJob: Job? = null
    private val prefs: SharedPreferences = application.getSharedPreferences("turbo_core_prefs", Context.MODE_PRIVATE)

    // Adaptive Polling Constants
    private val INTERVAL_NORMAL = 5000L
    private val INTERVAL_CRITICAL = 15000L
    private val TEMP_CRITICAL_THRESHOLD = 40.0f

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

        // Load active profile name from persistence
        val savedProfileName = prefs.getString("active_profile_name", "Balanceado")
        val savedProfile = profiles.find { it.name == savedProfileName }
        if (savedProfile != null) {
            _uiState.update { it.copy(selectedProfile = savedProfile) }
        }

        // Watchdog Init Check
        if (prefs.getBoolean("is_dirty", false)) {
            _uiState.update { it.copy(showSafetyDialog = true) }
        }
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

    fun applyProfile(profile: Profile) {
        _uiState.update { it.copy(selectedProfile = profile) }
        prefs.edit().putString("active_profile_name", profile.name).apply()

        viewModelScope.launch {
            val sb = StringBuilder()
            var calculatedResString: String? = null

            // Dynamic Resolution Logic
            if (profile.scale != 1.0f || profile.dpi != null) {
                // Safety Flag for Display Changes
                prefs.edit().putBoolean("is_dirty", true).apply()

                // Fetch Physical Resolution
                val physicalRes = withContext(Dispatchers.IO) { ShellEngine.getPhysicalResolution() }

                if (physicalRes != null && profile.scale != 1.0f) {
                    val newWidth = (physicalRes.first * profile.scale).toInt()
                    val newHeight = (physicalRes.second * profile.scale).toInt()

                    // Sanity check
                    if (newWidth > 200 && newHeight > 200) {
                        sb.append("wm size ${newWidth}x${newHeight}")
                        calculatedResString = "${newWidth}x${newHeight}"
                    } else {
                        // Fallback to safe defaults if calculation is weird
                        if (profile.name == "Turbo") sb.append("wm size 540x1200")
                        else if (profile.name == "Eco") sb.append("wm size 480x1066")
                        else sb.append("wm size reset")
                    }
                } else if (profile.scale == 1.0f) {
                     sb.append("wm size reset")
                } else {
                    // Fallback if physical res fetch failed
                    if (profile.name == "Turbo") sb.append("wm size 540x1200")
                    else if (profile.name == "Eco") sb.append("wm size 480x1066")
                    else sb.append("wm size reset")
                }

                // Density Command
                if (profile.dpi != null) {
                    sb.append(" && wm density ${profile.dpi}")
                } else {
                    sb.append(" && wm density reset")
                }

                // Show dialog with calculated resolution
                _uiState.update { it.copy(showSafetyDialog = true, pendingResolution = calculatedResString) }
            } else {
                // Balanced / Default
                sb.append("wm size reset && wm density reset")
            }

            if (profile.powerMode != null) {
                sb.append(" && settings put global low_power ${profile.powerMode}")
            }

            if (profile.trimRam) {
                sb.append(" && cmd activity trim-caches 20")
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
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val tempC = tempRaw / 10.0f

            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)

            val totalMemBytes = memInfo.totalMem
            val usedMemBytes = totalMemBytes - memInfo.availMem

            val totalMemGB = totalMemBytes.toFloat() / (1024 * 1024 * 1024)
            val usedMemGB = usedMemBytes.toFloat() / (1024 * 1024 * 1024)
            val ramPercent = (usedMemBytes.toFloat() / totalMemBytes.toFloat()) * 100

            val ramStr = String.format("%.1fGB / %.1fGB", usedMemGB, totalMemGB)
            val isCritical = tempC >= TEMP_CRITICAL_THRESHOLD

            val pingMs = measurePing()

            _uiState.update {
                it.copy(
                    temperature = tempC,
                    ramUsage = ramStr,
                    ramPercent = ramPercent,
                    ping = pingMs,
                    isCritical = isCritical
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(logText = "Erro ao ler sensores: ${e.message}") }
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

    // Explicit reset method for watchdog
    fun performWatchdogReset() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                ShellEngine.runCommand("wm size reset && wm density reset")
            }
            prefs.edit().putBoolean("is_dirty", false).apply()
            prefs.edit().remove("active_profile_name").apply() // Clear active profile persistence

            val balanced = profiles.find { it.name == "Balanceado" }
            _uiState.update { it.copy(
                logText = "Recuperação automática executada com sucesso.",
                showSafetyDialog = false,
                selectedProfile = balanced
            ) }
        }
    }
}
