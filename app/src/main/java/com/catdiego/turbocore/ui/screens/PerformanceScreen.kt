package com.catdiego.turbocore.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.catdiego.turbocore.util.ShellEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PerformanceScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Performance Presets")

        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    // Sensi FF
                    ShellEngine.runCommand("wm density 90")
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Sensi FF Applied", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sensi FF")
        }

        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    // Modo Bruto
                    ShellEngine.runCommand("wm size 360x800")
                    ShellEngine.runCommand("cmd thermalservice override_status 0")
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Modo Bruto Applied", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Modo Bruto")
        }

        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    // Gamer Ultimate
                    ShellEngine.runCommand("pm disable-user --user 0 com.samsung.android.game.gos")
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Gamer Ultimate Applied", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Gamer Ultimate")
        }
    }
}
