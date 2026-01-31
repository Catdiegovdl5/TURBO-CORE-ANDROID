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
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val isShizukuReady = Shizuku.pingBinder()
            var statusMessage by remember { mutableStateOf(if (isShizukuReady) "SHIZUKU ATIVO ✅" else "SHIZUKU NÃO DETECTADO ❌") }

            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("TURBO CORE + SHIZUKU", style = MaterialTheme.typography.headlineMedium)
                Text(statusMessage, color = if (isShizukuReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (Shizuku.pingBinder()) {
                            executarComandoShizuku("wm density 90")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ATIVAR MODO CAPA (90 DPI)")
                }

                Button(
                    onClick = {
                        if (Shizuku.pingBinder()) {
                            executarComandoShizuku("wm density reset")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("RESETAR DPI")
                }
            }
        }
    }

    private fun executarComandoShizuku(comando: String) {
        try {
            Shizuku.newProcess(arrayOf("sh", "-c", comando), null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
