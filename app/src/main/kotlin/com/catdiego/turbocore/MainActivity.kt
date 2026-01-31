package com.catdiego.turbocore

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import java.io.File

class MainActivity : ComponentActivity() {

    // Definição das Telas/Abas
    enum class Screen(val title: String) {
        DASHBOARD("🖥️ Dashboard"),
        PERFORMANCE("🎮 Performance"),
        BATTERY("🔋 Bateria"),
        SYSTEM("⚙️ Sistema")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // --- ESTADO GLOBAL E SEGURANÇA ---
            var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            var shizukuState by remember { mutableStateOf("Aguardando Sistema...") }
            var isShizukuReady by remember { mutableStateOf(false) }
            var ramUsage by remember { mutableStateOf("Calculando...") }

            // BLINDAGEM CONTRA CRASH EM ANDROID 16 (MediaTek) - MANTIDO
            LaunchedEffect(Unit) {
                delay(1500) // Delay Crítico
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
                        Log.e("TurboCore", "Falha crítica ao conectar no Shizuku", e)
                        shizukuState = "Erro Crítico: ${e.message}"
                    }
                }
            }

            // Listener de Permissão
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

            // Monitor de RAM
            LaunchedEffect(Unit) {
                while(true) {
                    safeRun {
                        try {
                            val memInfo = File("/proc/meminfo").readLines()
                            val total = memInfo.first { it.contains("MemTotal") }.filter { it.isDigit() }.toLong() / 1024
                            val avail = memInfo.first { it.contains("MemAvailable") }.filter { it.isDigit() }.toLong() / 1024
                            ramUsage = "RAM: ${total - avail}MB / ${total}MB"
                        } catch (e: Exception) {
                            ramUsage = "RAM: Erro Leit."
                        }
                    }
                    delay(3000)
                }
            }

            // --- TEMA E UI ---
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF00E5FF), // Ciano Hacker
                    background = Color(0xFF121212), // Fundo Dark
                    surface = Color(0xFF1E1E1E),
                    onPrimary = Color.Black,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = Color(0xFF1E1E1E),
                            drawerContentColor = Color.White
                        ) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "TURBO MENU",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color(0xFF00E5FF)
                            )
                            Divider(color = Color.Gray)
                            Screen.values().forEach { screen ->
                                NavigationDrawerItem(
                                    label = { Text(screen.title) },
                                    selected = screen == currentScreen,
                                    onClick = {
                                        currentScreen = screen
                                        scope.launch { drawerState.close() }
                                    },
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
                                title = {
                                    Text("TURBO CORE",
                                        color = Color(0xFF00E5FF),
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(
                                            imageVector = Icons.Filled.Menu,
                                            contentDescription = "Menu",
                                            tint = Color.White
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = Color(0xFF121212)
                                )
                            )
                        },
                        containerColor = Color(0xFF121212)
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (!isShizukuReady) {
                                // Aviso Bloqueante se Shizuku não estiver pronto
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFb91c1c))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("⚠️ SHIZUKU NÃO DETECTADO", style = MaterialTheme.typography.titleLarge, color = Color.White)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Status: $shizukuState", color = Color.White)
                                        Text("Certifique-se que o Shizuku está rodando e tente novamente.", color = Color.White)
                                    }
                                }
                            } else {
                                // Conteúdo das Telas
                                when (currentScreen) {
                                    Screen.DASHBOARD -> DashboardScreen(ramUsage, shizukuState)
                                    Screen.PERFORMANCE -> PerformanceScreen()
                                    Screen.BATTERY -> BatteryScreen()
                                    Screen.SYSTEM -> SystemScreen()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- COMPOSABLES DAS TELAS ---

    @Composable
    fun DashboardScreen(ramUsage: String, shizukuStatus: String) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            InfoCard("Monitoramento", ramUsage, Color.Green)
            InfoCard("Status Shizuku", shizukuStatus, Color.Cyan)

            Text("Ações Rápidas", style = MaterialTheme.typography.titleMedium, color = Color.Gray)

            CyberButton(
                text = "🧹 LIMPAR RAM (Kill-All)",
                color = Color(0xFF00E5FF),
                onClick = { runShizukuCommand("am kill-all") }
            )
        }
    }

    @Composable
    fun PerformanceScreen() {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Modos de Jogo & Performance", style = MaterialTheme.typography.titleLarge, color = Color.White)

            CyberButton(
                text = "🔥 GAMER ULTIMATE (No GOS)",
                color = Color(0xFFb91c1c), // Vermelho
                onClick = { runShizukuCommand("pm disable-user --user 0 com.samsung.android.game.gos") }
            )

            CyberButton(
                text = "☢️ MODO BRUTO (Sem Térmica)",
                color = Color(0xFFFF9800), // Laranja
                onClick = { runShizukuCommand("settings put global power_manager_constants disable_thermal_control=true") }
            )

            CyberButton(
                text = "🚀 FF LISO (540p Performance)",
                color = Color(0xFF00E5FF),
                onClick = {
                    runShizukuCommand("wm size 540x960")
                    changeDpiSafely(160)
                    runShizukuCommand("am kill-all")
                }
            )
        }
    }

    @Composable
    fun BatteryScreen() {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Gerenciamento de Energia", style = MaterialTheme.typography.titleLarge, color = Color.White)

            CyberButton(
                text = "🔋 ECONOMIA NORMAL",
                color = Color.Green,
                onClick = { runShizukuCommand("settings put global low_power 1") }
            )

            CyberButton(
                text = "🪫 ULTRA ECONOMIA (Pixel)",
                color = Color.DarkGray,
                onClick = {
                    runShizukuCommand("wm size 360x800")
                    changeDpiSafely(120)
                    runShizukuCommand("settings put system screen_brightness 0")
                    runShizukuCommand("am kill-all")
                }
            )
        }
    }

    @Composable
    fun SystemScreen() {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Ferramentas do Sistema", style = MaterialTheme.typography.titleLarge, color = Color.White)

            CyberButton(
                text = "🔄 RESTAURAR PADRÃO (Reset)",
                color = Color.Gray,
                onClick = {
                    runShizukuCommand("wm size reset")
                    changeDpiSafely(null) // Reset DPI
                    runShizukuCommand("settings put global low_power 0")
                    runShizukuCommand("settings put global window_animation_scale 1")
                    runShizukuCommand("pm enable com.samsung.android.game.gos") // Reativa GOS
                }
            )
        }
    }

    // --- COMPONENTES UI AUXILIARES ---

    @Composable
    fun InfoCard(title: String, value: String, valueColor: Color) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                Text(value, style = MaterialTheme.typography.headlineSmall, color = valueColor)
            }
        }
    }

    @Composable
    fun CyberButton(text: String, color: Color, onClick: () -> Unit) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(text, fontSize = 16.sp, color = if(color == Color.White || color == Color.Green || color == Color(0xFF00E5FF)) Color.Black else Color.White)
        }
    }

    // --- LÓGICA DO SISTEMA (MANTIDA/ADAPTADA) ---

    /**
     * Wrapper de segurança para evitar crashes não tratados.
     */
    private inline fun safeRun(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Log.e("TurboCore", "Erro capturado em safeRun", e)
        }
    }

    /**
     * Altera a DPI do dispositivo de forma segura.
     */
    private fun changeDpiSafely(density: Int?) {
        val command = if (density == null) {
            "wm density reset"
        } else {
            if (density < 72 || density > 640) return
            "wm density $density"
        }
        runShizukuCommand(command)
    }

    /**
     * Executa comandos shell via Shizuku usando Reflexão.
     */
    private fun runShizukuCommand(command: String) {
        safeRun {
            val shizukuClass = rikka.shizuku.Shizuku::class.java
            val newProcessMethod = shizukuClass.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null)
        }
    }
}
