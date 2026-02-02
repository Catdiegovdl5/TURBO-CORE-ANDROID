package com.catdiego.turbocore

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {
    private val REQUEST_CODE = 1001

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
        val categories = ModeCategory.values()
        val tabs = categories.map { it.name } + "APPS"
        var terminalLog by remember { mutableStateOf("Aguardando comando...") }

        var currentTemp by remember { mutableStateOf(0f) }
        val isShizukuLimited = remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            while(true) {
                currentTemp = ThermalWatchdog.getTemperature(this@MainActivity)
                if (currentTemp > 39) {
                    terminalLog = AppManager.runRawCommand("cmd package compile --reset -a")
                }
                delay(60000)
            }
        }

        LaunchedEffect(shizukuStatus) {
            if (Shizuku.pingBinder()) {
                isShizukuLimited.value = try {
                    val method = Shizuku::class.java.getDeclaredMethod("isLimited")
                    method.invoke(null) as Boolean
                } catch (e: Exception) { false }
            }
        }

        val appsList = remember { AppManager.getInstalledApps(this@MainActivity, false) }

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        lifecycleScope.launch {
                            val modeFF = OptimizationMode(999, "Mode Free Fire", "", "cmd package compile -m speed-profile -f com.dts.freefireth", ModeCategory.CHIMERA, 3)
                            terminalLog = AppManager.runMode(this@MainActivity, modeFF)
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
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Black,
                    contentColor = Color.Cyan,
                    edgePadding = 16.dp
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
                    }
                }

                LazyColumn(Modifier.padding(16.dp)) {
                    item {
                        Text("Status: $shizukuStatus | Temp: ${currentTemp}°C",
                            color = if(currentTemp > 38) Color.Red else Color.Green,
                            style = MaterialTheme.typography.bodySmall)

                        if (isShizukuLimited.value) {
                            Text("ERRO: ATIVE 'DESATIVAR MONITORAMENTO DE PERMISSÕES'", color = Color.Red, style = MaterialTheme.typography.labelSmall)
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

                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))) {
                            Text(terminalLog, color = Color.Green, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(8.dp))
                        }

                        Spacer(Modifier.height(8.dp))
                    }

                    if (selectedTab < categories.size) {
                        val currentCategory = categories[selectedTab]
                        val modes = SmartCoreEngineV206.getModesByCategory(currentCategory)

                        items(modes) { mode ->
                            ModeCard(mode) {
                                lifecycleScope.launch {
                                    terminalLog = AppManager.runMode(this@MainActivity, mode)
                                }
                            }
                        }
                    } else {
                        items(appsList) { app ->
                            AppCard(app)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ModeCard(mode: OptimizationMode, onClick: () -> Unit) {
        val riskColor = when(mode.riskLevel) {
            1 -> Color.Green
            2 -> Color.Yellow
            else -> Color.Red
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151515))
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(mode.title, style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
                    Surface(shape = CircleShape, color = riskColor.copy(alpha = 0.2f)) {
                        Text("RISK ${mode.riskLevel}", color = riskColor, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
                Text(mode.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onClick,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan, contentColor = Color.Black)
                ) {
                    Text("ATIVAR", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    @Composable
    fun AppCard(app: AppInfo) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151515))
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(app.name, style = MaterialTheme.typography.titleSmall, color = Color.White)
                    Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                if (app.isSystem) {
                    Text("SYSTEM", color = Color.Yellow, style = MaterialTheme.typography.labelSmall)
                }
            }
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
