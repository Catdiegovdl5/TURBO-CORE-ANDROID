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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import java.io.File

// Correção: Adicionei o OptIn para o Menu funcionar
@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    enum class Screen(val title: String) {
        DASHBOARD("🖥️ DASHBOARD"),
        COMPETITIVO("🎮 COMPETITIVO"),
        BATERIA("🔋 BATERIA"),
        SISTEMA("⚙️ SISTEMA")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()
            
            var shizukuState by remember { mutableStateOf("Aguardando Sistema...") }
            var isShizukuReady by remember { mutableStateOf(false) }
            var ramUsage by remember { mutableStateOf("Calculando...") }

            // Anti-Crash System
            LaunchedEffect(Unit) {
                delay(1500)
                safeRun {
                    try {
                        if (Shizuku.pingBinder()) {
                            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                                isShizukuReady = true
                                shizukuState = "Conectado"
                            } else {
                                if (Shizuku.shouldShowRequestPermissionRationale()) {
                                    shizukuState = "Sem Permissão"
                                } else {
                                    Shizuku.requestPermission(0)
                                    shizukuState = "Solicitando..."
                                }
                            }
                        } else {
                            shizukuState = "Shizuku Off"
                        }
                    } catch (e: Exception) {
                        shizukuState = "Erro"
                    }
                }
            }

            // Monitor
            LaunchedEffect(Unit) {
                while(true) {
                    safeRun {
                        try {
                            val memInfo = File("/proc/meminfo").readLines()
                            val total = memInfo.first { it.contains("MemTotal") }.filter { it.isDigit() }.toLong() / 1024
                            val avail = memInfo.first { it.contains("MemAvailable") }.filter { it.isDigit() }.toLong() / 1024
                            ramUsage = "RAM: ${total - avail}MB / ${total}MB"
                        } catch (e: Exception) { ramUsage = "..." }
                    }
                    delay(3000)
                }
            }

            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF00E5FF),
                    background = Color(0xFF0A0A0A),
                    surface = Color(0xFF171717)
                )
            ) {
                // Fundo com Gradiente
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
                    // Tenta carregar a imagem se existir, senão usa cor sólida
                    // (Isso evita crash se a imagem falhar)
                    
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(
                                drawerContainerColor = Color(0xFF171717),
                                drawerContentColor = Color.White
                            ) {
                                Spacer(Modifier.height(24.dp))
                                Text("TURBO CORE", modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.headlineMedium, color = Color(0xFF00E5FF))
                                Divider(color = Color(0xFF333333))
                                Screen.values().forEach { screen ->
                                    NavigationDrawerItem(
                                        label = { Text(screen.title) },
                                        selected = screen == currentScreen,
                                        onClick = { currentScreen = screen; scope.launch { drawerState.close() } },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                        colors = NavigationDrawerItemDefaults.colors(
                                            selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
                                            selectedTextColor = Color(0xFF00E5FF),
                                            unselectedTextColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    ) {
                        Scaffold(
                            topBar = {
                                CenterAlignedTopAppBar(
                                    title = { Text("TURBO CORE", color = Color(0xFF00E5FF)) },
                                    navigationIcon = {
                                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                            Icon(Icons.Filled.Menu, "Menu", tint = Color.White)
                                        }
                                    },
                                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                                )
                            },
                            containerColor = Color.Transparent
                        ) { innerPadding ->
                            Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
                                if (!isShizukuReady) {
                                    CyberCard(Color(0xFFb91c1c)) {
                                        Text("⚠️ SHIZUKU OFF", color = Color(0xFFb91c1c), style = MaterialTheme.typography.titleLarge)
                                        Text("Status: $shizukuState", color = Color.Gray)
                                    }
                                } else {
                                    when (currentScreen) {
                                        Screen.DASHBOARD -> DashboardScreen(ramUsage, shizukuState)
                                        Screen.COMPETITIVO -> CompetitivoScreen()
                                        Screen.BATERIA -> BateriaScreen()
                                        Screen.SISTEMA -> SistemaScreen()
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
    fun DashboardScreen(ram: String, status: String) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CyberCard { Text("RAM", color = Color.Gray); Text(ram, style = MaterialTheme.typography.headlineMedium, color = Color(0xFF00E5FF)) }
            CyberCard { Text("STATUS", color = Color.Gray); Text(status, style = MaterialTheme.typography.titleMedium, color = Color.Green) }
        }
    }

    @Composable
    fun CompetitivoScreen() {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CyberButton("🔥 GAMER ULTIMATE (No GOS)", Color(0xFFb91c1c)) { runShizukuCommand("pm disable-user --user 0 com.samsung.android.game.gos") }
            CyberButton("☢️ MODO BRUTO", Color(0xFFFF9800)) { 
                runShizukuCommand("settings put global power_manager_constants disable_thermal_control=true")
                runShizukuCommand("wm size 360x800; wm density 120; am kill-all")
            }
            CyberButton("🚀 FF LISO (540p)", Color(0xFF00E5FF)) { runShizukuCommand("wm size 540x960; wm density 160; am kill-all") }
        }
    }

    @Composable
    fun BateriaScreen() {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CyberButton("🔋 ECONOMIA NORMAL", Color.Green) { runShizukuCommand("settings put global low_power 1") }
            CyberButton("🪫 ULTRA ECONOMIA", Color.DarkGray) { 
                runShizukuCommand("wm size 360x800; wm density 120")
                runShizukuCommand("settings put system screen_brightness 0; am kill-all")
            }
        }
    }

    @Composable
    fun SistemaScreen() {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CyberButton("🔄 RESTAURAR ORIGINAL", Color.Gray) { 
                runShizukuCommand("wm size reset; wm density reset; settings put global low_power 0; pm enable com.samsung.android.game.gos")
            }
            CyberButton("🧹 LIMPAR RAM", Color(0xFF00E5FF)) { runShizukuCommand("am kill-all") }
        }
    }

    @Composable
    fun CyberCard(borderColor: Color = Color(0xFF333333), content: @Composable ColumnScope.() -> Unit) {
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF171717)), border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }

    @Composable
    fun CyberButton(text: String, color: Color, onClick: () -> Unit) {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.8f)), shape = MaterialTheme.shapes.small) {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (color == Color.White || color == Color.Green) Color.Black else Color.White)
        }
    }

    private fun runShizukuCommand(cmd: String) {
        safeRun {
            val clazz = rikka.shizuku.Shizuku::class.java
            val method = clazz.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            method.isAccessible = true
            method.invoke(null, arrayOf("sh", "-c", cmd), null, null)
        }
    }

    private inline fun safeRun(block: () -> Unit) { try { block() } catch (e: Exception) {} }
}
