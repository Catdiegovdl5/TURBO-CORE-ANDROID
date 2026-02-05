package com.catdiego.turbocore

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DashboardUiState(
    val temperature: Float = 0f,
    val ramUsage: String = "-- / --",
    val ramPercent: Float = 0f,
    val logText: String = "Sistema pronto. Aguardando comandos...",
    val isCritical: Boolean = false,
    val pollingRate: Long = 5000L
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var monitoringJob: Job? = null

    // Adaptive Polling Constants
    private val INTERVAL_NORMAL = 5000L
    private val INTERVAL_CRITICAL = 15000L
    private val TEMP_CRITICAL_THRESHOLD = 40.0f

    /**
     * Starts the adaptive polling loop.
     * Should be called when UI is visible (onResume).
     */
    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return

        monitoringJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                updateMetrics()

                // Adaptive Logic:
                // If Critical (Temp > 40C) -> Slow down to 15s to reduce heat
                // Else -> 5s for responsiveness
                val currentDelay = if (_uiState.value.isCritical) INTERVAL_CRITICAL else INTERVAL_NORMAL

                _uiState.update { it.copy(pollingRate = currentDelay) }
                delay(currentDelay)
            }
        }
    }

    /**
     * Stops the monitoring loop.
     * Should be called when UI goes background (onPause) or is destroyed.
     * This fulfills the requirement: "isBackground -> Long.MAX_VALUE // Pausa total"
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    private fun updateMetrics() {
        val context = getApplication<Application>()

        try {
            // 1. Temperature (Native BatteryManager) - Very Low CPU overhead
            // Using sticky intent is the most reliable way across devices
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val tempC = tempRaw / 10.0f

            // 2. RAM (Native ActivityManager)
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)

            val totalMemBytes = memInfo.totalMem
            val availMemBytes = memInfo.availMem
            val usedMemBytes = totalMemBytes - availMemBytes

            val totalMemGB = totalMemBytes.toFloat() / (1024 * 1024 * 1024)
            val usedMemGB = usedMemBytes.toFloat() / (1024 * 1024 * 1024)
            val ramPercent = (usedMemBytes.toFloat() / totalMemBytes.toFloat()) * 100

            val ramStr = String.format("%.1fGB / %.1fGB", usedMemGB, totalMemGB)
            val isCritical = tempC >= TEMP_CRITICAL_THRESHOLD

            _uiState.update {
                it.copy(
                    temperature = tempC,
                    ramUsage = ramStr,
                    ramPercent = ramPercent,
                    isCritical = isCritical
                )
            }
        } catch (e: Exception) {
            // Fail silently or log to UI if needed, but don't crash loop
            _uiState.update { it.copy(logText = "Erro ao ler sensores: ${e.message}") }
        }
    }

    fun runOptimization(command: String, description: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(logText = "Executando: $description...") }

            val output = withContext(Dispatchers.IO) {
                AppManager.runCommand(command)
            }

            _uiState.update { it.copy(logText = "[$description]: $output") }
        }
    }
}
