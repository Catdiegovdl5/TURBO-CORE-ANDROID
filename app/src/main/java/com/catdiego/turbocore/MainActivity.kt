package com.catdiego.turbocore

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    private val REQUEST_CODE = 1001
    private lateinit var viewModel: DashboardViewModel

    // Reactive state for permission checks
    private var permissionCheckTrigger by mutableStateOf(0)

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkShizukuPermission()
    }

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            viewModel.updateShizukuStatus(true)
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission result handling if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runCatching {
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addRequestPermissionResultListener(permissionListener)
        }

        askNotificationPermission()

        setContent {
            viewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            // Reactive Usage Permission Check
            var hasUsageStats by remember { mutableStateOf(AppDetector.hasPermission(this)) }

            // Re-check when trigger updates (onResume)
            LaunchedEffect(permissionCheckTrigger) {
                hasUsageStats = AppDetector.hasPermission(this@MainActivity)
                if (Shizuku.pingBinder()) {
                    checkShizukuPermission()
                }
            }

            // Usage Stats Permission Dialog
            if (!hasUsageStats) {
                UsageStatsPermissionDialog {
                    startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
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
            } else {
                InnovationHubDashboard(
                    viewModel = viewModel,
                    shizukuStatus = if (uiState.isShizukuReady) "Conectado" else "Desconectado",
                    onConnectShizuku = {
                         checkShizukuPermission()
                    }
                )
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Trigger recomposition to check permissions again
        permissionCheckTrigger++
    }

    private fun checkShizukuPermission() {
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            if (::viewModel.isInitialized) {
                viewModel.updateShizukuStatus(true)
            }
        } else {
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
fun UsageStatsPermissionDialog(onSettings: () -> Unit) {
    Dialog(onDismissRequest = { /* Blocking */ }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Cyan, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ACESSO NECESSÁRIO",
                    color = Color.Cyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Para ativar o Modo Turbo automaticamente nos seus jogos, o Turbo Core precisa acessar as estatísticas de uso.",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("AUTORIZAR", color = Color.Black)
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
