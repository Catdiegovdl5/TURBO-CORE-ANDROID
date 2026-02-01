package com.catdiego.turbocore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.catdiego.turbocore.ui.components.ResetButton
import com.catdiego.turbocore.ui.components.TurboActionButtons

@Composable
fun CompetitiveScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Competitive Mode")

        // Linking the same command motor buttons as requested
        TurboActionButtons()

        Spacer(modifier = Modifier.height(32.dp))

        ResetButton()
    }
}
