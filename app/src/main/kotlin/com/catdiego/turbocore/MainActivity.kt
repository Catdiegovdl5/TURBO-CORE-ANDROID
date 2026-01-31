package com.catdiego.turbocore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            var status by remember { mutableStateOf("Verificando Shizuku...") }
            
            LaunchedEffect(Unit) {
                status = try {
                    if (Shizuku.pingBinder()) "SHIZUKU ATIVO ✅" else "SHIZUKU PARADO ❌"
                } catch (e: Exception) {
                    "ERRO AO CONECTAR ⚠️"
                }
            }

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("TURBO CORE + SHIZUKU", style = MaterialTheme.typography.headlineMedium)
                Text(status)

                Spacer(modifier = Modifier.height(20.dp))

                Button(onClick = { runCmd("wm density 90") }, modifier = Modifier.fillMaxWidth()) {
                    Text("ATIVAR MODO CAPA (90 DPI)")
                }

                Button(onClick = { runCmd("wm density reset") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("RESETAR DPI")
                }
            }
        }
    }

    private fun runCmd(command: String) {
        try {
            // Forma alternativa e segura para Shizuku 13+
            Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
