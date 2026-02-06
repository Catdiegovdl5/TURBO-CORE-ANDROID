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

    // Referência estável para evitar criação de múltiplos objetos de escuta
    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            viewModel.terminalLog = "Ξ: Autorizado"
            viewModel.shizukuStatus.value = "Conectado"
        } else {
             viewModel.terminalLog = "Shizuku Negado"
             viewModel.shizukuStatus.value = "Negado"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Adiciona apenas se o binder estiver vivo
        if (Shizuku.pingBinder()) {
            Shizuku.addRequestPermissionResultListener(permissionListener)
        }

        // Tentativa inicial de conexão via Manager
        ShizukuManager.autoConnectShizuku(this, viewModel.shizukuStatus)

        setContent {
            InnovationHubDashboard(viewModel = viewModel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // PONTO CRÍTICO: Limpeza obrigatória para evitar vazamento do Handler interno do Shizuku
        runCatching {
            Shizuku.removeRequestPermissionResultListener(permissionListener)
        }
    }
}
