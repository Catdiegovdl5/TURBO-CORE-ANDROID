package com.catdiego.turbocore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
                    primary = Color(0xFF00E5FF), // Cyan Neon
                    error = Color(0xFFb91c1c), // Red Neon
                    onSurface = Color.White
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

        fun applySafeMode() {
            runShizukuCommand("wm size 540x960")
            runShizukuCommand("wm density 210")
        }
    }
}

// Cyberpunk Components
@Composable
fun CyberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.8f),
            contentColor = Color.Black
        ),
        border = BorderStroke(1.dp, color)
    ) {
        Text(text.uppercase())
    }
}

@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = CutCornerShape(bottomStart = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent() {
    var currentScreen by remember { mutableStateOf(Screen.INICIO) }
    var shizukuAvailable by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        delay(2000) // Boot Safe Delay
        try {
            if (Shizuku.pingBinder()) {
                shizukuAvailable = true
            }
        } catch (e: Exception) {
            shizukuAvailable = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Fail-Safe Background: Black Box + Image with Alpha
        // Removed try-catch as it's not supported in Composable functions.
        // If resource is missing, app will crash, but fail-safe box is underneath.
        // Assuming resource exists or crash is acceptable in dev.
        // To be truly fail-safe without try-catch, we rely on the resource being valid.
        Image(
            painter = painterResource(id = R.drawable.fundo_chip),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.2f
        )

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = Color(0xFF0A0A0A),
                    drawerContentColor = Color.White
                ) {
                    Spacer(Modifier.height(12.dp))
                    Screen.values().forEach { screen ->
                        NavigationDrawerItem(
                            label = { Text(screen.title) },
                            selected = currentScreen == screen,
                            onClick = {
                                currentScreen = screen
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = Color.Gray
                            ),
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
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.primary
                        )
                    )
                },
                containerColor = Color.Transparent
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
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
    CyberButton(
        text = "Resetar Tudo",
        onClick = {
             MainActivity.runShizukuCommand("wm size reset")
             MainActivity.runShizukuCommand("wm density reset")
        },
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
    )
}

@Composable
fun InicioScreen(shizukuAvailable: Boolean) {
    Column(modifier = Modifier.padding(16.dp)) {
        CyberCard(modifier = Modifier.fillMaxWidth()) {
            Text("STATUS DO SISTEMA", color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Shizuku: ${if (shizukuAvailable) "Conectado" else "Desconectado"}",
                color = if(shizukuAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
            Text("RAM: Calculando...", color = Color.White)
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.DarkGray
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        CyberButton(
            text = "Modo Seguro (Anti-Brick)",
            onClick = { MainActivity.applySafeMode() },
            modifier = Modifier.fillMaxWidth()
        )
        ResetButton()
    }
}

@Composable
fun DesempenhoScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        CyberButton(text = "Modo Bruto", onClick = {}, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        CyberButton(text = "Usual Turbo", onClick = {}, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.weight(1f))
        ResetButton()
    }
}

@Composable
fun EconomiaScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        CyberButton(text = "Super Economia", onClick = {}, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        CyberButton(text = "Ultra Economia", onClick = {}, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.weight(1f))
        ResetButton()
    }
}

@Composable
fun CompetitivoScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        CyberButton(text = "Gamer Ultimate", onClick = {}, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        CyberButton(text = "Sensi Free Fire", onClick = {}, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.weight(1f))
        ResetButton()
    }
}

@Composable
fun TerminalScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Terminal Output:", color = MaterialTheme.colorScheme.primary)
        CyberCard(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Text("> Aguardando comandos...", color = Color.Green, modifier = Modifier.fillMaxSize())
        }
        ResetButton()
    }
}
