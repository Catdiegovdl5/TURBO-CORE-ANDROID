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
import androidx.compose.ui.Alignment
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

class MainActivity : ComponentActivity() {

    // Definição das Telas/Abas
    enum class Screen(val title: String) {
        DASHBOARD("🖥️ DASHBOARD"),
        COMPETITIVO("🎮 COMPETITIVO"),
        BATERIA("🔋 BATERIA"),
        SISTEMA("⚙️ SISTEMA")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // --- ESTADO GLOBAL ---
            var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            var shizukuState by remember { mutableStateOf("Aguardando Sistema...") }
            var isShizukuReady by remember { mutableStateOf(false) }
            var ramUsage by remember { mutableStateOf("Calculando...") }

            // --- ANTI-CRASH SYSTEM (ANDROID 16/MEDIATEK) ---
            LaunchedEffect(Unit) {
                delay(1500) // Delay Crítico para estabilização do Binder (MtkPower)
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

            // --- TEMA CYBERPUNK ---
            val cyberpunkColors = darkColorScheme(
                primary = Color(0xFF00E5FF), // Studio Blue
                background = Color(0xFF0A0A0A), // Darkest
                surface = Color(0xFF171717), // Panel Background
                error = Color(0xFFb91c1c), // ROG Red
                onPrimary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )

            MaterialTheme(colorScheme = cyberpunkColors) {
                // Background Box Global
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
                    // Imagem de Fundo
                    Image(
                        painter = painterResource(id = R.drawable.fundo_chip),
                        contentDescription = "Background",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.3f // Leve transparência para não ofuscar o texto
                    )

                    // Navigation Drawer
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(
                                drawerContainerColor = Color(0xFF171717),
                                drawerContentColor = Color.White
                            ) {
                                Spacer(Modifier.height(24.dp))
                                Text(
                                    "TURBO CORE",
                                    modifier = Modifier.padding(start = 24.dp, bottom = 12.dp),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF00E5FF)
                                )
                                Divider(color = Color(0xFF333333))
                                Spacer(Modifier.height(12.dp))

                                Screen.values().forEach { screen ->
                                    NavigationDrawerItem(
                                        label = { Text(screen.title, fontWeight = FontWeight.SemiBold) },
                                        selected = screen == currentScreen,
                                        onClick = {
                                            currentScreen = screen
                                            scope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                                        colors = NavigationDrawerItemDefaults.colors(
                                            selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.15f),
                                            selectedTextColor = Color(0xFF00E5FF),
                                            unselectedTextColor = Color.Gray,
                                            unselectedContainerColor = Color.Transparent
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
                                            fontWeight = FontWeight.Bold,
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
                                        containerColor = Color.Transparent // Transparente para ver o fundo
                                    )
                                )
                            },
                            containerColor = Color.Transparent // Transparente para ver o fundo
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
                                        Text("O app requer Shizuku para aplicar otimizações.", color = Color.Gray)
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

    // --- TELAS ---

    @Composable
    fun DashboardScreen(ramUsage: String, shizukuStatus: String) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CyberCard {
                Text("MONITORAMENTO", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Text(ramUsage, style = MaterialTheme.typography.headlineMedium, color = Color(0xFF00E5FF))
            }
            CyberCard {
                Text("SHIZUKU CORE", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Text(shizukuStatus, style = MaterialTheme.typography.titleMedium, color = Color.Green)
            }
            Spacer(modifier = Modifier.height(8.dp))
            CyberButton("🧹 Limpar RAM (Kill-All)", Color(0xFF00E5FF)) {
                runShizukuCommand("am kill-all")
            }
        }
    }

    @Composable
    fun CompetitivoScreen() {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("MODOS DE PERFORMANCE", color = Color.White, style = MaterialTheme.typography.titleMedium)

            CyberButton("🔥 Gamer Ultimate (Mobile)", Color(0xFFb91c1c)) {
                // Desativa GOS
                runShizukuCommand("pm disable-user --user 0 com.samsung.android.game.gos")
            }

            CyberButton("☢️ Ultimate Desempenho (Bruto)", Color(0xFFFF9800)) {
                // Sem termal, 360p, 120dpi
                runShizukuCommand("settings put global power_manager_constants disable_thermal_control=true")
                runShizukuCommand("wm size 360x800")
                changeDpiSafely(120)
                runShizukuCommand("am kill-all")
            }

            CyberButton("⚡ Usual Turbo", Color(0xFF00E5FF)) {
                // Animações 0.5x
                runShizukuCommand("settings put global window_animation_scale 0.5")
                runShizukuCommand("settings put global transition_animation_scale 0.5")
            }

            CyberButton("🚀 FF LISO (540p)", Color(0xFF00E5FF)) {
                runShizukuCommand("wm size 540x960")
                changeDpiSafely(160)
                runShizukuCommand("am kill-all")
            }
        }
    }

    @Composable
    fun BateriaScreen() {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("GERENCIAMENTO DE ENERGIA", color = Color.White, style = MaterialTheme.typography.titleMedium)

            CyberButton("🔋 Economia Normal", Color.Green) {
                runShizukuCommand("settings put global low_power 1")
            }

            CyberButton("📉 Super Economia", Color(0xFF4CAF50)) {
                runShizukuCommand("wm size 576x1280")
                changeDpiSafely(240)
                runShizukuCommand("svc bluetooth disable")
            }

            CyberButton("🪫 Ultimate Economia (Deep)", Color.DarkGray) {
                runShizukuCommand("wm size 360x800")
                changeDpiSafely(120)
                runShizukuCommand("settings put system screen_brightness 0")
                runShizukuCommand("am kill-all")
            }
        }
    }

    @Composable
    fun SistemaScreen() {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("FERRAMENTAS DO SISTEMA", color = Color.White, style = MaterialTheme.typography.titleMedium)

            CyberButton("🔄 Restaurar Original", Color.Gray) {
                runShizukuCommand("wm size reset")
                changeDpiSafely(null) // Reset DPI
                runShizukuCommand("settings put global low_power 0")
                runShizukuCommand("pm enable com.samsung.android.game.gos")
                runShizukuCommand("settings put global window_animation_scale 1") // Reset padrão
            }

            CyberButton("🧹 Limpar RAM", Color(0xFF00E5FF)) {
                runShizukuCommand("am kill-all")
            }
        }
    }

    // --- COMPONENTES UI CUSTOMIZADOS ---

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

    // --- LOGIC UTILS ---

    private inline fun safeRun(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Log.e("TurboCore", "Erro capturado em safeRun", e)
        }
    }

    private fun changeDpiSafely(density: Int?) {
        val command = if (density == null) {
            "wm density reset"
        } else {
            if (density < 72 || density > 640) return
            "wm density $density"
        }
        runShizukuCommand(command)
    }

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
