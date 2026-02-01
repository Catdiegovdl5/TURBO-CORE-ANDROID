package com.catdiego.turbocore.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.catdiego.turbocore.ui.components.ResetButton
import com.catdiego.turbocore.ui.components.TurboActionButtons

@Composable
fun PerformanceScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Performance Presets")

        TurboActionButtons()

        Spacer(modifier = Modifier.height(32.dp))

        ResetButton()
    }
}
