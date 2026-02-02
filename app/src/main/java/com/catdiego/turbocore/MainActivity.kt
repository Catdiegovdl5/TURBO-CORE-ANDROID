package com.catdiego.turbocore

import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    private val REQUEST_CODE = 1001

    // Estado global para atualizar a UI quando a permissão for concedida
    private var shizukuStatus by mutableStateOf("Verificando...")

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        shizukuStatus = if (grantResult == PackageManager.PERMISSION_GRANTED) "Conectado" else "Permissão Negada"
    }

    private val binderListener = Shizuku.OnBinderReceivedListener {
        checkAndRequestShizukuPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runCatching { Shizuku.addRequestPermissionResultListener(permissionListener) }
        runCatching { Shizuku.addBinderReceivedListener(binderListener) }
        if (Shizuku.pingBinder()) { checkAndRequestShizukuPermission() }

        setContent {
            TurboCoreUI()
        }
    }

    @Composable
    fun TurboCoreUI() {
        var selectedTab by remember { mutableStateOf(0) }
        val tabs = listOf("UNIVERSAL", SmartCoreEngineV205.brand, "SISTEMA")
        var terminalLog by remember { mutableStateOf("Aguardando comando...") }
        val brandColor = Color(SmartCoreEngineV205.getBrandColor())

        var currentTemp by remember { mutableStateOf(0f) }
        val isShizukuLimited = remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            while(true) {
                currentTemp = ThermalWatchdog.getTemperature(this@MainActivity)
                if (currentTemp > 39) {
                    terminalLog = AppManager.runCommand(this@MainActivity, "cmd package compile --reset -a")
                }
                delay(60000)
            }
        }

        LaunchedEffect(shizukuStatus) {
            if (Shizuku.pingBinder()) {
                isShizukuLimited.value = try {
                    val method = Shizuku::class.java.getDeclaredMethod("isLimited")
                    method.invoke(null) as Boolean
                } catch (e: Exception) {
                    false
                }
            }
        }

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        lifecycleScope.launch {
                            if (currentTemp < 38) {
                                terminalLog = AppManager.runCommand(this@MainActivity, SmartCoreEngineV205.wrapCommand("cmd package compile -m speed-profile -f com.dts.freefireth"))
                            } else {
                                terminalLog = "Erro: Dispositivo muito quente (${currentTemp}°C) para o modo Free Fire."
                            }
                        }
                    },
                    containerColor = Color.Red,
                    contentColor = Color.White
                ) {
                    Text("FF", style = MaterialTheme.typography.labelLarge)
                }
            }
        ) { paddingValues ->
            Column(Modifier.fillMaxSize().background(Color(0xFF0A0A0A)).padding(paddingValues)) {
                TabRow(selectedTabIndex = selectedTab, containerColor = Color.Black, contentColor = Color.Cyan) {
                    tabs.forEachIndexed { index, title ->
                        Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                    }
                }

                LazyColumn(Modifier.padding(16.dp)) {
                    item {
                        Text("Status: $shizukuStatus | Temp: ${currentTemp}°C",
                            color = if(currentTemp > 39) Color.Red else Color.Green,
                            style = MaterialTheme.typography.bodySmall)

                        if (currentTemp > 39) {
                            Text("AVISO: SUPERAQUECIMENTO DETECTADO! RESFRIANDO...", color = Color.Red)
                        }

                        if (isShizukuLimited.value) {
                            Text("ERRO: ATIVE 'DESATIVAR MONITORAMENTO DE PERMISSÕES'", color = Color.Red)
                        }

                        if (shizukuStatus == "Shizuku não instalado!") {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))
                                    this@MainActivity.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                            ) {
                                Text("BAIXAR SHIZUKU", color = Color.White)
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }

                    when (selectedTab) {
                        0 -> { // ABA UNIVERSAL
                            items(SmartCoreEngineV205.getUniversalCommands().size) { i ->
                                val cmd = SmartCoreEngineV205.getUniversalCommands()[i]
                                PerformanceButton(cmd.first, cmd.second) {
                                    lifecycleScope.launch {
                                        terminalLog = AppManager.runCommand(this@MainActivity, SmartCoreEngineV205.wrapCommand(cmd.second))
                                    }
                                }
                            }
                        }
                        1 -> { // ABA ESPECÍFICA (SAMSUNG/XIAOMI)
                            items(SmartCoreEngineV205.getSpecificCommands().size) { i ->
                                val cmd = SmartCoreEngineV205.getSpecificCommands()[i]
                                PerformanceButton(cmd.first, cmd.second, brandColor) {
                                    lifecycleScope.launch {
                                        terminalLog = AppManager.runCommand(this@MainActivity, SmartCoreEngineV205.wrapCommand(cmd.second))
                                    }
                                }
                            }
                        }
                        2 -> { // STATUS E LOGS
                            item {
                                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))) {
                                    Column(Modifier.padding(16.dp)) {
                                        Text("LOG DE SISTEMA", color = Color.Cyan)
                                        Spacer(Modifier.height(8.dp))
                                        Text(terminalLog, color = Color.Green, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun PerformanceButton(title: String, command: String, color: Color = Color.Cyan, onClick: () -> Unit) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.Black)
        ) {
            Text(title)
        }
    }

    private fun checkAndRequestShizukuPermission() {
        if (!AppManager.isShizukuInstalled(this)) {
            shizukuStatus = "Shizuku não instalado!"
            return
        }
        try {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                shizukuStatus = "Conectado"
            } else {
                Shizuku.requestPermission(REQUEST_CODE)
                shizukuStatus = "Autorize no App Shizuku..."
            }
        } catch (e: Exception) {
            launchShizukuApp(this)
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
                // Se o intent falhar por segurança do Android 15, tenta via URI
                val githubIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RikkaApps/Shizuku/releases"))
                context.startActivity(githubIntent)
            }
        } catch (e: Exception) {
            shizukuStatus = "Erro de I/O: Reinstale o Shizuku"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        runCatching { Shizuku.removeRequestPermissionResultListener(permissionListener) }
        runCatching { Shizuku.removeBinderReceivedListener(binderListener) }
    }
}
