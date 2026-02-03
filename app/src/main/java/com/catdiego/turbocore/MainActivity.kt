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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
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
        val categories = remember { ModeCategory.values() }

        val themeColor = remember(selectedTab) {
            when (categories.getOrNull(selectedTab)) {
                ModeCategory.GAMER, ModeCategory.JOGOS -> Color(0xFFFF4500) // Fire OrangeRed
                ModeCategory.ECONOMIA -> Color(0xFF00FFFF) // Ice Cyan
                else -> Color(0xFF1E90FF) // Water DodgerBlue
            }
        }

        val themeGradient = remember(selectedTab) {
            when (categories.getOrNull(selectedTab)) {
                ModeCategory.GAMER, ModeCategory.JOGOS -> androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFb91c1c), Color(0xFFFF8C00)))
                ModeCategory.ECONOMIA -> androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF00CED1), Color(0xFFE0FFFF)))
                else -> androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF00008B), Color(0xFF1E90FF)))
            }
        }
        val tabs = remember { categories.map { it.name } + "APPS" }
        var terminalLog by remember { mutableStateOf("Aguardando comando...") }

        var currentTemp by remember { mutableStateOf(0f) }
        var currentRam by remember { mutableStateOf("Calculando...") }
        var activeModeName by remember { mutableStateOf("Nenhum") }

        val isShizukuLimited = remember { mutableStateOf(false) }
        var appsList by remember { mutableStateOf(emptyList<AppInfo>()) }

        LaunchedEffect(Unit) {
            launch(Dispatchers.IO) {
                appsList = AppManager.getInstalledApps(this@MainActivity, false)
            }
            launch(Dispatchers.IO) {
                while(true) {
                    currentRam = AppManager.getRamUsage(this@MainActivity)
                    delay(5000)
                }
            }
            while(true) {
                currentTemp = ThermalWatchdog.getTemperature(this@MainActivity)
                if (currentTemp >= 40) {
                    terminalLog = AppManager.triggerCriticalReset()
                    android.widget.Toast.makeText(this@MainActivity, "⚠️ EMERGÊNCIA TÉRMICA: 40°C!", android.widget.Toast.LENGTH_LONG).show()
                } else if (currentTemp >= 39) {
                    terminalLog = AppManager.runRawCommand("cmd package compile --reset -a")
                    android.widget.Toast.makeText(this@MainActivity, "Superaquecimento: 39°C. Resetando...", android.widget.Toast.LENGTH_SHORT).show()
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

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        lifecycleScope.launch {
                            val modeFF = SmartCoreEngineV206.getModeById(20) ?: OptimizationMode(999, "Mode Free Fire", "", "cmd package compile -m speed -f com.dts.freefireth", ModeCategory.JOGOS, 3)
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
                    contentColor = themeColor,
                    edgePadding = 16.dp
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = {
                            Text(title, color = if(selectedTab == index) themeColor else Color.Gray)
                        })
                    }
                }

                LazyColumn(Modifier.padding(16.dp)) {
                    item {
                        GamerDashboard(shizukuStatus, currentTemp, currentRam, activeModeName, themeColor)

                        if (isShizukuLimited.value) {
                            Text("⚠️ ERRO: ATIVE 'DESATIVAR MONITORAMENTO DE PERMISSÕES'", color = Color.Red, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 4.dp))
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

                        ResetBar {
                            lifecycleScope.launch {
                                terminalLog = AppManager.runRawCommand("wm size reset && wm density reset && settings put global low_power 0 && pm unsuspend com.google.android.gms && cmd power set-fixed-performance-mode-enabled false && settings put global window_animation_scale 1 && settings put global transition_animation_scale 1 && settings put global animator_duration_scale 1 && settings put global touch_latency_mode 0 && settings put system pointer_speed 2 && settings put global wifi_scan_always_enabled 1")
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                    }

                    if (selectedTab < categories.size) {
                        val currentCategory = categories[selectedTab]
                        val modes = SmartCoreEngineV206.getModesByCategory(currentCategory)

                        items(modes) { mode ->
                            ModeCard(
                                mode = mode,
                                themeColor = themeColor,
                                onActivate = {
                                    lifecycleScope.launch {
                                        terminalLog = AppManager.runMode(this@MainActivity, mode)
                                        if (!terminalLog.startsWith("Erro") && !terminalLog.startsWith("BLOQUEIO")) {
                                            activeModeName = mode.title
                                            android.widget.Toast.makeText(this@MainActivity, "Modo ${mode.title} Ativado", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onDeactivate = {
                                    lifecycleScope.launch {
                                        terminalLog = AppManager.runRawCommand("wm size reset && wm density reset && settings put global low_power 0 && pm unsuspend com.google.android.gms && cmd power set-fixed-performance-mode-enabled false && settings put global window_animation_scale 1 && settings put global transition_animation_scale 1 && settings put global animator_duration_scale 1")
                                        activeModeName = "Nenhum"
                                    }
                                }
                            )
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
    fun ResetBar(onReset: () -> Unit) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0A0A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("RESTAURAR PADRÃO", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    Text("Limpa wm, density e energia", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                }
                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFb91c1c)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("LIMPAR RAM E CPU", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    @Composable
    fun GamerDashboard(status: String, temp: Float, ram: String, activeMode: String, themeColor: Color) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, themeColor.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("SISTEMA", style = MaterialTheme.typography.labelSmall, color = Color.Cyan)
                        Text(status, style = MaterialTheme.typography.titleSmall, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val tempColor = when {
                            temp >= 40 -> Color.Red
                            temp >= 39 -> Color.Yellow
                            else -> Color.Green
                        }
                        Text(if(temp >= 40) "CRÍTICO" else "BATERIA", style = MaterialTheme.typography.labelSmall, color = if(temp >= 40) Color.Red else Color.Cyan)
                        Text("${temp}°C", style = MaterialTheme.typography.titleSmall, color = tempColor)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("MEMÓRIA RAM", style = MaterialTheme.typography.labelSmall, color = Color.Cyan)
                        Text(ram, style = MaterialTheme.typography.titleSmall, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("MODO ATIVO", style = MaterialTheme.typography.labelSmall, color = Color.Yellow)
                        Text(activeMode, style = MaterialTheme.typography.titleSmall, color = Color.White)
                    }
                }
            }
        }
    }

    @Composable
    fun ModeCard(mode: OptimizationMode, themeColor: Color, onActivate: () -> Unit, onDeactivate: () -> Unit) {
        val riskColor = when(mode.riskLevel) {
            1 -> Color.Green
            2 -> Color.Yellow
            else -> Color.Red
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
            border = androidx.compose.foundation.BorderStroke(1.dp, themeColor.copy(alpha = 0.3f))
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(mode.title, style = MaterialTheme.typography.titleMedium, color = Color.White, modifier = Modifier.weight(1f))
                    Surface(shape = CircleShape, color = riskColor.copy(alpha = 0.2f)) {
                        Text("RISK ${mode.riskLevel}", color = riskColor, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
                Text(mode.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(
                        onClick = onDeactivate,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("RESETAR TELEFONE", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = onActivate,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Cyan, contentColor = Color.Black)
                    ) {
                        Text("ATIVAR", style = MaterialTheme.typography.labelMedium)
                    }
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
