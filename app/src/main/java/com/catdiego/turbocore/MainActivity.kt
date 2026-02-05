package com.catdiego.turbocore

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    private val REQUEST_CODE = 1001
    private lateinit var viewModel: DashboardViewModel

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkShizukuPermission()
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            viewModel.updateShizukuStatus(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runCatching {
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addRequestPermissionResultListener(permissionListener)
        }

        setContent {
            viewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            // Initial check on UI composition
            LaunchedEffect(Unit) {
                if (Shizuku.pingBinder()) {
                    checkShizukuPermission()
                }
            }

            // Watchdog Logic
            var showWatchdogDialog by remember { mutableStateOf(false) }
            var watchdogTimer by remember { mutableStateOf(10) }

            LaunchedEffect(Unit) {
                if (viewModel.isRecoveryNeeded()) {
                    showWatchdogDialog = true
                    while (watchdogTimer > 0 && showWatchdogDialog) {
                        delay(1000)
                        watchdogTimer--
                    }
                    if (showWatchdogDialog) {
                         // Timeout - reset automatically
                         viewModel.performWatchdogReset()
                         showWatchdogDialog = false
                    }
                }
            }

            if (showWatchdogDialog) {
                WatchdogDialog(
                    secondsRemaining = watchdogTimer,
                    onConfirmStable = {
                        viewModel.confirmConfigurationStability()
                        showWatchdogDialog = false
                    },
                    onReset = {
                        viewModel.performWatchdogReset()
                        showWatchdogDialog = false
                    }
                )
            } else if (!uiState.isShizukuReady) {
                ShizukuPermissionDialog(
                    onConnect = {
                        try {
                            if (Shizuku.pingBinder()) {
                                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                                    Shizuku.requestPermission(REQUEST_CODE)
                                } else {
                                    viewModel.updateShizukuStatus(true)
                                }
                            } else {
                                launchShizukuApp(this)
                            }
                        } catch (e: Exception) {
                            launchShizukuApp(this)
                        }
                    },
                    onDismiss = { /* Blocking Dialog */ }
                )
            }

            InnovationHubDashboard(
                viewModel = viewModel,
                shizukuStatus = if (uiState.isShizukuReady) "Conectado" else "Desconectado",
                onConnectShizuku = {
                     checkShizukuPermission()
                }
            )
        }
    }

    private fun checkShizukuPermission() {
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            viewModel.updateShizukuStatus(true)
        } else {
            // Request permission if binder is alive but permission missing
             if (Shizuku.pingBinder()) {
                 Shizuku.requestPermission(REQUEST_CODE)
             }
        }
    }

    private fun launchShizukuApp(context: Context) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage("rikka.app.shizuku")
                ?: context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")

            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                val githubIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RikkaApps/Shizuku/releases"))
                context.startActivity(githubIntent)
            }
        } catch (e: Exception) {
            // Handled by UI state usually
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeRequestPermissionResultListener(permissionListener)
        }
    }
}

@Composable
fun WatchdogDialog(secondsRemaining: Int, onConfirmStable: () -> Unit, onReset: () -> Unit) {
    Dialog(onDismissRequest = { /* Prevent dismiss, must choose */ }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Yellow, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "VERIFICAÇÃO DE SEGURANÇA",
                    color = Color.Yellow,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Detectamos uma configuração ativa. A tela está normal?",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Reset automático em $secondsRemaining segundos...",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = onReset,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Text("NÃO (RESET)", fontSize = 12.sp)
                    }
                    Button(
                        onClick = onConfirmStable,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    ) {
                        Text("SIM (MANTER)", color = Color.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ShizukuPermissionDialog(onConnect: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Red, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ACESSO NEGADO",
                    color = Color.Red,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "O Turbo Core precisa do serviço Shizuku para funcionar. Por favor, autorize ou inicie o serviço.",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("AUTORIZAR ACESSO", color = Color.White)
                }
            }
        }
    }
}
