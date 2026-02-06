package com.catdiego.turbocore

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.StrictMode
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

    // Agora inicializado com o logger
    private var binderListener: Shizuku.OnBinderReceivedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuração de StrictMode para DEBUG (Mitigação de Risco)
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }

        // Inicializa o listener usando o ShizukuManager e o Logger do ViewModel
        binderListener = ShizukuManager.setupAutoReconnect(this, viewModel.shizukuStatus, viewModel::logDebug)

        // IMPORTANTE: addBinderReceivedListenerSticky dispara imediatamente se o serviço já estiver rodando
        binderListener?.let { Shizuku.addBinderReceivedListenerSticky(it) }

        // Listener de permissão
        Shizuku.addRequestPermissionResultListener(permissionListener)

        // Tentativa inicial direta (caso sticky demore ou falhe)
        ShizukuManager.autoConnectShizuku(this, viewModel.shizukuStatus, viewModel::logDebug)

        setContent {
            InnovationHubDashboard(viewModel = viewModel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching {
            Shizuku.removeRequestPermissionResultListener(permissionListener)
            binderListener?.let { Shizuku.removeBinderReceivedListener(it) }
        }
    }
}
