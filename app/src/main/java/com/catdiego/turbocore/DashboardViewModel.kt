package com.catdiego.turbocore

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.app.ActivityManager
import android.content.Context
import com.catdiego.turbocore.manager.ShizukuManager
import com.catdiego.turbocore.util.AppLogger
import com.catdiego.turbocore.util.LogLevel

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsManager = PreferencesManager(application)

    private val _currentProfile = MutableStateFlow<Profile>(getProfileByName(prefsManager.getLastProfile()))
    val currentProfile = _currentProfile.asStateFlow()

    var cpuLoad by mutableStateOf(0f)
    var ramUsage by mutableStateOf(0f)
    var temp by mutableStateOf(0f)
    var isGlitchActive by mutableStateOf(_currentProfile.value is Profile.RankXi)

    // Log Flow for Debug Console
    val logFlow = AppLogger.logFlow

    // Connection State for UI Indicator
    enum class ConnectionState {
        CONNECTED, OFFLINE, CHECKING
    }
    var connectionState by mutableStateOf(ConnectionState.CHECKING)

    var quickActions by mutableStateOf(emptyList<QuickAction>())
        private set

    private var prevTotal = 0L
    private var prevActive = 0L

    init {
        initializeSmartCore()
        updateDynamicActions()
        startMonitoring()
        setProfile(_currentProfile.value)

        ShizukuManager.startMonitoring()
        observeShizukuStatus()
    }

    private fun observeShizukuStatus() {
        viewModelScope.launch {
            ShizukuManager.statusFlow.collect { status ->
                connectionState = when {
                    status.startsWith("Conectado") -> ConnectionState.CONNECTED
                    status.startsWith("Offline") -> ConnectionState.OFFLINE
                    else -> ConnectionState.CHECKING
                }
                AppLogger.i("ShizukuStatus", status)
            }
        }
    }

    private fun initializeSmartCore() {
        viewModelScope.launch {
            AppLogger.i("SmartCore", "Iniciando...")
            val manufacturer = ShellEngine.getManufacturer()
            AppLogger.d("SmartCore", "Fabricante: $manufacturer")

            if (manufacturer.contains("samsung", ignoreCase = true)) {
                AppLogger.i("SmartCore", "Aplicando Bypass Knox (Samsung)...")
                ShellEngine.bypassKnox()
            }
            if (manufacturer.contains("xiaomi", ignoreCase = true)) {
                AppLogger.i("SmartCore", "Desativando Joyose (Xiaomi)...")
                ShellEngine.disableJoyose()
            }

            val totalRam = ShellEngine.getTotalRam()
            if (totalRam < 4000000) {
                AppLogger.w("SmartCore", "Low-End ($totalRam KB). Force Swappiness 10.")
                ShellEngine.runCommand("echo 10 > /proc/sys/vm/swappiness")
            }
        }
    }

    // Compatibility wrapper
    fun logDebug(message: String) {
        AppLogger.d("Dashboard", message)
    }

    private fun startMonitoring() {
        viewModelScope.launch {
            while (true) {
                updateMetrics()
                delay(2000)
            }
        }
    }

    private fun updateMetrics() {
        viewModelScope.launch {
            val am = getApplication<Application>().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            val totalRam = memInfo.totalMem.toFloat()
            val availRam = memInfo.availMem.toFloat()
            ramUsage = ((totalRam - availRam) / totalRam) * 100

            val stats = ShellEngine.getCpuRawStats()
            val currentActive = stats.first
            val currentTotal = stats.second

            if (prevTotal != 0L) {
                val deltaTotal = currentTotal - prevTotal
                val deltaActive = currentActive - prevActive
                if (deltaTotal > 0) {
                    cpuLoad = (deltaActive.toFloat() / deltaTotal.toFloat()) * 100
                }
            }
            prevTotal = currentTotal
            prevActive = currentActive

            val realTemp = ShellEngine.getThermalTemp()
            temp = if (realTemp > 0) realTemp else 0f

            if (realTemp > 39 && (_currentProfile.value is Profile.RankXi || _currentProfile.value is Profile.RankOmega)) {
                AppLogger.e("Watchdog", "ALERTA TÉRMICO: ${realTemp}°C. Revertendo para Eco.")
                setProfile(Profile.Eco)
            }

            updateDynamicActions()
        }
    }

    private fun updateDynamicActions() {
        val context = getApplication<Application>()
        val currentApp = AppDetector.getForegroundApp(context) ?: "com.android.systemui"

        quickActions = listOf(
            QuickAction("Limpar RAM", "echo 3 > /proc/sys/vm/drop_caches", "Limpa cache"),
            QuickAction("Boost App", "cmd package compile -m speed $currentApp", "Otimiza $currentApp"),
            QuickAction("DNS Gamer", "settings put global private_dns_mode hostname && settings put global private_dns_specifier 1.1.1.1", "Cloudflare")
        )
    }

    fun setProfile(profile: Profile) {
        viewModelScope.launch {
            _currentProfile.value = profile
            prefsManager.saveLastProfile(profile.name)
            isGlitchActive = (profile is Profile.RankXi)

            val targetPid = if (profile is Profile.RankOmega || profile is Profile.RankXi) {
                val context = getApplication<Application>()
                val pkg = AppDetector.getForegroundApp(context)
                if (pkg != null) ShellEngine.getPid(pkg) else null
            } else null

            val result = ShellEngine.applyProfile(profile, targetPid)
            if (result.startsWith("Erro")) {
                AppLogger.e("Profile", "Falha ao aplicar ${profile.name}: $result")
            } else {
                AppLogger.s("Profile", "Perfil ${profile.name} ativo. $result")
            }
        }
    }

    fun executeQuickAction(action: QuickAction) {
        viewModelScope.launch {
            AppLogger.i("Action", "Executando: ${action.name}")
            val result = ShellEngine.runCommand(action.command)
            if (result.startsWith("Erro")) {
                AppLogger.e("Action", "Falha: $result")
            } else {
                AppLogger.s("Action", "Resultado: $result")
            }
        }
    }

    private fun getProfileByName(name: String): Profile {
        return when (name) {
            Profile.Eco.name -> Profile.Eco
            Profile.Balanced.name -> Profile.Balanced
            Profile.RankS.name -> Profile.RankS
            Profile.RankSSS.name -> Profile.RankSSS
            Profile.RankOmega.name -> Profile.RankOmega
            Profile.RankXi.name -> Profile.RankXi
            Profile.SensiFF.name -> Profile.SensiFF
            "Turbo" -> Profile.RankS
            "Sacrifício (Ξ)" -> Profile.RankXi
            else -> Profile.Balanced
        }
    }
}
