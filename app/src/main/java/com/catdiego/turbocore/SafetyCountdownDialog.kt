package com.catdiego.turbocore

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

@Composable
fun SafetyCountdownDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var secondsRemaining by remember { mutableStateOf(10) }

    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000)
            secondsRemaining--
        }
        // Timeout reached -> Trigger Dismiss (Reset)
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = { /* Prevent dismiss, must choose */ },
        containerColor = Color(0xFF111111),
        title = { Text("Verificação de Segurança", color = Color.Yellow) },
        text = {
            Text(
                "Uma alteração de tela foi detectada. Se você pode ler isso, clique em MANTER.\n\nReset em $secondsRemaining segundos.",
                color = Color.White
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
            ) {
                Text("MANTER", color = Color.Black)
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("RESETAR")
            }
        }
    )
}
