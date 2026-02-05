package com.catdiego.turbocore

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.*
import kotlinx.coroutines.*
import rikka.shizuku.Shizuku
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val REQUEST_CODE = 1001

    private var shizukuStatus by mutableStateOf("Verificando...")

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        shizukuStatus = if (grantResult == PackageManager.PERMISSION_GRANTED) "Conectado" else "Permissão Negada"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()

        // ContentObserver for ADB Status (V250 Protocol)
        val adbObserver = object : android.database.ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                checkAndRequestShizukuPermission()
            }
        }
        contentResolver.registerContentObserver(
            android.provider.Settings.Global.getUriFor("adb_wifi_enabled"),
            false,
            adbObserver
        )

        PerformanceManager.registerBinderListener {
            checkAndRequestShizukuPermission()
        }

        NativeThermalManager(this).registerThermalListener {
            lifecycleScope.launch(Dispatchers.IO) {
                PerformanceManager.triggerCriticalReset()
            }
        }

        scheduleBackgroundMonitoring()

        lifecycleScope.launch(Dispatchers.IO) {
            // Background priority for Shizuku initialization
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)

            runCatching { Shizuku.addRequestPermissionResultListener(permissionListener) }

            if (Shizuku.pingBinder()) {
                // Samsung Knox relaxation delay
                if (SmartCoreEngineV250.brand.contains("SAMSUNG")) {
                    delay(2000)
                }
                checkAndRequestShizukuPermission()
            }
        }

        setContent {
            TurboCoreUI()
        }
    }

    @Composable
    fun TurboCoreTheme(content: @Composable () -> Unit) {
        val brand = SmartCoreEngineV250.brand
        val colorScheme = when {
            brand.contains("SAMSUNG") -> darkColorScheme(
                primary = Color(0xFF0A84FF), // Samsung Blue
                surface = Color(0xFF121212),
                background = Color(0xFF000000), // AMOLED BLACK
                onSurface = Color.White
            )
            brand.contains("XIAOMI") -> darkColorScheme(
                primary = Color(0xFFFF6700), // Xiaomi Orange
                surface = Color(0xFF111111),
                background = Color(0xFF000000), // AMOLED BLACK
                onSurface = Color.White
            )
            else -> darkColorScheme(
                primary = Color(0xFF00E5FF), // Cyan
                surface = Color(0xFF111111),
                background = Color(0xFF000000), // AMOLED BLACK
                onSurface = Color.White
            )
        }

        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as Activity).window
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }

        MaterialTheme(colorScheme = colorScheme, content = content)
    }

    @Composable
    fun TurboCoreUI() {
        val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
        var selectedTab by rememberSaveable { mutableStateOf(0) }
        val tabsV250 = listOf("DESEMPENHO", "ECONOMIA", "COMPETITIVO", "APPS")

        val themeColor = remember(selectedTab) {
            when (selectedTab) {
                0 -> Color(0xFFFF4500) // Fire for Desempenho
                1 -> Color(0xFF00FF00) // Green for Economia
                2 -> Color(0xFF00E5FF) // Cyan for Competitivo
                else -> Color(0xFF9C27B0) // Purple for Apps
            }
        }

        var terminalLog by rememberSaveable { mutableStateOf("Aguardando comando...") }

        var currentTemp by remember { mutableStateOf(0f) }
        var currentRam by remember { mutableStateOf("Calculando...") }
        var currentCpu by remember { mutableStateOf("Carregando...") }
        var activeModeName by rememberSaveable { mutableStateOf("Nenhum") }
        var activeModeId by rememberSaveable { mutableStateOf<Int?>(null) }
        var bloatwareCount by remember { mutableStateOf(0) }
        var isOptimizing by remember { mutableStateOf(false) }

        val isShizukuLimited = remember { mutableStateOf(false) }
        var appsList by remember { mutableStateOf(emptyList<AppInfo>()) }

        LaunchedEffect(lifecycleOwner) {
            launch(Dispatchers.IO) {
                appsList = PerformanceManager.getInstalledApps(this@MainActivity, false)
                bloatwareCount = PerformanceManager.detectBloatware(this@MainActivity).size
            }

            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch(Dispatchers.IO) {
                    while(true) {
                        currentRam = PerformanceManager.getRamUsage(this@MainActivity)
                        currentCpu = PerformanceManager.getCpuStatus()
                        // Adaptive Refresh: Slow down UI polling under thermal or CPU stress
                        val pollingDelay = when {
                            currentTemp >= 40f -> 15000L
                            currentCpu == "CRÍTICO" -> 10000L
                            else -> 5000L
                        }
                        delay(pollingDelay)
                    }
                }
                launch(Dispatchers.IO) {
                    while(true) {
                        currentTemp = HealthManager.getTemperature(this@MainActivity)
                        if (currentTemp >= 40) {
                            terminalLog = PerformanceManager.triggerCriticalReset()
                        } else if (currentTemp >= 39) {
                            terminalLog = PerformanceManager.runRawCommand("cmd package compile --reset -a")
                        }
                        delay(60000)
                    }
                }
            }
        }

        LaunchedEffect(shizukuStatus) {
            if (Shizuku.pingBinder()) {
                isShizukuLimited.value = try {
                    val method = Shizuku::class.java.getDeclaredMethod("isLimited")
                    method.invoke(null) as Boolean
                } catch (e: Exception) { false }

                // Ice Breaker Protocol: Emergency cooling and CPU stress mitigation
                launch(Dispatchers.IO) {
                    val iceBreakerCmd = """
                        cmd package compile --reset -a
                        content stop-sync
                        settings put global activity_manager_constants background_settle_time=0
                        setprop ctl.stop logd
                        am force-stop com.samsung.android.game.gos
                        am force-stop com.samsung.android.bixby.agent
                        am force-stop com.samsung.android.bbc.bbcagent
                        am force-stop com.sec.android.app.samsungapps
                        am force-stop com.google.android.gms
                        cmd package bg-dexopt-job --cancel
                    """.trimIndent()
                    PerformanceManager.runRawCommand(iceBreakerCmd)
                }
            }
        }

        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(currentTemp) {
            if (currentTemp >= 38 && currentTemp < 39) {
                snackbarHostState.showSnackbar("ALERTA TÉRMICO: ${currentTemp}°C - Reduza o uso!")
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(paddingValues)) {
                HealthCheckDashboard(shizukuStatus, currentTemp, bloatwareCount)

                FloatingPanicButton()

                if (isOptimizing) {
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("OTIMIZANDO SISTEMA...", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                if (shizukuStatus != "Conectado") {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("🚨 CONEXÃO RESTRITA (ANDROID 15+)", style = MaterialTheme.typography.titleSmall)
                            Text("1. Vá em Configurações > Apps > Turbo Core", style = MaterialTheme.typography.labelSmall)
                            Text("2. Clique nos 3 pontinhos (canto superior)", style = MaterialTheme.typography.labelSmall)
                            Text("3. Selecione 'Permitir configurações restritas'", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (HealthManager.isCoolingDown()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFb91c1c))
                    ) {
                        Text(
                            "❄️ RESFRIAMENTO ATIVO: Throttling de 3 min.",
                            modifier = Modifier.padding(12.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Black,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabsV250.forEachIndexed { index, title ->
                        Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = {
                            Text(title, style = MaterialTheme.typography.labelSmall)
                        })
                    }
                }

                LazyColumn(Modifier.padding(16.dp)) {
                    item {
                        GamerDashboard(shizukuStatus, currentTemp, currentRam, currentCpu, activeModeName, MaterialTheme.colorScheme.primary)

                        if (isShizukuLimited.value) {
                            Text("⚠️ ERRO: ATIVE 'DESATIVAR MONITORAMENTO DE PERMISSÕES'", color = Color.Red, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 4.dp))
                        }

                        if (shizukuStatus != "Conectado") {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Yellow.copy(alpha = 0.5f))
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Status: $shizukuStatus", modifier = Modifier.weight(1f), color = Color.White, style = MaterialTheme.typography.bodyMedium)
                                    Button(
                                        onClick = { launchShizukuApp(this@MainActivity) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E90FF)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("REPARAR", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))) {
                            Text(terminalLog, color = Color.Green, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(8.dp))
                        }

                        ResetBar {
                            lifecycleScope.launch {
                                terminalLog = PerformanceManager.runRawCommand("wm size reset && wm density reset && settings put global low_power 0 && pm unsuspend com.google.android.gms && cmd power set-fixed-performance-mode-enabled false && settings put global window_animation_scale 1 && settings put global transition_animation_scale 1 && settings put global animator_duration_scale 1 && settings put global touch_latency_mode 0 && settings put system pointer_speed 2 && settings put global wifi_scan_always_enabled 1")
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                    }

                    val modes = when(selectedTab) {
                        0 -> SmartCoreEngineV250.getModesByCategory(ModeCategory.CPU) +
                             SmartCoreEngineV250.getModesByCategory(ModeCategory.GPU) +
                             SmartCoreEngineV250.getModesByCategory(ModeCategory.CHIMERA)
                        1 -> SmartCoreEngineV250.getModesByCategory(ModeCategory.POWER)
                        2 -> SmartCoreEngineV250.getModesByCategory(ModeCategory.MIRA) +
                             SmartCoreEngineV250.getModesByCategory(ModeCategory.REDE)
                        else -> emptyList()
                    }.distinctBy { it.id }

                    if (selectedTab < 3) {
                        items(modes) { mode ->
                            ModeCard(
                                mode = mode,
                                themeColor = themeColor,
                                isActive = activeModeId == mode.id,
                                onToggle = {
                                    lifecycleScope.launch {
                                        if (activeModeId == mode.id) {
                                            // Desativar (V250 SOS Protocol)
                                            terminalLog = PerformanceManager.triggerCriticalReset()
                                            activeModeName = "Nenhum"
                                            activeModeId = null
                                            stopService(Intent(this@MainActivity, ShizukuKeeperService::class.java))
                                        } else {
                                            // Ativar
                                            isOptimizing = true
                                            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                vibrator.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                            } else {
                                                @Suppress("DEPRECATION")
                                                vibrator.vibrate(100)
                                            }

                                            terminalLog = PerformanceManager.runMode(this@MainActivity, mode)
                                            isOptimizing = false
                                            if (!terminalLog.startsWith("Erro") && !terminalLog.startsWith("BLOQUEIO")) {
                                                activeModeName = mode.title
                                                activeModeId = mode.id
                                                android.widget.Toast.makeText(this@MainActivity, "Modo ${mode.title} Ativado", android.widget.Toast.LENGTH_SHORT).show()

                                                if (mode.category == ModeCategory.CPU || mode.category == ModeCategory.GPU || mode.category == ModeCategory.CHIMERA || mode.category == ModeCategory.MIRA) {
                                                    val serviceIntent = Intent(this@MainActivity, ShizukuKeeperService::class.java)
                                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                        startForegroundService(serviceIntent)
                                                    } else {
                                                        startService(serviceIntent)
                                                    }
                                                }
                                            }
                                        }
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
    fun HealthCheckDashboard(status: String, temp: Float, bloat: Int) {
        val ramData = HealthManager.getRamPieData(this)
        val trend = HealthManager.tempTrend
        val ping = NetworkManager.lastLatency

        var showPingShield by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            while(true) {
                val p = NetworkManager.measureLatency()
                if (p > 100) showPingShield = true
                delay(10000)
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("INNOVATION HUB V250", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularGauge("TEMP", temp, 50f, if(temp < 38) Color.Green else Color.Red)
                    Text(if(trend > 0) "▲ Sobe" else "▼ Desce", style = MaterialTheme.typography.labelSmall, color = if(trend > 0) Color.Red else Color.Green)
                }
                RamPieChart(ramData)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val pingColor = if(ping < 50) Color.Green else if(ping < 100) Color.Yellow else Color.Red
                    CircularGauge("PING", ping.toFloat(), 200f, pingColor)
                    if (showPingShield) {
                        Text("🛡️ PING SHIELD", style = MaterialTheme.typography.labelSmall, color = Color.Cyan)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HealthItem("Shizuku", if(status == "Conectado") "ONLINE" else "OFFLINE", if(status == "Conectado") Color.Green else Color.Red)
                HealthItem("Bloat", "$bloat", if(bloat == 0) Color.Green else Color.Yellow)
                HealthItem("Safety", if(HealthManager.isCoolingDown()) "THROTTLING" else "SAFE", if(HealthManager.isCoolingDown()) Color.Red else Color.Green)
            }

            if (showPingShield) {
                Button(onClick = {
                    lifecycleScope.launch {
                        NetworkManager.optimizePing()
                        showPingShield = false
                    }
                }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("ATIVAR ESCUDO DE PING")
                }
            }
        }
    }

    @Composable
    fun HealthItem(label: String, value: String, color: Color) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = color)
        }
    }

    @Composable
    fun CircularGauge(label: String, value: Float, max: Float, color: Color) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
            CircularProgressIndicator(
                progress = value / max,
                modifier = Modifier.fillMaxSize(),
                color = color,
                strokeWidth = 10.dp,
                trackColor = color.copy(alpha = 0.1f)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text("${value.toInt()}", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
        }
    }

    @Composable
    fun RamPieChart(data: List<Float>) {
        val colors = listOf(MaterialTheme.colorScheme.primary, Color.Gray.copy(alpha = 0.3f))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                data.forEachIndexed { index, ratio ->
                    val sweepAngle = ratio * 360f
                    drawArc(
                        color = colors.getOrElse(index) { Color.White },
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true
                    )
                    startAngle += sweepAngle
                }
            }
            Text("RAM", style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
    }

    @Composable
    fun GlassCard(
        modifier: Modifier = Modifier,
        onClick: (() -> Unit)? = null,
        content: @Composable ColumnScope.() -> Unit
    ) {
        Card(
            modifier = if (onClick != null) modifier.clickable { onClick() } else modifier,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                content()
            }
        }
    }

    @Composable
    fun FloatingPanicButton() {
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.BottomEnd) {
            Button(
                onClick = {
                    lifecycleScope.launch {
                        PerformanceManager.triggerCriticalReset()
                        android.widget.Toast.makeText(this@MainActivity, "EMERGENCY RESET!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = CircleShape,
                modifier = Modifier.size(60.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("🆘", style = MaterialTheme.typography.titleLarge)
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
    fun GamerDashboard(status: String, temp: Float, ram: String, cpu: String, activeMode: String, themeColor: Color) {
        val thermalType = if (HealthManager.useNativeThermal) "FS" else "BAT"
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
                        Text(if(temp >= 40) "CRÍTICO ($thermalType)" else "SISTEMA ($thermalType)", style = MaterialTheme.typography.labelSmall, color = if(temp >= 40) Color.Red else Color.Cyan)
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
                        Text("STATUS CPU", style = MaterialTheme.typography.labelSmall, color = Color.Cyan)
                        Text(cpu, style = MaterialTheme.typography.titleSmall, color = if(cpu == "FORÇA MÁXIMA") Color.Red else Color.White)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("MODO ATIVO", style = MaterialTheme.typography.labelSmall, color = Color.Yellow)
                        Text(activeMode, style = MaterialTheme.typography.titleSmall, color = Color.White)
                    }
                }
            }
        }
    }

    @Composable
    fun ModeCard(mode: OptimizationMode, themeColor: Color, isActive: Boolean, onToggle: () -> Unit) {
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
                    Button(
                        onClick = onToggle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive) Color.Red else Color.Cyan,
                            contentColor = if (isActive) Color.White else Color.Black
                        )
                    ) {
                        Text(if (isActive) "RESETAR TELEFONE" else "ATIVAR", style = MaterialTheme.typography.labelMedium)
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
        lifecycleScope.launch(Dispatchers.IO) {
            if (!PerformanceManager.isShizukuInstalled(this@MainActivity)) {
                shizukuStatus = "Shizuku não instalado!"
                return@launch
            }
            try {
                if (Shizuku.pingBinder()) {
                    if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                        shizukuStatus = "Conectado"
                    } else {
                        // Pulo do Gato: Força o popup de autorização
                        withContext(Dispatchers.Main) {
                            Shizuku.requestPermission(REQUEST_CODE)
                        }
                        shizukuStatus = "Autorize no App Shizuku..."
                    }
                } else {
                    shizukuStatus = "Binder Shizuku OFF"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    launchShizukuApp(this@MainActivity)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Shizuku Keeper"
            val descriptionText = "Mantém a conexão Shizuku ativa durante otimizações."
            val importance = android.app.NotificationManager.IMPORTANCE_LOW
            val channel = android.app.NotificationChannel("shizuku_keeper", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: android.app.NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleBackgroundMonitoring() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val request = PeriodicWorkRequestBuilder<ThermalMonitorWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ThermalMonitor",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
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
        PerformanceManager.unregisterBinderListener()
    }
}
