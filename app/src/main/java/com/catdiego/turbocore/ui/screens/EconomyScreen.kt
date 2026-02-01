package com.catdiego.turbocore.ui.screens

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
import kotlinx.coroutines.launch

@Composable
fun EconomyScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Economy Mode")

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    // Super Economia
                    ShellEngine.runCommandWithFeedback(context, "settings put global low_power 1")
                    ShellEngine.runCommandWithFeedback(context, "svc bluetooth disable")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Super Economia")
        }
    }
}
