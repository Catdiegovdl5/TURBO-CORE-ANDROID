package com.catdiego.turbocore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

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

        fun applySafeMode() {
            runShizukuCommand("wm size 540x960")
            runShizukuCommand("wm density 210")
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
        // Removed Image for Bulletproof Boot

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
                containerColor = Color.Transparent
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (currentScreen == Screen.INICIO) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Turbo Core Ativo", color = Color.White)
                            Spacer(Modifier.height(16.dp))
                            Text("Shizuku: ${if (shizukuAvailable) "Conectado" else "Desconectado"}", color = Color.Gray)
                        }
                    } else {
                        Text(currentScreen.title, color = Color.White)
                    }
                }
            }
        }
    }
}
