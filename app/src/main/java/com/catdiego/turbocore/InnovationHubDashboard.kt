package com.catdiego.turbocore

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InnovationHubDashboard(
    viewModel: DashboardViewModel = viewModel(),
    shizukuStatus: String,
    onConnectShizuku: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Lifecycle Observer for Adaptive Polling
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.startMonitoring()
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.stopMonitoring()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Snackbar effect
    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearUserMessage()
        }
    }

    // Watchdog Dialog (Modularized)
    if (uiState.showSafetyDialog) {
        SafetyCountdownDialog(
            onConfirm = { viewModel.confirmSafety() },
            onDismiss = { viewModel.performWatchdogReset() }
        )
    }

    Scaffold(
        containerColor = Color(0xFF000000),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.runOptimization("wm size reset && wm density reset", "Reset SOS")
                },
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Warning, contentDescription = "SOS Reset")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // HEADER
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "TURBO CORE V300",
                            color = Color.Cyan,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        // Status LED
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = if (uiState.isShizukuReady) Color.Green else Color.Red,
                                    shape = RoundedCornerShape(50)
                                )
                        )
                    }
                    Text(
                        text = "INNOVATION HUB",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Shizuku Status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF111111), RoundedCornerShape(8.dp))
                            .border(1.dp, if (uiState.isShizukuReady) Color.Green else Color.Red, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if(uiState.isShizukuReady) "Motor Ativo" else "Motor Inativo",
                            color = if (uiState.isShizukuReady) Color.Green else Color.Red,
                            fontSize = 14.sp
                        )
                        if (!uiState.isShizukuReady) {
                            Button(
                                onClick = onConnectShizuku,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("CONECTAR", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // COCKPIT (Gauges: Temp & Ping)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("COCKPIT", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // TEMP GAUGE
                            GaugeItem(
                                value = uiState.temperature,
                                max = 60f,
                                label = "SOC TEMP",
                                unit = "°C",
                                color = if (uiState.temperature > 40) Color.Red else Color.Cyan
                            )

                            // PING GAUGE
                            val pingVal = if(uiState.ping == -1L) 999f else uiState.ping.toFloat()
                            GaugeItem(
                                value = pingVal,
                                max = 300f,
                                label = "PING",
                                unit = "ms",
                                color = if (pingVal < 100) Color.Green else Color.Yellow
                            )
                        }
                    }
                }
            }

            // RAM DETAIL CARD
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("MEMÓRIA RAM", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = uiState.ramPercent / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = if (uiState.ramPercent > 85) Color.Red else Color.Cyan,
                            trackColor = Color.DarkGray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = uiState.ramUsage,
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                            Text(
                                text = String.format("%.0f%%", uiState.ramPercent),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Delay: ${uiState.pollingRate}ms",
                            color = Color.DarkGray,
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }

            // ACTIONS - DESEMPENHO
            item {
                SectionHeader("DESEMPENHO")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(
                        text = "MODO BRUTO",
                        color = Color(0xFF6200EE),
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.runOptimization("wm size 540x1200 && wm density 210", "Modo Bruto (720p)")
                    }
                    ActionButton(
                        text = "USUAL TURBO",
                        color = Color(0xFF3700B3),
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.runOptimization("wm size reset && wm density reset", "Usual Turbo")
                    }
                }
            }

            // ACTIONS - ECONOMIA
            item {
                SectionHeader("ECONOMIA")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(
                        text = "SUPER ECO",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.runOptimization("settings put global low_power 1 && pm suspend com.google.android.gms", "Super Economia")
                    }
                    ActionButton(
                        text = "ULTRA ECO",
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.runOptimization("wm size 360x800 && settings put global low_power 1", "Ultra Economia")
                    }
                }
            }

            // ACTIONS - COMPETITIVO
            item {
                SectionHeader("COMPETITIVO")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(
                        text = "GAMER ULT",
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.runOptimization("cmd power set-fixed-performance-mode-enabled true", "Gamer Ultimate")
                    }
                    ActionButton(
                        text = "SENSI FF",
                        color = Color(0xFF1976D2),
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.runOptimization("wm density 180", "Sensi Free Fire")
                    }
                }
            }

            // RESET
            item {
                ActionButton(
                    text = "RESETAR TUDO (PADRÃO)",
                    color = Color.Red,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    viewModel.runOptimization("wm size reset && wm density reset && settings put global low_power 0", "Reset Geral")
                }
            }

            // TERMINAL
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("TERMINAL:", color = Color.Cyan, fontSize = 12.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color(0xFF0A0A0A), RoundedCornerShape(8.dp))
                        .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = uiState.logText,
                        color = Color.Green,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color.Gray,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
    )
}

@Composable
fun ActionButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
fun GaugeItem(
    value: Float,
    max: Float,
    label: String,
    unit: String,
    color: Color,
    size: Dp = 80.dp
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
            Canvas(modifier = Modifier.size(size)) {
                // Background Arc
                drawArc(
                    color = Color.DarkGray.copy(alpha = 0.3f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
                // Progress Arc
                val sweep = (value / max) * 270f
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = sweep.coerceIn(0f, 270f),
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = String.format("%.0f", value),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = unit,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
    }
}
