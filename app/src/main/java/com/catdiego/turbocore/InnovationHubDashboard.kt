package com.catdiego.turbocore

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun InnovationHubDashboard(viewModel: DashboardViewModel) {
    val currentProfile by viewModel.currentProfile.collectAsState()
    val isGlitchActive = viewModel.isGlitchActive
    var showSafetyDialog by remember { mutableStateOf(false) }

    if (showSafetyDialog) {
        SafetyCountdownDialog(
            onDismiss = { showSafetyDialog = false },
            onConfirm = {
                showSafetyDialog = false
                viewModel.setProfile(Profile.Sacrifice)
            }
        )
    }

    // Glitch Animation
    val infiniteTransition = rememberInfiniteTransition()
    val glitchColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF6200EE),
        targetValue = Color.White,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 300 // Irregular total duration
                Color(0xFF6200EE) at 0
                Color.Transparent at 50 // Blink off
                Color.White at 80 // Flash white
                Color(0xFF6200EE) at 120 // Back to purple
                Color.Black at 200 // Flicker dark
                Color(0xFF6200EE) at 300
            },
            repeatMode = RepeatMode.Restart
        )
    )

    val ledColor = if (isGlitchActive) glitchColor else Color(currentProfile.colorHex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("INNOVATION HUB", color = Color.White, style = MaterialTheme.typography.titleLarge)

            // Status LED
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(ledColor, CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Rank Xi Badge
        if (currentProfile is Profile.Sacrifice) {
             val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFF6200EE).copy(alpha = pulseAlpha), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("RANK Ξ: HARDWARE TRANSCENDENCE", color = Color(0xFFBB86FC), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Metrics (Simplified)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            MetricBadge("CPU", "${viewModel.cpuLoad.toInt()}%")
            MetricBadge("RAM", "${viewModel.ramUsage.toInt()}%")
            MetricBadge("TEMP", "${viewModel.temp.toInt()}°C")
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Profile Selector
        Text("PERFIL ATIVO: ${currentProfile.name}", color = Color.Gray)
        Spacer(modifier = Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileChip(Profile.Eco, currentProfile) { viewModel.setProfile(it) }
            ProfileChip(Profile.Balanced, currentProfile) { viewModel.setProfile(it) }
            ProfileChip(Profile.Turbo, currentProfile) { viewModel.setProfile(it) }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { showSafetyDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)),
            modifier = Modifier.fillMaxWidth(),
            border = if (currentProfile is Profile.Sacrifice) BorderStroke(2.dp, Color.White) else null
        ) {
            Text("RANK Ξ (SACRIFÍCIO)")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Terminal
        Text("TERMINAL:", color = Color.Green, style = MaterialTheme.typography.labelSmall)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0A0A0A), RoundedCornerShape(4.dp))
                .border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            Text(viewModel.terminalLog, color = Color.Green, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun MetricBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .border(2.dp, Color.DarkGray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(value, color = Color.White)
        }
        Text(label, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun RowScope.ProfileChip(profile: Profile, current: Profile, onClick: (Profile) -> Unit) {
    val selected = profile == current
    Button(
        onClick = { onClick(profile) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(profile.colorHex) else Color.DarkGray
        ),
        modifier = Modifier.weight(1f)
    ) {
        Text(profile.name.take(4))
    }
}
