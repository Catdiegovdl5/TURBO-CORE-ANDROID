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
import kotlin.random.Random
import android.app.ActivityManager
import android.content.Context

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentProfile = MutableStateFlow<Profile>(Profile.Balanced)
    val currentProfile = _currentProfile.asStateFlow()

    var cpuLoad by mutableStateOf(0f)
    var ramUsage by mutableStateOf(0f)
    var temp by mutableStateOf(0f)
    var isGlitchActive by mutableStateOf(false)

    var terminalLog by mutableStateOf("Aguardando comando...")

    init {
        startMonitoring()
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
        // RAM
        val am = getApplication<Application>().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val total = memInfo.totalMem.toFloat()
        val avail = memInfo.availMem.toFloat()
        ramUsage = ((total - avail) / total) * 100

        // CPU & Temp (Simulated/Placeholder as file reading requires IO and specific paths)
        // In a real scenario, we would parse /proc/stat and thermal zones.
        cpuLoad = Random.nextFloat() * 100
        temp = 35f + Random.nextFloat() * 10
    }

    fun setProfile(profile: Profile) {
        viewModelScope.launch {
            _currentProfile.value = profile

            isGlitchActive = (profile is Profile.Sacrifice)

            val result = ShellEngine.applyProfile(profile)
            terminalLog = result
        }
    }
}
