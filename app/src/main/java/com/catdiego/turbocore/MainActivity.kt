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

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            viewModel.terminalLog = "Ξ: Autorizado"
        } else {
             viewModel.terminalLog = "Shizuku Negado"
        }
    }

    private val binderListener = ShizukuManager.getStickyListener()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
            )
        }

        // Listener Sticky para detecção imediata
        Shizuku.addBinderReceivedListenerSticky(binderListener)
        Shizuku.addRequestPermissionResultListener(permissionListener)

        // Inicia monitoramento
        ShizukuManager.startMonitoring()

        setContent {
            InnovationHubDashboard(viewModel = viewModel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching {
            Shizuku.removeRequestPermissionResultListener(permissionListener)
            Shizuku.removeBinderReceivedListener(binderListener)
        }
    }
}
