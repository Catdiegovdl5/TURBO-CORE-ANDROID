package com.catdiego.turbocore.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.catdiego.turbocore.util.ShellEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun TurboActionButtons() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Button(
        onClick = {
            scope.launch(Dispatchers.IO) {
                // Fail-Safe Reset
                ShellEngine.runShizukuCommand("wm size reset")
                ShellEngine.runShizukuCommand("wm density reset")

                // Sensi FF - Safe Mode V128: Density 210
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

                // Modo Bruto - Safe Mode V128: 540x1200
                ShellEngine.runShizukuCommand("wm size 540x1200")

                // Samsung Kernel Tweaks
                ShellEngine.runShizukuCommand("settings put global sem_enhanced_cpu_responsiveness 1")
                ShellEngine.runShizukuCommand("settings put global multicore_packet_scheduler 1")
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
                // Gamer Ultimate - Safe Mode V128: Suspend GOS
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
}

@Composable
fun ResetButton() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Button(
        onClick = {
            scope.launch(Dispatchers.IO) {
                ShellEngine.runShizukuCommand("wm size reset")
                ShellEngine.runShizukuCommand("wm density reset")
                ShellEngine.runShizukuCommand("settings put global low_power 0")
                ShellEngine.runShizukuCommand("settings put global sem_enhanced_cpu_responsiveness 0")
                ShellEngine.runShizukuCommand("settings put global multicore_packet_scheduler 0")
                ShellEngine.runShizukuCommand("pm unsuspend com.samsung.android.game.gos")
                ShellEngine.runShizukuCommand("pm enable com.samsung.android.game.gos")

                launch(Dispatchers.Main) {
                    Toast.makeText(context, "All Settings Reset", Toast.LENGTH_SHORT).show()
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
    ) {
        Text("RESET ALL")
    }
}
