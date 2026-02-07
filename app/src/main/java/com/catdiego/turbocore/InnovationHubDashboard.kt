package com.catdiego.turbocore

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import com.catdiego.turbocore.manager.ShizukuManager
import com.catdiego.turbocore.util.LogEntry
import com.catdiego.turbocore.util.LogLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InnovationHubDashboard(viewModel: DashboardViewModel) {
    val currentProfile by viewModel.currentProfile.collectAsState()
    val isGlitchActive = viewModel.isGlitchActive
    val context = LocalContext.current

    // UI State from ViewModel
    val connectionState = viewModel.connectionState
    val logList by viewModel.logFlow.collectAsState()

    var showSafetyDialog by remember { mutableStateOf(false) }
    var showDebugConsole by remember { mutableStateOf(false) }

    if (showSafetyDialog) {
        SafetyCountdownDialog(
            onDismiss = { showSafetyDialog = false },
            onConfirm = {
                showSafetyDialog = false
                viewModel.setProfile(Profile.RankXi)
            }
        )
    }

    if (showDebugConsole) {
        DebugConsoleDialog(
            logs = logList,
            onDismiss = { showDebugConsole = false },
            onForceRebind = { ShizukuManager.autoConnectShizuku(context, mutableStateOf("Rebinding...")) }
        )
    }

    val ledColor = if (isGlitchActive) {
        val infiniteTransition = rememberInfiniteTransition()
        val glitchColor by infiniteTransition.animateColor(
            initialValue = Color(0xFF6200EE),
            targetValue = Color.White,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 300
                    Color(0xFF6200EE) at 0
                    Color.Transparent at 50
                    Color.White at 80
                    Color(0xFF6200EE) at 120
                    Color.Black at 200
                    Color(0xFF6200EE) at 300
                },
                repeatMode = RepeatMode.Restart
            )
        )
        glitchColor
    } else {
        Color(currentProfile.colorHex)
    }

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
            Column {
                Text("INNOVATION HUB", color = Color.White, style = MaterialTheme.typography.titleLarge)

                // Connection Status Indicator
                val statusColor = when(connectionState) {
                    DashboardViewModel.ConnectionState.CONNECTED -> Color.Green
                    DashboardViewModel.ConnectionState.OFFLINE -> Color.Red
                    DashboardViewModel.ConnectionState.CHECKING -> Color.Yellow
                }
                Text(
                    text = "Shizuku: ${connectionState.name}",
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.clickable {
                        ShizukuManager.handleShizukuButtonClick(context)
                    }
                )
            }

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
        if (currentProfile is Profile.RankXi) {
             val infiniteTransition = rememberInfiniteTransition()
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

        // Quick Actions Grid
        Text("AÇÕES RÁPIDAS", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(140.dp)
        ) {
            items(viewModel.quickActions) { action ->
                QuickActionCard(action) { viewModel.executeQuickAction(action) }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Metrics
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            MetricBadge("CPU", "${viewModel.cpuLoad.toInt()}%")
            MetricBadge("RAM", "${viewModel.ramUsage.toInt()}%")
            MetricBadge("TEMP", "${viewModel.temp.toInt()}°C")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Profile Selector
        Text("PERFIL ATIVO: ${currentProfile.name}", color = Color.Gray)
        Spacer(modifier = Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfileChip(Profile.Eco, currentProfile) { viewModel.setProfile(it) }
            ProfileChip(Profile.Balanced, currentProfile) { viewModel.setProfile(it) }
            ProfileChip(Profile.RankS, currentProfile) { viewModel.setProfile(it) } // Mapped 'Turbo' to RankS in UI
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
             Button(
                onClick = { viewModel.setProfile(Profile.SensiFF) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentProfile is Profile.SensiFF) Color(0xFFFF9800) else Color.DarkGray
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("SENSI FF")
            }
            Button(
                onClick = { showSafetyDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)),
                modifier = Modifier.weight(1f),
                border = if (currentProfile is Profile.RankXi) BorderStroke(2.dp, Color.White) else null
            ) {
                Text("RANK Ξ")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // DEBUG CONSOLE BUTTON (Replaces static terminal)
        Button(
            onClick = { showDebugConsole = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A0A0A)),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, Color.DarkGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TERMINAL / DEBUG CONSOLE", color = Color.Green, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.weight(1f))
                Text("Abrir >", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun DebugConsoleDialog(logs: List<LogEntry>, onDismiss: () -> Unit, onForceRebind: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121212),
        title = { Text("Debug Console", color = Color.White) },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.Black)
                        .border(1.dp, Color.Gray)
                        .padding(4.dp)
                ) {
                    LazyColumn(reverseLayout = true) {
                        items(logs.reversed()) { entry ->
                            val color = when(entry.level) {
                                LogLevel.ERROR -> Color.Red
                                LogLevel.SUCCESS -> Color.Green
                                LogLevel.WARN -> Color.Yellow
                                else -> Color.White
                            }
                            Text(
                                text = "${entry.timestamp} [${entry.tag}] ${entry.message}",
                                color = color,
                                fontSize = 10.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onForceRebind, modifier = Modifier.fillMaxWidth()) {
                    Text("FORÇAR RE-BIND SHIZUKU")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("FECHAR") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionCard(action: QuickAction, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(action.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(action.description, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
fun MetricBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .border(2.dp, Color.DarkGray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(value, color = Color.White, style = MaterialTheme.typography.bodySmall)
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
