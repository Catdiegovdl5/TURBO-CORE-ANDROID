package com.catdiego.turbocore

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private var isShizukuInstalledState by mutableStateOf(false)
    private var isShizukuPermissionGrantedState by mutableStateOf(false)

    // Shizuku permission listener
    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            isShizukuPermissionGrantedState = true
        } else {
            isShizukuPermissionGrantedState = false
            runOnUiThread {
                Toast.makeText(
                    this,
                    "Acesso negado. O Turbo Core precisa do Shizuku para otimizar o sistema.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. INSTALLATION CHECK
        isShizukuInstalledState = isShizukuInstalled(this)

        // Register Shizuku listener if installed
        if (isShizukuInstalledState) {
            try {
                Shizuku.addRequestPermissionResultListener(permissionListener)
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    isShizukuPermissionGrantedState = true
                } else {
                    if (Shizuku.isPreV11()) {
                        // Pre-v11 handling
                    } else {
                        Shizuku.requestPermission(0)
                    }
                }
            } catch (e: Exception) {
                // Shizuku service might not be running even if installed
                isShizukuPermissionGrantedState = false
            }
        }

        setContent {
            MaterialTheme {
                MainScreen(
                    isShizukuInstalled = isShizukuInstalledState,
                    isShizukuPermissionGranted = isShizukuPermissionGrantedState
                )
            }
        }
    }

    private fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("rikka.app.shizuku", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            Shizuku.removeRequestPermissionResultListener(permissionListener)
        } catch (e: Exception) {
            // Ignore
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    isShizukuInstalled: Boolean,
    isShizukuPermissionGranted: Boolean
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf("Início") }
    val snackbarHostState = remember { SnackbarHostState() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    label = { Text("Início") },
                    selected = currentScreen == "Início",
                    onClick = {
                        currentScreen = "Início"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Economia") },
                    selected = currentScreen == "Economia",
                    onClick = {
                        currentScreen = "Economia"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Desempenho") },
                    selected = currentScreen == "Desempenho",
                    onClick = {
                        currentScreen = "Desempenho"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Competitivo") },
                    selected = currentScreen == "Competitivo",
                    onClick = {
                        currentScreen = "Competitivo"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Terminal") },
                    selected = currentScreen == "Terminal",
                    onClick = {
                        currentScreen = "Terminal"
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Turbo Core") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_sort_by_size),
                                contentDescription = "Menu"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFF0A0A0A),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A0A0A))
                    .padding(paddingValues)
            ) {
                // Background Image
                val context = LocalContext.current
                val isResourceValid = remember {
                    try {
                        if (R.drawable.fundo_chip != 0) {
                            context.getDrawable(R.drawable.fundo_chip)
                            true
                        } else {
                            false
                        }
                    } catch (e: Exception) {
                        false
                    }
                }

                if (isResourceValid) {
                    Image(
                        painter = painterResource(id = R.drawable.fundo_chip),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Screen Content
                when (currentScreen) {
                    "Início" -> InicioScreen(isShizukuInstalled, isShizukuPermissionGranted, snackbarHostState)
                    "Economia" -> EconomiaScreen(snackbarHostState)
                    "Desempenho" -> DesempenhoScreen(snackbarHostState)
                    "Competitivo" -> CompetitivoScreen(snackbarHostState)
                    "Terminal" -> TerminalScreen(snackbarHostState)
                }
            }
        }
    }
}

@Composable
fun ResetButton(snackbarHostState: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    Button(
        onClick = {
            scope.launch {
                val success = AppManager.resetEverything()
                if (success) snackbarHostState.showSnackbar("Sistema Resetado!")
                else snackbarHostState.showSnackbar("Erro ao resetar.")
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        Text("Resetar Tudo")
    }
}

@Composable
fun ActionButton(
    text: String,
    snackbarHostState: SnackbarHostState,
    onClick: suspend () -> Boolean
) {
    val scope = rememberCoroutineScope()
    var buttonColor by remember { mutableStateOf(Color(0xFFFF9800)) } // Default Orange
    var buttonText by remember { mutableStateOf(text) }
    var isLoading by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (!isLoading) {
                scope.launch {
                    isLoading = true
                    val success = onClick()
                    isLoading = false
                    if (success) {
                        buttonColor = Color.Green
                        buttonText = "Aplicado"
                        snackbarHostState.showSnackbar("$text Aplicado!")
                        delay(2000)
                        buttonColor = Color(0xFFFF9800)
                        buttonText = text
                    } else {
                        snackbarHostState.showSnackbar("Erro: Falha ou Sem Permissão.")
                    }
                }
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(buttonText)
        }
    }
}

@Composable
fun CyberCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.Cyan)
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}


@Composable
fun InicioScreen(
    isShizukuInstalled: Boolean,
    isShizukuPermissionGranted: Boolean,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    var ramUsage by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            ramUsage = AppManager.getRamUsage(context)
            delay(1000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Shizuku Status
        Text("Status Shizuku", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))

        if (!isShizukuInstalled) {
            Text("Não Instalado", color = Color.Red)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app"))
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("Baixar Shizuku")
            }
        } else if (isShizukuPermissionGranted) {
            Text("Funcionando (Permissão Concedida)", color = Color.Green)
        } else {
            Text("Permissão Pendente / Serviço Parado", color = Color.Yellow)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // RAM Usage
        Text("Uso de RAM", color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = ramUsage,
            modifier = Modifier.fillMaxWidth().height(20.dp),
            color = Color.Green,
            trackColor = Color.DarkGray
        )
        Text("${(ramUsage * 100).toInt()}%", color = Color.White)

        Spacer(modifier = Modifier.weight(1f))
        ResetButton(snackbarHostState)
    }
}

@Composable
fun EconomiaScreen(snackbarHostState: SnackbarHostState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text("Modo Economia", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        CyberCard("Opções de Economia") {
            ActionButton("Super Economia", snackbarHostState) { AppManager.enableSuperEconomy() }
            ActionButton("Ultra Economia", snackbarHostState) { AppManager.enableUltraEconomy() }
        }

        Spacer(modifier = Modifier.weight(1f))
        ResetButton(snackbarHostState)
    }
}

@Composable
fun DesempenhoScreen(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text("Modo Desempenho", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        CyberCard("Resolução") {
            ActionButton("Modo Safe (540x1200)", snackbarHostState) {
                ShellEngine.runCommand("wm size 540x1200")
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        ResetButton(snackbarHostState)
    }
}

@Composable
fun CompetitivoScreen(snackbarHostState: SnackbarHostState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text("Modo Competitivo", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        CyberCard("Gaming") {
            ActionButton("DPI Safe (210)", snackbarHostState) {
                ShellEngine.runCommand("wm density 210")
            }
            ActionButton("Gamer Ultimate", snackbarHostState) { AppManager.enableGamerUltimate() }
            ActionButton("Sensi Free Fire", snackbarHostState) { AppManager.enableSensiFreeFire() }
        }

        Spacer(modifier = Modifier.weight(1f))
        ResetButton(snackbarHostState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(snackbarHostState: SnackbarHostState) {
    var command by remember { mutableStateOf("") }
    val outputLog = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Terminal", color = Color.White, style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = command,
            onValueChange = { command = it },
            label = { Text("Comando", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Green,
                unfocusedTextColor = Color.Green,
                focusedContainerColor = Color(0xFF1E1E1E),
                unfocusedContainerColor = Color(0xFF1E1E1E),
                cursorColor = Color.Green
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    outputLog.add("> $command (Executando...)")
                    val result = ShellEngine.runCommandWithOutput(command)
                    outputLog.add(result)
                    command = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
        ) {
            Text("Executar", color = Color.Green)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0A0A0A))
                .padding(8.dp)
        ) {
            LazyColumn {
                items(outputLog.size) { index ->
                    Text(outputLog[index], color = Color.Green, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        ResetButton(snackbarHostState)
    }
}
