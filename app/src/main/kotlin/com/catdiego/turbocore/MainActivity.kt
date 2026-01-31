package com.catdiego.turbocore

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    enum class Screen(val title: String) {
        DASHBOARD("🖥️ DASHBOARD"),
        COMPETITIVO("🎮 COMPETITIVO"),
        APPS("📦 APPS"),
        TERMINAL("💻 TERMINAL")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            
            var shizukuState by remember { mutableStateOf("Buscando...") }
            var isShizukuReady by remember { mutableStateOf(false) }

            // Stats
            var ramUsage by remember { mutableStateOf("...") }
            var batteryLevel by remember { mutableStateOf("...") }
            var temp by remember { mutableStateOf("...") }
            var storage by remember { mutableStateOf("...") }

            LaunchedEffect(Unit) {
                delay(1500) // Delay MediaTek fix
                safeRun {
                    try {
                        if (Shizuku.pingBinder()) {
                            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                                isShizukuReady = true
                                shizukuState = "CONECTADO"
                            } else {
                                if (Shizuku.shouldShowRequestPermissionRationale()) {
                                    shizukuState = "NEGADO"
                                } else {
                                    Shizuku.requestPermission(0)
                                    shizukuState = "SOLICITANDO..."
                                }
                            }
                        } else {
                            shizukuState = "NÃO RODANDO"
                        }
                    } catch (e: Exception) {
                        shizukuState = "ERRO: ${e.message}"
                    }
                }
            }

            DisposableEffect(Unit) {
                val listener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        isShizukuReady = true
                        shizukuState = "CONECTADO"
                    } else {
                        shizukuState = "NEGADO"
                    }
                }
                Shizuku.addRequestPermissionResultListener(listener)
                onDispose { Shizuku.removeRequestPermissionResultListener(listener) }
            }

            // Monitoring Loop
            LaunchedEffect(Unit) {
                while(true) {
                    safeRun {
                        try {
                            // RAM
                            val memInfo = File("/proc/meminfo").readLines()
                            val total = memInfo.first { it.contains("MemTotal") }.filter { it.isDigit() }.toLong() / 1024
                            val avail = memInfo.first { it.contains("MemAvailable") }.filter { it.isDigit() }.toLong() / 1024
                            val used = total - avail
                            ramUsage = "RAM: ${(used/1024.0).format(1)}/${(total/1024.0).format(1)} GB"

                            // Battery (Simple estimation without BroadcastReceiver for simplicity in this loop)
                            // Ideally use BatteryManager, but file read is fast enough for "cyberpunk" feel
                            // /sys/class/power_supply/battery/capacity usually exists
                            val capFile = File("/sys/class/power_supply/battery/capacity")
                            if (capFile.exists()) batteryLevel = "BAT: ${capFile.readText().trim()}%"

                            val tempFile = File("/sys/class/power_supply/battery/temp")
                            if (tempFile.exists()) temp = "TEMP: ${(tempFile.readText().trim().toInt() / 10.0)}°C"

                        } catch (e: Exception) {
                             // Ignore
                        }
                    }
                    delay(3000)
                }
            }

            val cyberpunkColors = darkColorScheme(
                primary = Color(0xFF00E5FF),
                background = Color(0xFF0A0A0A),
                surface = Color(0xFF171717),
                error = Color(0xFFEF4444),
                onPrimary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )

            MaterialTheme(colorScheme = cyberpunkColors) {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
                    Image(
                        painter = painterResource(id = R.drawable.fundo_chip),
                        contentDescription = "Background",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.2f
                    )

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(
                                drawerContainerColor = Color(0xFF171717),
                                drawerContentColor = Color.White
                            ) {
                                Spacer(Modifier.height(24.dp))
                                Text(
                                    "TURBO CORE\nMOBILE V115",
                                    modifier = Modifier.padding(start = 24.dp, bottom = 12.dp),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF00E5FF)
                                )
                                Text("EDITION", modifier = Modifier.padding(start = 24.dp), color = Color.Gray, fontSize = 10.sp)
                                Divider(color = Color(0xFF333333), modifier = Modifier.padding(vertical = 12.dp))
                                
                                Screen.values().forEach { screen ->
                                    NavigationDrawerItem(
                                        label = { Text(screen.title, fontWeight = FontWeight.Bold) },
                                        selected = screen == currentScreen,
                                        onClick = {
                                            currentScreen = screen
                                            scope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                        colors = NavigationDrawerItemDefaults.colors(
                                            selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.15f),
                                            selectedTextColor = Color(0xFF00E5FF),
                                            unselectedTextColor = Color.Gray
                                        )
                                    )
                                }
                            }
                        }
                    ) {
                        Scaffold(
                            topBar = {
                                Column {
                                    CenterAlignedTopAppBar(
                                        title = {
                                            // Status Header Logic
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(if(isShizukuReady) "📱 SISTEMA PRONTO" else "❌ SHIZUKU OFF",
                                                    color = if(isShizukuReady) Color(0xFF10B981) else Color(0xFFEF4444),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        },
                                        navigationIcon = {
                                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                                Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
                                            }
                                        },
                                        actions = {
                                            IconButton(onClick = { runShizukuCommand("am kill-all") }) {
                                                Text("🚀", fontSize = 20.sp)
                                            }
                                        },
                                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                                    )
                                    // Sub-header stats
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("$batteryLevel | $temp | $ramUsage", color = Color.Gray, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                    }
                                    Divider(color = Color(0xFF333333))
                                }
                            },
                            containerColor = Color.Transparent
                        ) { innerPadding ->
                            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                                if (!isShizukuReady) {
                                     Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CyberCard(borderColor = Color(0xFFEF4444)) {
                                            Text("⚠️ SHIZUKU NECESSÁRIO", style = MaterialTheme.typography.titleLarge, color = Color(0xFFEF4444))
                                            Text("O app requer permissão root ou Shizuku.", color = Color.Gray)
                                            Text("Status: $shizukuState", color = Color.White, modifier = Modifier.padding(top=8.dp))
                                        }
                                     }
                                } else {
                                    when (currentScreen) {
                                        Screen.DASHBOARD -> DashboardScreen()
                                        Screen.COMPETITIVO -> CompetitivoScreen()
                                        Screen.APPS -> AppsScreen(context)
                                        Screen.TERMINAL -> TerminalScreen()
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
    fun DashboardScreen() {
        Column(
            modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Performance Card
            CyberCard("CONTROLE DE PERFORMANCE") {
                CyberButton("☢️ MODO BRUTO (Disable Thermal)", Color(0xFFFF9800)) {
                    runShizukuCommand("settings put global power_manager_constants disable_thermal_control=true")
                    runShizukuCommand("wm size 360x800")
                    changeDpiSafely(120)
                    runShizukuCommand("am kill-all")
                }
                Spacer(Modifier.height(8.dp))
                CyberButton("🚀 USUAL TURBO (Animations 0.5x)", Color(0xFF00E5FF)) {
                    runShizukuCommand("settings put global window_animation_scale 0.5")
                    runShizukuCommand("settings put global transition_animation_scale 0.5")
                    runShizukuCommand("settings put global animator_duration_scale 0.5")
                }
            }

            // Battery Card
            CyberCard("GERENCIADOR DE BATERIA") {
                CyberButton("🔋 SUPER ECONOMIA", Color(0xFF4CAF50)) {
                    runShizukuCommand("settings put global low_power 1")
                    runShizukuCommand("svc bluetooth disable")
                }
                Spacer(Modifier.height(8.dp))
                CyberButton("🪫 ULTRA ECONOMIA (Pixel)", Color.DarkGray) {
                    runShizukuCommand("wm size 360x800")
                    changeDpiSafely(120)
                    runShizukuCommand("settings put system screen_brightness 0")
                    runShizukuCommand("am kill-all")
                }
            }

            // Utils Card
            CyberCard("FERRAMENTAS DO SISTEMA") {
                 CyberButton("🔄 RESTAURAR ORIGINAL", Color.Gray) {
                    runShizukuCommand("wm size reset")
                    changeDpiSafely(null)
                    runShizukuCommand("settings put global low_power 0")
                    runShizukuCommand("pm enable com.samsung.android.game.gos")
                    runShizukuCommand("settings put global window_animation_scale 1")
                    runShizukuCommand("settings put global transition_animation_scale 1")
                    runShizukuCommand("settings put global animator_duration_scale 1")
                    runShizukuCommand("settings put global power_manager_constants disable_thermal_control=false")
                }
            }
        }
    }

    @Composable
    fun CompetitivoScreen() {
        Column(
            modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CyberCard("MODOS COMPETITIVOS") {
                CyberButton("🔥 GAMER ULTIMATE (No GOS)", Color(0xFFb91c1c)) {
                    runShizukuCommand("pm disable-user --user 0 com.samsung.android.game.gos")
                }
                Spacer(Modifier.height(8.dp))
                CyberButton("🎯 SENSI FREE FIRE (Capa)", Color(0xFF00E5FF)) {
                    changeDpiSafely(90)
                    runShizukuCommand("settings put system pointer_speed 7")
                }
                Spacer(Modifier.height(8.dp))
                CyberButton("🚀 FF LISO (Performance)", Color(0xFF22c55e)) {
                    runShizukuCommand("wm size 540x960")
                    changeDpiSafely(160)
                    runShizukuCommand("cmd power set-mode 1")
                    runShizukuCommand("settings put global window_animation_scale 0")
                    runShizukuCommand("am kill-all")
                }
            }
        }
    }

    @Composable
    fun AppsScreen(context: Context) {
        var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var searchText by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                apps = AppManager.getInstalledApps(context)
                isLoading = false
            }
        }

        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Buscar pacote...") },
                modifier = Modifier.fillMaxWidth().padding(bottom=16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            if (isLoading) {
                Text("Carregando apps...", color = Color.Gray)
            } else {
                val filteredApps = apps.filter { it.name.contains(searchText, ignoreCase = true) || it.packageName.contains(searchText, ignoreCase = true) }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredApps) { app ->
                        AppRow(app, context)
                    }
                }
            }
        }
    }

    @Composable
    fun AppRow(app: AppInfo, context: Context) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF171717)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (app.icon != null) {
                    Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(40.dp))
                }
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(app.name, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(app.packageName, fontSize = 10.sp, color = Color.Gray)
                }

                // Actions
                IconButton(onClick = {
                    try {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                        context.startActivity(launchIntent)
                    } catch(e: Exception) {}
                }) { Text("▶", color = Color(0xFF10B981)) }

                IconButton(onClick = { runShizukuCommand("am force-stop ${app.packageName}") }) {
                    Text("⚡", color = Color(0xFFFFFF00))
                }

                IconButton(onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_DELETE).apply {
                            data = Uri.parse("package:${app.packageName}")
                        }
                        context.startActivity(intent)
                    } catch(e: Exception) {}
                }) { Text("❌", color = Color(0xFFEF4444)) }
            }
        }
    }

    @Composable
    fun TerminalScreen() {
        var command by remember { mutableStateOf("") }
        var output by remember { mutableStateOf("Aguardando comando...") }
        val scope = rememberCoroutineScope()

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                label = { Text("Digite um comando (ex: wm size)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { 
                    scope.launch { output = runShizukuCommandWithOutput(command) }
                })
            )
            CyberButton("EXECUTAR", Color(0xFF00E5FF)) {
                scope.launch { 
                    output = "Executando..."
                    output = runShizukuCommandWithOutput(command) 
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF000000)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
            ) {
                Text(
                    text = output,
                    color = Color(0xFF00FF00),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())
                )
            }
        }
    }

    @Composable
    fun CyberCard(title: String = "", borderColor: Color = Color(0xFF333333), content: @Composable ColumnScope.() -> Unit) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF171717)),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (title.isNotEmpty()) {
                    Text(title, style = MaterialTheme.typography.labelMedium, color = Color(0xFFA1A1AA), modifier = Modifier.padding(bottom = 12.dp))
                }
                content()
            }
        }
    }

    @Composable
    fun CyberButton(text: String, color: Color, onClick: () -> Unit) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, color)
        ) {
            Text(
                text, 
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if(color == Color.White) Color.Black else Color.White
            )
        }
    }

    private inline fun safeRun(block: () -> Unit) {
        try { block() } catch (e: Exception) { Log.e("TurboCore", "Erro", e) }
    }

    private fun changeDpiSafely(density: Int?) {
        val command = if (density == null) "wm density reset" else {
            if (density < 72 || density > 640) return
            "wm density $density"
        }
        runShizukuCommand(command)
    }

    private fun runShizukuCommand(command: String) {
        safeRun {
            val shizukuClass = rikka.shizuku.Shizuku::class.java
            val newProcessMethod = shizukuClass.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            newProcessMethod.isAccessible = true
            newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null)
        }
    }

    private suspend fun runShizukuCommandWithOutput(command: String): String = withContext(Dispatchers.IO) {
        try {
            val shizukuClass = rikka.shizuku.Shizuku::class.java
            val newProcessMethod = shizukuClass.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) { output.append(line).append("\n") }
            
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            while (errorReader.readLine().also { line = it } != null) { output.append("ERRO: ").append(line).append("\n") }
            
            process.waitFor()
            output.toString().ifEmpty { "Comando executado (sem saída)." }
        } catch (e: Exception) { "Erro ao executar: ${e.message}" }
    }

    // Extension for Double formatting
    fun Double.format(digits: Int) = "%.${digits}f".format(this)
}
