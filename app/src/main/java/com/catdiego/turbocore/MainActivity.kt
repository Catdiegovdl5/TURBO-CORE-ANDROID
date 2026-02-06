package com.catdiego.turbocore

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import rikka.shizuku.Shizuku
import com.catdiego.turbocore.manager.ShizukuManager

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels()
    private val REQUEST_CODE = 1001

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            viewModel.terminalLog = "Shizuku Conectado"
            viewModel.shizukuStatus.value = "Conectado"
        } else {
             viewModel.terminalLog = "Shizuku Negado"
             viewModel.shizukuStatus.value = "Negado"
        }
    }

    // Usando o novo manager para reconexão automática
    private var binderListener: Shizuku.OnBinderReceivedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa o listener usando o ShizukuManager
        binderListener = ShizukuManager.setupAutoReconnect(this, viewModel.shizukuStatus)

        // Shizuku Setup
        runCatching { Shizuku.addRequestPermissionResultListener(permissionListener) }
        runCatching {
            binderListener?.let { Shizuku.addBinderReceivedListener(it) }
        }

        // Tentativa inicial de conexão
        ShizukuManager.autoConnectShizuku(this, viewModel.shizukuStatus)

        setContent {
            InnovationHubDashboard(viewModel = viewModel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { Shizuku.removeRequestPermissionResultListener(permissionListener) }
        runCatching {
            binderListener?.let { Shizuku.removeBinderReceivedListener(it) }
        }
    }
}
