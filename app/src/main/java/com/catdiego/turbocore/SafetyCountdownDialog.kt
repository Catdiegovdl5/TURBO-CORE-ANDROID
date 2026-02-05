package com.catdiego.turbocore

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SafetyCountdownDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var timeLeft by remember { mutableStateOf(5) }
    var isChecked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121212),
        title = {
            Text("⚠️ AVISO CRÍTICO", color = Color.Red, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "Hardware operará sem limitadores térmicos.",
                    color = Color(0xFFFF0000),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "Esta ação remove proteções de segurança do kernel. O superaquecimento pode ocorrer.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { isChecked = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color.Red, uncheckedColor = Color.Gray)
                    )
                    Text(
                        "Risco de desligamento súbito e desgaste de bateria.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = timeLeft == 0 && isChecked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    disabledContainerColor = Color.DarkGray
                )
            ) {
                Text(if (timeLeft > 0) "Aguarde ${timeLeft}s" else "ATIVAR Ξ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = Color.Gray)
            }
        }
    )
}
