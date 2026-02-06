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
    var isGlitchActive by mutableStateOf(_currentProfile.value is Profile.Sacrifice)

    var terminalLog by mutableStateOf("Aguardando comando...")
    var shizukuStatus = mutableStateOf("Verificando...")

    var quickActions by mutableStateOf(emptyList<QuickAction>())
        private set

    private var prevTotal = 0L
    private var prevActive = 0L

    init {
        updateDynamicActions()
        startMonitoring()
        // Re-apply saved profile on startup
        setProfile(_currentProfile.value)

        // Initial Shizuku Check
        ShizukuManager.autoConnectShizuku(application, shizukuStatus)
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

            // 3. Temperatura REAL
            val realTemp = ShellEngine.getThermalTemp()
            temp = if (realTemp > 0) realTemp else 0f

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

            isGlitchActive = (profile is Profile.Sacrifice)

            val result = ShellEngine.applyProfile(profile)
            terminalLog = result
        }
    }

    fun executeQuickAction(action: QuickAction) {
        viewModelScope.launch {
            terminalLog = "Executando: ${action.name}...\n"
            val result = ShellEngine.runCommand(action.command)
            terminalLog += ">> $result"
        }
    }

    private fun getProfileByName(name: String): Profile {
        return when (name) {
            Profile.Eco.name -> Profile.Eco
            Profile.Balanced.name -> Profile.Balanced
            Profile.Turbo.name -> Profile.Turbo
            Profile.SensiFF.name -> Profile.SensiFF
            Profile.Sacrifice.name -> Profile.Sacrifice
            else -> Profile.Balanced
        }
    }
}
