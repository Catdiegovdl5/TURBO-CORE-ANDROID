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

    // State to trigger recomposition when permission result comes in
    private var permissionStateTrigger by mutableStateOf(0)

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
        permissionStateTrigger++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching { Shizuku.addRequestPermissionResultListener(permissionListener) }

        setContent {
            val viewModel: DashboardViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            // Re-evaluate check whenever trigger increments OR binder state changes
            val currentPermissionTrigger = permissionStateTrigger
            var showPermissionDialog by remember { mutableStateOf(false) }

            // Logic to determine if we should show the blocking dialog
            LaunchedEffect(uiState.isShizukuActive, currentPermissionTrigger) {
                // If binder is not active (not received), we wait or show dialog
                if (!uiState.isShizukuActive) {
                    // Double check manually in case ViewModel missed it initially
                    if (!Shizuku.pingBinder()) {
                        showPermissionDialog = true
                        return@LaunchedEffect
                    }
                }

                // Binder is active, check permission
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(REQUEST_CODE)
                    showPermissionDialog = true
                } else {
                    showPermissionDialog = false
                }
            }

            if (showPermissionDialog) {
                ShizukuPermissionDialog(
                    onConnect = {
                        try {
                            if (Shizuku.pingBinder()) {
                                Shizuku.requestPermission(REQUEST_CODE)
                            } else {
                                launchShizukuApp(this)
                            }
                        } catch (e: Exception) {
                            launchShizukuApp(this)
                        }
                    },
                    onDismiss = { /* Non-dismissable until connected/authorized */ }
                )
            }

            InnovationHubDashboard(
                viewModel = viewModel,
                shizukuStatus = if (!showPermissionDialog && uiState.isShizukuActive) "Conectado" else "Desconectado",
                onConnectShizuku = {
                     // Manual trigger
                     permissionStateTrigger++
                }
            )
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
        runCatching { Shizuku.removeRequestPermissionResultListener(permissionListener) }
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
