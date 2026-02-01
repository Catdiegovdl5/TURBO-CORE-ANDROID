package com.catdiego.turbocore

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

    // Shizuku permission listener
    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        // Handle permission result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register Shizuku listener
        try {
            Shizuku.addRequestPermissionResultListener(permissionListener)
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                if (Shizuku.isPreV11()) {
                    // Pre-v11 handling
                } else {
                    Shizuku.requestPermission(0)
                }
            }
        } catch (e: Exception) {
            // Shizuku not available
        }

        setContent {
            MaterialTheme {
                MainScreen()
            }
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
fun MainScreen() {
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
                Image(
                    painter = painterResource(id = R.drawable.fundo_chip),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Screen Content
                when (currentScreen) {
                    "Início" -> InicioScreen()
                    "Desempenho" -> DesempenhoScreen(snackbarHostState)
                    "Competitivo" -> CompetitivoScreen(snackbarHostState)
                }
            }
        }
    }
}

@Composable
fun InicioScreen() {
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
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Uso de RAM", color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = ramUsage,
            modifier = Modifier.fillMaxWidth().height(20.dp),
            color = Color.Green,
            trackColor = Color.DarkGray
        )
        Text("${(ramUsage * 100).toInt()}%", color = Color.White)
    }
}

@Composable
fun DesempenhoScreen(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                scope.launch {
                    val success = AppManager.applyGoldenRatioResolution(context, 720)
                    if (success) snackbarHostState.showSnackbar("Modo Bruto Ativado!")
                    else snackbarHostState.showSnackbar("Erro: Shizuku não autorizado.")
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
        ) {
            Text("Modo Bruto (720p)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    val success = AppManager.resetEverything()
                    if (success) snackbarHostState.showSnackbar("Sistema Resetado!")
                    else snackbarHostState.showSnackbar("Erro ao resetar.")
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Resetar Tudo")
        }
    }
}

@Composable
fun CompetitivoScreen(snackbarHostState: SnackbarHostState) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
         Text("Modo Competitivo", color = Color.White)
         Spacer(modifier = Modifier.height(16.dp))
         Button(
            onClick = {
                scope.launch {
                    val success = AppManager.resetEverything()
                    if (success) snackbarHostState.showSnackbar("Sistema Resetado!")
                    else snackbarHostState.showSnackbar("Erro ao resetar.")
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("Resetar Tudo")
        }
    }
}
