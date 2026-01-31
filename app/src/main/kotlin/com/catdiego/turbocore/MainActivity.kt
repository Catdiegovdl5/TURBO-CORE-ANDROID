package com.catdiego.turbocore

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
        INICIO("🏠 INÍCIO"),
        DESEMPENHO("⚡ DESEMPENHO"),
        ECONOMIA("🔋 ECONOMIA"),
        COMPETITIVO("🏆 COMPETITIVO"),
        TERMINAL("💻 TERMINAL")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentScreen by remember { mutableStateOf(Screen.INICIO) }
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            
            var shizukuState by remember { mutableStateOf("Aguardando Sistema...") }
            var isShizukuReady by remember { mutableStateOf(false) }
            var ramUsage by remember { mutableStateOf("Calculando...") }

            LaunchedEffect(Unit) {
                delay(1500)
                safeRun {
                    try {
                        if (Shizuku.pingBinder()) {
                            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                                isShizukuReady = true
                                shizukuState = "Conectado e Seguro"
                            } else {
                                if (Shizuku.shouldShowRequestPermissionRationale()) {
                                    shizukuState = "Permissão Negada"
                                } else {
                                    Shizuku.requestPermission(0)
                                    shizukuState = "Solicitando Acesso..."
                                }
                            }
                        } else {
                            shizukuState = "Shizuku não rodando"
                        }
                    } catch (e: Exception) {
                        shizukuState = "Erro Crítico: ${e.message}"
                    }
                }
            }

            DisposableEffect(Unit) {
                val listener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        isShizukuReady = true
                        shizukuState = "Conectado e Seguro"
                    } else {
                        shizukuState = "Permissão Negada pelo Usuário"
                    }
                }
                Shizuku.addRequestPermissionResultListener(listener)
                onDispose { Shizuku.removeRequestPermissionResultListener(listener) }
            }

            LaunchedEffect(Unit) {
                while(true) {
                    safeRun {
                        try {
                            val memInfo = File("/proc/meminfo").readLines()
                            val total = memInfo.first { it.contains("MemTotal") }.filter { it.isDigit() }.toLong() / 1024
                            val avail = memInfo.first { it.contains("MemAvailable") }.filter { it.isDigit() }.toLong() / 1024
                            ramUsage = "RAM: ${total - avail}MB / ${total}MB"
                        } catch (e: Exception) { ramUsage = "RAM: Erro Leit." }
                    }
                    delay(3000)
                }
            }

            val cyberpunkColors = darkColorScheme(
                primary = Color(0xFF00E5FF),
                background = Color(0xFF0A0A0A),
                surface = Color(0xFF171717),
                error = Color(0xFFb91c1c),
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
                                    "TURBO CORE V115",
                                    modifier = Modifier.padding(start = 24.dp, bottom = 12.dp),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF00E5FF)
                                )
                                Divider(color = Color(0xFF333333))
                                Spacer(Modifier.height(12.dp))
                                
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
                                CenterAlignedTopAppBar(
                                    title = { 
                                        Text(currentScreen.title, 
                                            color = Color(0xFF00E5FF),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        ) 
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(imageVector = Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
                                        }
                                    },
                                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                                )
                            },
                            containerColor = Color.Transparent
                        ) { innerPadding ->
                            Column(
                                modifier = Modifier
                                    .padding(innerPadding)
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                if (!isShizukuReady) {
                                    CyberCard(borderColor = Color(0xFFb91c1c)) {
                                        Text("⚠️ SHIZUKU OFF", style = MaterialTheme.typography.titleLarge, color = Color(0xFFb91c1c))
                                        Text("Status: $shizukuState", color = Color.White)
                                        Text("O app requer Shizuku para funcionar.", color = Color.Gray)
                                    }
                                } else {
                                    when (currentScreen) {
                                        Screen.INICIO -> DashboardScreen(ramUsage, shizukuState)
                                        Screen.DESEMPENHO -> DesempenhoScreen()
                                        Screen.ECONOMIA -> EconomiaScreen()
                                        Screen.COMPETITIVO -> CompetitivoScreen()
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
    fun DashboardScreen(ramUsage: String, shizukuStatus: String) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CyberCard {
                Text("MONITORAMENTO", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Text(ramUsage, style = MaterialTheme.typography.headlineMedium, color = Color(0xFF00E5FF))
            }
            CyberCard {
                Text("STATUS SHIZUKU", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Text(shizukuStatus, style = MaterialTheme.typography.titleMedium, color = Color.Green)
            }
        }
    }

    @Composable
    fun DesempenhoScreen() {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CyberButton("☢️ MODO BRUTO", Color(0xFFFF9800)) {
                runShizukuCommand("settings put global power_manager_constants disable_thermal_control=true")
                runShizukuCommand("wm size 360x800")
                changeDpiSafely(120)
                runShizukuCommand("am kill-all")
            }
            
            CyberButton("🚀 USUAL TURBO", Color(0xFF00E5FF)) {
                runShizukuCommand("settings put global window_animation_scale 0.5")
                runShizukuCommand("settings put global transition_animation_scale 0.5")
                runShizukuCommand("settings put global animator_duration_scale 0.5")
            }
            Spacer(Modifier.height(24.dp))
            ResetButton()
        }
    }

    @Composable
    fun EconomiaScreen() {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CyberButton("🔋 SUPER ECONOMIA", Color(0xFF4CAF50)) {
                runShizukuCommand("settings put global low_power 1")
                runShizukuCommand("svc bluetooth disable")
            }
            CyberButton("🪫 ULTRA ECONOMIA (Pixel)", Color.DarkGray) {
                runShizukuCommand("wm size 360x800")
                changeDpiSafely(120)
                runShizukuCommand("settings put system screen_brightness 0")
                runShizukuCommand("am kill-all")
            }
            Spacer(Modifier.height(24.dp))
            ResetButton()
        }
    }

    @Composable
    fun CompetitivoScreen() {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CyberButton("🔥 GAMER ULTIMATE (No GOS)", Color(0xFFb91c1c)) {
                runShizukuCommand("pm disable-user --user 0 com.samsung.android.game.gos")
            }
            CyberButton("🎯 SENSI FREE FIRE (Capa)", Color(0xFF00E5FF)) {
                changeDpiSafely(90)
                runShizukuCommand("settings put system pointer_speed 7")
            }
            Spacer(Modifier.height(24.dp))
            ResetButton()
        }
    }

    @Composable
    fun TerminalScreen() {
        var command by remember { mutableStateOf("") }
        var output by remember { mutableStateOf("Aguardando comando...") }
        val scope = rememberCoroutineScope()

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                modifier = Modifier.fillMaxWidth().height(300.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF171717)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
            ) {
                Text(
                    text = output,
                    color = Color.Green,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())
                )
            }
        }
    }

    @Composable
    fun ResetButton() {
        CyberButton("🔄 RESETAR TUDO", Color.Gray) {
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

    @Composable
    fun CyberCard(borderColor: Color = Color(0xFF333333), content: @Composable ColumnScope.() -> Unit) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF171717)),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }

    @Composable
    fun CyberButton(text: String, color: Color, onClick: () -> Unit) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.8f)),
            shape = MaterialTheme.shapes.small,
            border = androidx.compose.foundation.BorderStroke(1.dp, color)
        ) {
            Text(
                text, 
                fontSize = 16.sp, 
                fontWeight = FontWeight.Bold,
                color = if(color == Color.White || color == Color.Green || color == Color(0xFF00E5FF)) Color.Black else Color.White
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
}
