package com.catdiego.turbocore

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
import kotlinx.coroutines.delay
import rikka.shizuku.Shizuku
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // UI State
            var shizukuState by remember { mutableStateOf("Aguardando Sistema...") }
            var isShizukuReady by remember { mutableStateOf(false) }
            var ramUsage by remember { mutableStateOf("Calculando...") }

            // BLINDAGEM CONTRA CRASH EM ANDROID 16 (MediaTek)
            // O driver mtkpower@impl pode causar deadlock se o binder for chamado muito cedo na main thread.
            // Solução: Delay estratégico de 1500ms fora da thread principal.
            LaunchedEffect(Unit) {
                delay(1500) // Delay Crítico para estabilização do Binder
                safeRun {
                    try {
                        if (Shizuku.pingBinder()) {
                            // Verifica permissão (Shizuku.checkSelfPermission() é mais seguro que checkSelfPermission(Context))
                            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                                isShizukuReady = true
                                shizukuState = "Conectado e Seguro"
                            } else {
                                if (Shizuku.shouldShowRequestPermissionRationale()) {
                                    shizukuState = "Permissão Negada"
                                } else {
                                    Shizuku.requestPermission(0)
                                    shizukuState = "Solicitando Acesso..."
                                }
                            }
                        } else {
                            shizukuState = "Shizuku não rodando"
                        }
                    } catch (e: Exception) {
                        Log.e("TurboCore", "Falha crítica ao conectar no Shizuku", e)
                        shizukuState = "Erro Crítico: ${e.message}"
                    }
                }
            }

            // Listener de Permissão para atualizar UI instantaneamente
            DisposableEffect(Unit) {
                val listener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        isShizukuReady = true
                        shizukuState = "Conectado e Seguro"
                    } else {
                        shizukuState = "Permissão Negada pelo Usuário"
                    }
                }
                Shizuku.addRequestPermissionResultListener(listener)
                onDispose { Shizuku.removeRequestPermissionResultListener(listener) }
            }

             // Monitor de RAM
            LaunchedEffect(Unit) {
                while(true) {
                    safeRun {
                        try {
                            val memInfo = File("/proc/meminfo").readLines()
                            val total = memInfo.first { it.contains("MemTotal") }.filter { it.isDigit() }.toLong() / 1024
                            val avail = memInfo.first { it.contains("MemAvailable") }.filter { it.isDigit() }.toLong() / 1024
                            ramUsage = "RAM: ${total - avail}MB / ${total}MB"
                        } catch (e: Exception) {
                            ramUsage = "RAM: Erro Leit."
                        }
                    }
                    delay(3000)
                }
            }

            // UI Layout
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF00E5FF),
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                        Text("TURBO CORE V112", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF00E5FF))

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(ramUsage, color = Color.Green)
                                Text("STATUS: $shizukuState",
                                     color = if(isShizukuReady) Color.Cyan else Color.Red)
                            }
                        }

                        if (isShizukuReady) {
                             Text("MODOS COMPETITIVOS", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                
                            // Botão FF SENSI
                            Button(
                                onClick = {
                                    changeDpiSafely(90)
                                    runShizukuCommand("settings put system pointer_speed 7")
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFb91c1c))
                            ) { Text("🎯 FF SENSI (DPI 90)") }

                            // Botão FF LISO
                            Button(
                                onClick = {
                                    runShizukuCommand("wm size 540x960")
                                    changeDpiSafely(160)
                                    runShizukuCommand("am kill-all")
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) { Text("🚀 FF LISO (PERFORMANCE)") }

                            Text("SISTEMA", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))

                            Button(
                                onClick = {
                                    runShizukuCommand("wm size reset")
                                    runShizukuCommand("wm density reset")
                                    runShizukuCommand("settings put global window_animation_scale 1")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                            ) { Text("🔄 RESTAURAR PADRÃO") }
                        } else {
                            Text("Aguardando Shizuku... Certifique-se que ele está rodando.", color = Color.Gray, modifier = Modifier.padding(top=16.dp))
                        }
                    }
                }
            }
        }
    }

    /**
     * Wrapper de segurança para evitar crashes não tratados.
     */
    private inline fun safeRun(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Log.e("TurboCore", "Erro capturado em safeRun", e)
        }
    }

    /**
     * Altera a DPI do dispositivo de forma segura, respeitando limites físicos.
     * @param density Valor da DPI (72-640) ou null para resetar ao padrão de fábrica.
     */
    private fun changeDpiSafely(density: Int?) {
        val command = if (density == null) {
            "wm density reset"
        } else {
            // Validação de Segurança: Impede valores que podem brickar a UI
            if (density < 72 || density > 640) return
            "wm density $density"
        }
        runShizukuCommand(command)
    }

    /**
     * Executa comandos shell via Shizuku usando Reflexão para compatibilidade máxima.
     */
    private fun runShizukuCommand(command: String) {
        safeRun {
            // Tenta invocar Shizuku.newProcess via reflexão.
            val shizukuClass = rikka.shizuku.Shizuku::class.java
            val newProcessMethod = shizukuClass.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null)
        }
    }
}
