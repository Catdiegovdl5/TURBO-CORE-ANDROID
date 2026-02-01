package com.catdiego.turbocore

import android.os.Bundle
import android.content.pm.PackageManager
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    private val REQUEST_CODE_SHIZUKU = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var shizukuStatus by remember { mutableStateOf("Verificando...") }
            var terminalLog by remember { mutableStateOf("Terminal Pronto.") }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                delay(2000)
                try {
                    if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                        shizukuStatus = "Conectado"
                    } else {
                        Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
                    }
                } catch (e: Exception) {
                    shizukuStatus = "Serviço Shizuku não iniciado"
                }
            }

            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    item {
                        Text("TURBO CORE v1.0", color = Color.Cyan, style = MaterialTheme.typography.headlineMedium)
                        Text("Shizuku: $shizukuStatus", color = if(shizukuStatus == "Conectado") Color.Green else Color.Red)

                        Button(onClick = { Shizuku.requestPermission(REQUEST_CODE_SHIZUKU) }) {
                            Text("FORÇAR PERMISSÃO")
                        }

                        Spacer(Modifier.height(20.dp))

                        // DESEMPENHO
                        Text("DESEMPENHO", color = Color.White, style = MaterialTheme.typography.titleSmall)
                        Button(onClick = {
                            terminalLog = AppManager.runCommand("wm size 540x1200 && wm density 210")
                        }, modifier = Modifier.fillMaxWidth()) { Text("ATIVAR MODO BRUTO") }

                        Spacer(Modifier.height(8.dp))

                        Button(onClick = {
                            terminalLog = AppManager.runCommand("wm size reset && wm density reset")
                        }, modifier = Modifier.fillMaxWidth()) { Text("USUAL TURBO") }

                        Spacer(Modifier.height(16.dp))

                        // ECONOMIA
                        Text("ECONOMIA", color = Color.White, style = MaterialTheme.typography.titleSmall)
                        Button(onClick = {
                            terminalLog = AppManager.runCommand("settings put global low_power 1 && pm suspend com.google.android.gms")
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("SUPER ECONOMIA") }

                        Spacer(Modifier.height(8.dp))

                        Button(onClick = {
                            terminalLog = AppManager.runCommand("wm size 360x800 && settings put global low_power 1")
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("ULTRA ECONOMIA") }

                        Spacer(Modifier.height(16.dp))

                        // COMPETITIVO
                        Text("COMPETITIVO", color = Color.White, style = MaterialTheme.typography.titleSmall)
                        Button(onClick = {
                            terminalLog = AppManager.runCommand("cmd power set-fixed-performance-mode-enabled true")
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) { Text("GAMER ULTIMATE") }

                        Spacer(Modifier.height(8.dp))

                        Button(onClick = {
                            terminalLog = AppManager.runCommand("wm density 180")
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) { Text("SENSI FREE FIRE") }

                        Spacer(Modifier.height(24.dp))

                        Button(onClick = {
                            terminalLog = AppManager.runCommand("wm size reset && wm density reset && settings put global low_power 0")
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("RESETAR TUDO") }

                        Spacer(Modifier.height(20.dp))

                        Text("LOG: $terminalLog", color = Color.Green, modifier = Modifier.background(Color.Black).padding(8.dp).fillMaxWidth())
                    }
                }
            }
        }
    }
}
