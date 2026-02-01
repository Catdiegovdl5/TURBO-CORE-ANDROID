package com.catdiego.turbocore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import com.catdiego.turbocore.R

enum class Screen(val title: String) {
    INICIO("Inicio"),
    DESEMPENHO("Desempenho"),
    ECONOMIA("Economia"),
    COMPETITIVO("Competitivo"),
    TERMINAL("Terminal")
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0A0A0A),
                    surface = Color(0xFF171717),
                    primary = Color(0xFF00E5FF),
                    error = Color(0xFFb91c1c)
                )
            ) {
                MainContent()
            }
        }
    }

    companion object {
        fun runShizukuCommand(command: String): String {
            return try {
                val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
                val newProcessMethod = shizukuClass.getMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as Process

                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                process.waitFor()
                output.toString()
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }

        private fun calculateNewSize(targetWidth: Int): Int {
            val output = runShizukuCommand("wm size")
            // Output format example: "Physical size: 1080x2400"
            val regex = Regex("Physical size: (\\d+)x(\\d+)")
            val match = regex.find(output)

            return if (match != null) {
                val (widthStr, heightStr) = match.destructured
                val width = widthStr.toFloat()
                val height = heightStr.toFloat()
                val aspectRatio = height / width
                (targetWidth * aspectRatio).toInt()
            } else {
                // Fallback if parsing fails (e.g. 20:9 ratio generic)
                (targetWidth * 2.22).toInt()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent() {
    var currentScreen by remember { mutableStateOf(Screen.INICIO) }
    var shizukuAvailable by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Boot Safe Delay: 2000ms
    LaunchedEffect(Unit) {
        delay(2000)
        try {
            if (Shizuku.pingBinder()) {
                shizukuAvailable = true
            }
        } catch (e: Exception) {
            shizukuAvailable = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Fallback background
        Image(
            painter = painterResource(id = R.drawable.fundo_chip),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(Modifier.height(12.dp))
                    Screen.values().forEach { screen ->
                        NavigationDrawerItem(
                            label = { Text(screen.title) },
                            selected = currentScreen == screen,
                            onClick = {
                                currentScreen = screen
                                scope.launch { drawerState.close() }
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(currentScreen.title) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.primary
                        )
                    )
                },
                containerColor = Color.Transparent // Allow background image to show
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    when (currentScreen) {
                        Screen.INICIO -> InicioScreen(shizukuAvailable)
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

@Composable
fun ResetButton() {
    Button(
        onClick = { /* Reset logic */ },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFb91c1c)),
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
    ) {
        Text("Resetar Tudo")
    }
}

@Composable
fun InicioScreen(shizukuAvailable: Boolean) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Status Shizuku: ${if (shizukuAvailable) "Conectado" else "Desconectado"}", color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))
        Text("RAM Usage: calculating...", color = Color.White)
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.weight(1f))
        ResetButton()
    }
}

@Composable
fun DesempenhoScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Modo Bruto") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Usual Turbo") }
        Spacer(modifier = Modifier.weight(1f))
        ResetButton()
    }
}

@Composable
fun EconomiaScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Super Economia") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Ultra Economia") }
        Spacer(modifier = Modifier.weight(1f))
        ResetButton()
    }
}

@Composable
fun CompetitivoScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Gamer Ultimate") }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Sensi Free Fire") }
        Spacer(modifier = Modifier.weight(1f))
        ResetButton()
    }
}

@Composable
fun TerminalScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Terminal Output:", color = Color.White)
        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black.copy(alpha=0.5f)))
        ResetButton()
    }
}
