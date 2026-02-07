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

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsManager = PreferencesManager(application)

    private val _currentProfile = MutableStateFlow<Profile>(getProfileByName(prefsManager.getLastProfile()))
    val currentProfile = _currentProfile.asStateFlow()

    var cpuLoad by mutableStateOf(0f)
    var ramUsage by mutableStateOf(0f)
    var temp by mutableStateOf(0f)
    var isGlitchActive by mutableStateOf(_currentProfile.value is Profile.RankXi)

    var terminalLog by mutableStateOf("Inicializando...")
    // Observa o Flow do ShizukuManager em vez de usar MutableState passado
    var shizukuStatus by mutableStateOf("Verificando...")

    var quickActions by mutableStateOf(emptyList<QuickAction>())
        private set

    private var prevTotal = 0L
    private var prevActive = 0L

    init {
        initializeSmartCore()
        updateDynamicActions()
        startMonitoring()
        // Re-apply saved profile on startup
        setProfile(_currentProfile.value)

        // Start monitoring connection
        ShizukuManager.startMonitoring()
        observeShizukuStatus()
    }

    private fun observeShizukuStatus() {
        viewModelScope.launch {
            ShizukuManager.statusFlow.collect { status ->
                shizukuStatus = status
                if (status.startsWith("Erro")) {
                    logDebug(status)
                }
            }
        }
    }

    private fun initializeSmartCore() {
        viewModelScope.launch {
            logDebug("Smart-Core Engine: Iniciando...")
            val manufacturer = ShellEngine.getManufacturer()
            logDebug("Fabricante detectado: $manufacturer")

            if (manufacturer.contains("samsung", ignoreCase = true)) {
                logDebug("Aplicando Bypass Knox (Samsung)...")
                ShellEngine.bypassKnox()
            }
            if (manufacturer.contains("xiaomi", ignoreCase = true)) {
                logDebug("Desativando Joyose (Xiaomi)...")
                ShellEngine.disableJoyose()
            }

            val totalRam = ShellEngine.getTotalRam()
            if (totalRam < 4000000) { // < 4GB
                logDebug("Dispositivo Low-End detectado ($totalRam KB). Aplicando Swappiness 10.")
                ShellEngine.runCommand("echo 10 > /proc/sys/vm/swappiness")
            }
        }
    }

    fun logDebug(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())
        // Mantém apenas as últimas 5 linhas para não poluir a UI
        val lines = terminalLog.split("\n").takeLast(5)
        terminalLog = (lines + "[$timestamp] $message").joinToString("\n")
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
            // 1. RAM (Nativo - Mantém)
            val am = getApplication<Application>().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            val totalRam = memInfo.totalMem.toFloat()
            val availRam = memInfo.availMem.toFloat()
            ramUsage = ((totalRam - availRam) / totalRam) * 100

            // 2. CPU REAL (Cálculo Delta via ShellEngine)
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

            // 3. Temperatura REAL & Watchdog
            val realTemp = ShellEngine.getThermalTemp()
            temp = if (realTemp > 0) realTemp else 0f

            // Thermal Watchdog (Safety Protocol)
            if (realTemp > 39 && (_currentProfile.value is Profile.RankXi || _currentProfile.value is Profile.RankOmega)) {
                logDebug("ALERTA TÉRMICO: ${realTemp}°C. Revertendo para Eco.")
                setProfile(Profile.Eco)
            }

            // Atualiza ações dinâmicas periodicamente para pegar app foreground atual
            updateDynamicActions()
        }
    }

    private fun updateDynamicActions() {
        val context = getApplication<Application>()
        // Pega o app atual ou fallback para systemui se null
        val currentApp = AppDetector.getForegroundApp(context) ?: "com.android.systemui"

        quickActions = listOf(
            QuickAction("Limpar RAM", "echo 3 > /proc/sys/vm/drop_caches", "Limpa cache de página"),
            QuickAction("Boost App Atual", "cmd package compile -m speed $currentApp", "Compila $currentApp"),
            QuickAction("DNS Gamer", "settings put global private_dns_mode hostname && settings put global private_dns_specifier 1.1.1.1", "Cloudflare Low Ping")
        )
    }

    fun setProfile(profile: Profile) {
        viewModelScope.launch {
            _currentProfile.value = profile
            prefsManager.saveLastProfile(profile.name)

            isGlitchActive = (profile is Profile.RankXi)

            // Detect target app for pinning if needed
            val targetPid = if (profile is Profile.RankOmega) {
                val context = getApplication<Application>()
                val pkg = AppDetector.getForegroundApp(context)
                if (pkg != null) ShellEngine.getPid(pkg) else null
            } else null

            val result = ShellEngine.applyProfile(profile, targetPid)
            logDebug("Perfil ${profile.name}: $result")
        }
    }

    fun executeQuickAction(action: QuickAction) {
        viewModelScope.launch {
            logDebug("Executando: ${action.name}...")
            val result = ShellEngine.runCommand(action.command)
            logDebug(">> $result")
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
            // Legacy fallbacks
            "Turbo" -> Profile.RankS
            "Sacrifício (Ξ)" -> Profile.RankXi
            else -> Profile.Balanced
        }
    }
}
