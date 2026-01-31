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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var ramUsage by remember { mutableStateOf("Calculando...") }
            val isShizukuReady = try { Shizuku.pingBinder() } catch (e: Exception) { false }

            // Monitor de RAM em tempo real (Tradução da lógica do seu Python)
            LaunchedEffect(Unit) {
                while(true) {
                    try {
                        val memInfo = File("/proc/meminfo").readLines()
                        val total = memInfo.first { it.contains("MemTotal") }.filter { it.isDigit() }.toLong() / 1024
                        val avail = memInfo.first { it.contains("MemAvailable") }.filter { it.isDigit() }.toLong() / 1024
                        ramUsage = "RAM: ${total - avail}MB / ${total}MB"
                    } catch (e: Exception) { ramUsage = "RAM: Erro na leitura" }
                    kotlinx.coroutines.delay(3000)
                }
            }

            Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("TURBO CORE V111", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF00E5FF))
                
                // Card de Status
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(ramUsage, color = Color.Green)
                        Text(if(isShizukuReady) "SISTEMA PRONTO ✅" else "SHIZUKU NECESSÁRIO ❌", 
                             color = if(isShizukuReady) Color.Cyan else Color.Red)
                    }
                }

                Text("MODOS COMPETITIVOS", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                
                // Botão FF SENSI (O seu MODO CAPA do Python)
                Button(
                    onClick = { runShizuku("wm density 90; settings put system pointer_speed 7") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFb91c1c))
                ) { Text("🎯 FF SENSI (DPI 90)") }

                // Botão FF LISO (O seu MODO 540p do Python)
                Button(
                    onClick = { runShizuku("wm size 540x960; wm density 160; am kill-all") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) { Text("🚀 FF LISO (PERFORMANCE)") }

                Text("SISTEMA", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))

                Button(
                    onClick = { runShizuku("wm size reset; wm density reset; settings put global window_animation_scale 1") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) { Text("🔄 RESTAURAR PADRÃO") }
            }
        }
    }

    private fun runShizuku(command: String) {
        try {
            if (Shizuku.pingBinder()) {
                // Shizuku 13+ exige que o comando seja passado como array
                Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }
}
