package com.catdiego.turbocore.ui.components

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
import kotlinx.coroutines.launch

@Composable
fun TurboActionButtons() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Button(
        onClick = {
            scope.launch {
                // Fail-Safe Reset
                ShellEngine.runCommandWithFeedback(context, "wm size reset")
                ShellEngine.runCommandWithFeedback(context, "wm density reset")

                // Sensi FF
                ShellEngine.runCommandWithFeedback(context, "wm density 210")
                ShellEngine.runCommandWithFeedback(context, "settings put system pointer_speed 7")
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Sensi FF")
    }

    Button(
        onClick = {
            scope.launch {
                // Fail-Safe Reset
                ShellEngine.runCommandWithFeedback(context, "wm size reset")
                ShellEngine.runCommandWithFeedback(context, "wm density reset")

                // Modo Bruto
                ShellEngine.runCommandWithFeedback(context, "wm size 540x1200")
                ShellEngine.runCommandWithFeedback(context, "settings put global sem_enhanced_cpu_responsiveness 1")
                ShellEngine.runCommandWithFeedback(context, "settings put global multicore_packet_scheduler 1")
                ShellEngine.runCommandWithFeedback(context, "cmd thermalservice override_status 0")
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Modo Bruto")
    }

    Button(
        onClick = {
            scope.launch {
                // Gamer Ultimate
                ShellEngine.runCommandWithFeedback(context, "pm suspend com.samsung.android.game.gos")
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
            scope.launch {
                ShellEngine.runCommandWithFeedback(context, "wm size reset")
                ShellEngine.runCommandWithFeedback(context, "wm density reset")
                ShellEngine.runCommandWithFeedback(context, "settings put global low_power 0")
                ShellEngine.runCommandWithFeedback(context, "settings put global sem_enhanced_cpu_responsiveness 0")
                ShellEngine.runCommandWithFeedback(context, "settings put global multicore_packet_scheduler 0")
                ShellEngine.runCommandWithFeedback(context, "pm unsuspend com.samsung.android.game.gos")
                ShellEngine.runCommandWithFeedback(context, "pm enable com.samsung.android.game.gos")
            }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
    ) {
        Text("RESET ALL")
    }
}
