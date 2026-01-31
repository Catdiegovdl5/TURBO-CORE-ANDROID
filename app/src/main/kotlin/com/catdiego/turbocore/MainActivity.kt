package com.catdiego.turbocore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import rikka.shizuku.Shizuku
import java.io.File

class MainActivity : ComponentActivity() {

    private val REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Pede permissão ao Shizuku assim que abre
        if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != 0) {
            Shizuku.requestPermission(REQUEST_CODE)
        }

        setContent {
            var ramUsage by remember { mutableStateOf("Calculando...") }
            var cpuTemp by remember { mutableStateOf("Temp: --°C") }
            val isShizukuReady = try { Shizuku.pingBinder() } catch (e: Exception) { false }

            LaunchedEffect(Unit) {
                while(true) {
                    try {
                        val memInfo = File("/proc/meminfo").readLines()
                        val total = memInfo.first { it.contains("MemTotal") }.filter { it.isDigit() }.toLong() / 1024
                        val avail = memInfo.first { it.contains("MemAvailable") }.filter { it.isDigit() }.toLong() / 1024
                        ramUsage = "RAM: ${total - avail}MB / ${total}MB"

                        val tempFile = File("/sys/class/thermal/thermal_zone0/temp")
                        if (tempFile.exists()) {
                            val t = tempFile.readText().trim().toInt() / 1000
                            cpuTemp = "TEMP: $t°C"
                        }
                    } catch (e: Exception) { ramUsage = "Erro de Leitura" }
                    kotlinx.coroutines.delay(2000)
                }
            }

            Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("TURBO CORE V111", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF00E5FF))
                
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                     colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(ramUsage, color = Color.Green)
                        Text(cpuTemp, color = Color.Yellow)
                        Text(if(isShizukuReady) "MODO ADB: ATIVO ✅" else "MODO ADB: OFFLINE ❌", 
                             color = if(isShizukuReady) Color.Cyan else Color.Red)
                    }
                }

                Text("MODOS COMPETITIVOS", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                
                Button(
                    onClick = { changeDpiSafely(90) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFb91c1c))
                ) { Text("🎯 FF SENSI (DPI 90)") }

                Button(
                    onClick = { runShizuku("wm size 540x960; wm density 160; am kill-all") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) { Text("🚀 FF LISO (540p)") }

                Button(
                    onClick = { changeDpiSafely(null); runShizuku("wm size reset") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) { Text("🔄 RESTAURAR PADRÃO") }
            }
        }
    }

    private fun changeDpiSafely(density: Int?) {
        try {
            if (!Shizuku.pingBinder()) return
            val command = if (density == null) "wm density reset" else "wm density $density"
            if (density != null && (density < 72 || density > 640)) return
            Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun runShizuku(command: String) {
        try {
            if (Shizuku.pingBinder()) {
                Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
}
