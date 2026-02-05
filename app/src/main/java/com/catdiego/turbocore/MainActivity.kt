package com.catdiego.turbocore

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels()
    private val REQUEST_CODE = 1001

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            viewModel.terminalLog = "Shizuku Conectado"
        } else {
             viewModel.terminalLog = "Shizuku Negado"
        }
    }

    private val binderListener = Shizuku.OnBinderReceivedListener {
        checkAndRequestShizukuPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Shizuku Setup
        runCatching { Shizuku.addRequestPermissionResultListener(permissionListener) }
        runCatching { Shizuku.addBinderReceivedListener(binderListener) }

        if (Shizuku.pingBinder()) {
             checkAndRequestShizukuPermission()
        }

        setContent {
            InnovationHubDashboard(viewModel = viewModel)
        }
    }

    private fun checkAndRequestShizukuPermission() {
        try {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                 viewModel.terminalLog = "Shizuku Pronto"
            } else {
                Shizuku.requestPermission(REQUEST_CODE)
            }
        } catch (e: Exception) {
             viewModel.terminalLog = "Erro Shizuku: ${e.message}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { Shizuku.removeRequestPermissionResultListener(permissionListener) }
        runCatching { Shizuku.removeBinderReceivedListener(binderListener) }
    }
}
