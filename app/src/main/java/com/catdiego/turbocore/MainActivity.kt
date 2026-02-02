package com.catdiego.turbocore

import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    private val REQUEST_CODE = 1001

    // Estado global para atualizar a UI quando a permissão for concedida
    private var shizukuStatus by mutableStateOf("Verificando...")

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        shizukuStatus = if (grantResult == PackageManager.PERMISSION_GRANTED) "Conectado" else "Permissão Negada"
    }

    private val binderListener = Shizuku.OnBinderReceivedListener {
        checkAndRequestShizukuPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runCatching { Shizuku.addRequestPermissionResultListener(permissionListener) }
        runCatching { Shizuku.addBinderReceivedListener(binderListener) }
        if (Shizuku.pingBinder()) { checkAndRequestShizukuPermission() }

        setContent {
            var terminalLog by remember { mutableStateOf("Aguardando comando...") }
            val brandColor = Color(SmartCoreEngineV200.getBrandColor())
            val appTitle = SmartCoreEngineV200.getBrandTitle()
            val isLowEnd = SmartCoreEngineV200.isLowEnd()

            var currentTemp by remember { mutableStateOf(0f) }
            val isShizukuLimited = remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                while(true) {
                    currentTemp = ThermalWatchdog.getTemperature(this@MainActivity)
                    delay(60000) // 60 segundos conforme protocolo V200
                }
            }

            LaunchedEffect(shizukuStatus) {
                if (Shizuku.pingBinder()) {
                    isShizukuLimited.value = try {
                        val method = Shizuku::class.java.getDeclaredMethod("isLimited")
                        method.invoke(null) as Boolean
                    } catch (e: Exception) {
                        false
                    }
                }
            }

            LaunchedEffect(Unit) {
                delay(1000)
                if (Shizuku.pingBinder()) {
                    // Protocolo V153: Sempre verifique antes de pedir
                    checkAndRequestShizukuPermission()
                } else {
                    shizukuStatus = "Shizuku desligado no sistema!"
                }
            }

            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    item {
                        Text(appTitle, color = brandColor, style = MaterialTheme.typography.headlineMedium)
                        Text("Protocolo: V200 [Smart-Core Active]", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                        Text("Status: $shizukuStatus | Temp: ${currentTemp}°C", color = if(currentTemp > 39) Color.Red else Color.Green)

                        if (currentTemp > 39) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "AVISO: DISPOSITIVO SUPERAQUECIDO! RESFRIANDO...",
                                color = Color.Red,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.background(Color.White.copy(alpha = 0.1f)).padding(4.dp)
                            )
                        }

                        if (isShizukuLimited.value) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "ERRO: ATIVE 'DESATIVAR MONITORAMENTO DE PERMISSÕES' NO PC",
                                color = Color.Red,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.background(Color.White.copy(alpha = 0.1f)).padding(4.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // PROTOCOLO V155: CARD DE INTELIGÊNCIA DE HARDWARE
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("DISPOSITIVO DETECTADO:", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                Text(AppManager.getDeviceDisplayName(), color = Color.White, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(4.dp))
                                Text("SUGESTÃO:", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                Text(AppManager.getOptimizationLevelSuggestion(LocalContext.current), color = brandColor, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        if (!isLowEnd) {
                            Button(
                                onClick = {
                                    lifecycleScope.launch {
                                        val commands = AppManager.getAutoOptimizationCommands(this@MainActivity)
                                        val combinedCommand = commands.joinToString(" && ")
                                        terminalLog = AppManager.runCommand(this@MainActivity, SmartCoreEngineV200.wrapCommand(combinedCommand))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isShizukuLimited.value && currentTemp <= 39,
                                colors = ButtonDefaults.buttonColors(containerColor = brandColor, contentColor = Color.Black)
                            ) {
                                Text("APLICAR OTIMIZAÇÃO INTELIGENTE")
                            }
                        } else {
                            Button(
                                onClick = {
                                    lifecycleScope.launch {
                                        terminalLog = AppManager.runCommand(this@MainActivity, SmartCoreEngineV200.wrapCommand("pm trim-caches 256M && settings put global low_power 1"))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isShizukuLimited.value && currentTemp <= 39,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow, contentColor = Color.Black)
                            ) {
                                Text("OTIMIZAR LOW-END (SAFE MODE)")
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                try {
                                    if (Shizuku.pingBinder()) {
                                        Shizuku.requestPermission(REQUEST_CODE)
                                    } else {
                                        // Se o binder não responde, abre o app Shizuku direto
                                        launchShizukuApp(this@MainActivity)
                                    }
                                } catch (e: Exception) {
                                    launchShizukuApp(this@MainActivity)
                                }
                            }, Modifier.weight(1f)) {
                                Text("ATIVAR CONEXÃO")
                            }
                            Button(onClick = { updateStatus() }, Modifier.weight(1f)) {
                                Text("ATUALIZAR")
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // CATEGORIA: DESEMPENHO (Performance)
                        Text("DESEMPENHO", color = Color.Gray, style = MaterialTheme.typography.titleSmall)
                        Button(onClick = {
                            lifecycleScope.launch {
                                // V140 Requisito: wm size 540x1200 && wm density 210
                                terminalLog = AppManager.runCommand(this@MainActivity, SmartCoreEngineV200.wrapCommand("wm size 540x1200 && wm density 210"))
                            }
                        }, modifier = Modifier.fillMaxWidth(), enabled = !isShizukuLimited.value && currentTemp <= 39) { Text("Modo Bruto") }

                        Spacer(Modifier.height(8.dp))

                        Button(onClick = {
                            lifecycleScope.launch {
                                // V140 Requisito: wm size reset && wm density reset
                                terminalLog = AppManager.runCommand(this@MainActivity, SmartCoreEngineV200.wrapCommand("wm size reset && wm density reset"))
                            }
                        }, modifier = Modifier.fillMaxWidth(), enabled = !isShizukuLimited.value && currentTemp <= 39) { Text("Usual Turbo") }

                        Spacer(Modifier.height(16.dp))

                        // CATEGORIA: ECONOMIA (Battery Saver)
                        Text("ECONOMIA", color = Color.Gray, style = MaterialTheme.typography.titleSmall)
                        Button(onClick = {
                            lifecycleScope.launch {
                                terminalLog = AppManager.runCommand(this@MainActivity, SmartCoreEngineV200.wrapCommand("settings put global low_power 1 && pm suspend com.google.android.gms"))
                            }
                        }, modifier = Modifier.fillMaxWidth(), enabled = !isShizukuLimited.value && currentTemp <= 39, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("Super Economia") }

                        Spacer(Modifier.height(8.dp))

                        Button(onClick = {
                            lifecycleScope.launch {
                                terminalLog = AppManager.runCommand(this@MainActivity, SmartCoreEngineV200.wrapCommand("wm size 360x800 && settings put global low_power 1"))
                            }
                        }, modifier = Modifier.fillMaxWidth(), enabled = !isShizukuLimited.value && currentTemp <= 39, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("Ultra Economia") }

                        Spacer(Modifier.height(16.dp))

                        // CATEGORIA: COMPETITIVO (Gaming)
                        Text("COMPETITIVO", color = Color.Gray, style = MaterialTheme.typography.titleSmall)
                        Button(onClick = {
                            lifecycleScope.launch {
                                terminalLog = AppManager.runCommand(this@MainActivity, SmartCoreEngineV200.wrapCommand("cmd power set-fixed-performance-mode-enabled true"))
                            }
                        }, modifier = Modifier.fillMaxWidth(), enabled = !isShizukuLimited.value && currentTemp <= 39, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) { Text("Gamer Ultimate") }

                        Spacer(Modifier.height(8.dp))

                        Button(onClick = {
                            lifecycleScope.launch {
                                terminalLog = AppManager.runCommand(this@MainActivity, SmartCoreEngineV200.wrapCommand("wm density 180"))
                            }
                        }, modifier = Modifier.fillMaxWidth(), enabled = !isShizukuLimited.value && currentTemp <= 39, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) { Text("Sensi Free Fire") }

                        Spacer(Modifier.height(24.dp))

                        Button(onClick = {
                            lifecycleScope.launch {
                                terminalLog = AppManager.runCommand(this@MainActivity, SmartCoreEngineV200.wrapCommand("wm size reset && wm density reset && settings put global low_power 0 && cmd power set-fixed-performance-mode-enabled false && pm unsuspend com.google.android.gms"))
                            }
                        }, modifier = Modifier.fillMaxWidth(), enabled = !isShizukuLimited.value, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("RESETAR TUDO") }

                        Spacer(Modifier.height(20.dp))

                        Text("SAÍDA DO SISTEMA:", color = Color.Cyan)
                        Text(terminalLog, color = Color.Green, modifier = Modifier.background(Color.Black).padding(8.dp).fillMaxWidth())
                    }
                }
            }
        }
    }

    private fun checkAndRequestShizukuPermission() {
        try {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                shizukuStatus = "Conectado"
            } else {
                // Tenta o popup padrão
                Shizuku.requestPermission(REQUEST_CODE)
                // Se em 2 segundos não conectar, sugere abertura manual
                shizukuStatus = "Autorize no App Shizuku..."
            }
        } catch (e: Exception) {
            launchShizukuApp(this)
        }
    }

    private fun updateStatus() {
        shizukuStatus = if (!Shizuku.pingBinder()) {
            "Shizuku Parado (Abra o App Shizuku)"
        } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            "Conectado"
        } else if (shizukuStatus == "Permissão Negada") {
            "Configurações Restritas (Habilite Manualmente)"
        } else {
            "Aguardando Permissão"
        }
    }

    private fun openShizukuDownload(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RikkaApps/Shizuku/releases"))
        context.startActivity(intent)
    }

    private fun launchShizukuApp(context: Context) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage("rikka.app.shizuku")
                ?: context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                // Se o intent falhar por segurança do Android 15, tenta via URI
                val githubIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RikkaApps/Shizuku/releases"))
                context.startActivity(githubIntent)
            }
        } catch (e: Exception) {
            shizukuStatus = "Erro de I/O: Reinstale o Shizuku"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { Shizuku.removeRequestPermissionResultListener(permissionListener) }
        runCatching { Shizuku.removeBinderReceivedListener(binderListener) }
    }
}
