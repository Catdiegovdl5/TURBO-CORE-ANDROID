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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
            val scope = rememberCoroutineScope()
            var terminalLog by remember { mutableStateOf("Aguardando comando...") }

            LaunchedEffect(Unit) {
                delay(1000)
                if (Shizuku.pingBinder()) {
                    // Se o serviço está vivo, exige a tela de permissão na hora!
                    Shizuku.requestPermission(REQUEST_CODE)
                } else {
                    shizukuStatus = "Shizuku desligado no sistema!"
                }
            }

            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    item {
                        Text("TURBO CORE v1.0", color = Color.Cyan, style = MaterialTheme.typography.headlineMedium)
                        Text("Status: $shizukuStatus", color = if(shizukuStatus == "Conectado") Color.Green else Color.Red)

                        Spacer(Modifier.height(8.dp))

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

                        // DESEMPENHO
                        Text("DESEMPENHO", color = Color.Gray, style = MaterialTheme.typography.titleSmall)
                        Button(onClick = {
                            scope.launch { terminalLog = AppManager.runCommand("wm size 540x1200 && wm density 210") }
                        }, modifier = Modifier.fillMaxWidth()) { Text("MODO BRUTO (720p)") }

                        Spacer(Modifier.height(8.dp))

                        Button(onClick = {
                            scope.launch { terminalLog = AppManager.runCommand("wm size reset && wm density reset") }
                        }, modifier = Modifier.fillMaxWidth()) { Text("USUAL TURBO") }

                        Spacer(Modifier.height(16.dp))

                        // ECONOMIA
                        Text("ECONOMIA", color = Color.Gray, style = MaterialTheme.typography.titleSmall)
                        Button(onClick = {
                            scope.launch { terminalLog = AppManager.runCommand("settings put global low_power 1 && pm suspend com.google.android.gms") }
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("SUPER ECONOMIA") }

                        Spacer(Modifier.height(8.dp))

                        Button(onClick = {
                            scope.launch { terminalLog = AppManager.runCommand("wm size 360x800 && settings put global low_power 1") }
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("ULTRA ECONOMIA") }

                        Spacer(Modifier.height(16.dp))

                        // COMPETITIVO
                        Text("COMPETITIVO", color = Color.Gray, style = MaterialTheme.typography.titleSmall)
                        Button(onClick = {
                            scope.launch { terminalLog = AppManager.runCommand("cmd power set-fixed-performance-mode-enabled true") }
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) { Text("GAMER ULTIMATE") }

                        Spacer(Modifier.height(8.dp))

                        Button(onClick = {
                            scope.launch { terminalLog = AppManager.runCommand("wm size 540x960 && wm density 180") }
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) { Text("SENSI FREE FIRE") }

                        Spacer(Modifier.height(24.dp))

                        Button(onClick = {
                            scope.launch { terminalLog = AppManager.runCommand("wm size reset && wm density reset && settings put global low_power 0 && pm unsuspend com.google.android.gms && cmd power set-fixed-performance-mode-enabled false") }
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("RESETAR TUDO") }

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
