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
            // Inicializa como false para evitar travamento no boot
            var isShizukuReady by remember { mutableStateOf(false) }

            // Verifica Shizuku com delay de segurança
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(1000) 
                isShizukuReady = try {
                    Shizuku.pingBinder()
                } catch (e: Exception) {
                    false
                }
            }

            // Monitor de RAM
            LaunchedEffect(Unit) {
                while(true) {
                    try {
                        val memInfo = File("/proc/meminfo").readLines()
                        val total = memInfo.first { it.contains("MemTotal") }.filter { it.isDigit() }.toLong() / 1024
                        val avail = memInfo.first { it.contains("MemAvailable") }.filter { it.isDigit() }.toLong() / 1024
                        ramUsage = "RAM: ${total - avail}MB / ${total}MB"
                    } catch (e: Exception) { ramUsage = "RAM: Erro" }
                    kotlinx.coroutines.delay(3000)
                }
            }

            Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("TURBO CORE V111", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF00E5FF))
                
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(ramUsage, color = Color.Green)
                        Text(if(isShizukuReady) "SISTEMA PRONTO ✅" else "SHIZUKU: AGUARDANDO... ⏳", 
                             color = if(isShizukuReady) Color.Cyan else Color.Yellow)
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
                ) { Text("🚀 FF LISO (PERFORMANCE)") }

                Text("SISTEMA", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))

                Button(
                    onClick = { changeDpiSafely(null); runShizuku("wm size reset") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) { Text("🔄 RESTAURAR PADRÃO") }
            }
        }
    }

    private fun changeDpiSafely(density: Int?) {
        val command = if (density == null) {
            "wm density reset"
        } else {
            if (density < 72 || density > 640) return
            "wm density $density"
        }
        runShizuku(command)
    }

    private fun runShizuku(command: String) {
        try {
            if (!Shizuku.pingBinder()) return
            // Reflexão do Jules para burlar acesso privado
            val shizukuClass = rikka.shizuku.Shizuku::class.java
            val newProcessMethod = shizukuClass.getDeclaredMethod(
                "newProcess", 
                Array<String>::class.java, 
                Array<String>::class.java, 
                String::class.java
            )
            newProcessMethod.isAccessible = true
            newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
