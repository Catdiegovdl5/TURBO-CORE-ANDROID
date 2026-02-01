package com.catdiego.turbocore.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.catdiego.turbocore.util.ShellEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PerformanceScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun performReset() {
        ShellEngine.runShizukuCommand("wm size reset")
        ShellEngine.runShizukuCommand("wm density reset")
        ShellEngine.runShizukuCommand("settings put global low_power 0")
        // Re-enable GOS (assuming it was suspended or disabled)
        ShellEngine.runShizukuCommand("pm unsuspend com.samsung.android.game.gos")
        ShellEngine.runShizukuCommand("pm enable com.samsung.android.game.gos")
    }

    fun resetAll() {
        scope.launch(Dispatchers.IO) {
            performReset()
            launch(Dispatchers.Main) {
                Toast.makeText(context, "All Settings Reset", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                    // Fail-Safe Reset
                    ShellEngine.runShizukuCommand("wm size reset")
                    ShellEngine.runShizukuCommand("wm density reset")

                    // Sensi FF
                    // Updated to minimum safety value 210
                    ShellEngine.runShizukuCommand("wm density 210")
                    ShellEngine.runShizukuCommand("settings put system pointer_speed 7")
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
                    // Fail-Safe Reset
                    ShellEngine.runShizukuCommand("wm size reset")
                    ShellEngine.runShizukuCommand("wm density reset")

                    // Modo Bruto
                    // Updated to 540x1200 as requested
                    ShellEngine.runShizukuCommand("wm size 540x1200")
                    ShellEngine.runShizukuCommand("cmd thermalservice override_status 0")
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
                    ShellEngine.runShizukuCommand("pm suspend com.samsung.android.game.gos")
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Gamer Ultimate Applied", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Gamer Ultimate")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { resetAll() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("RESET ALL")
        }
    }
}
