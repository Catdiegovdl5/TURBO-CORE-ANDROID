package com.catdiego.turbocore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.catdiego.turbocore.ui.viewmodel.TerminalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel = viewModel()
) {
    val output by viewModel.terminalOutput.collectAsState()
    var inputCommand by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // Auto-scroll to bottom
    LaunchedEffect(output) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp)
    ) {
        // Output Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            Text(
                text = output,
                color = Color.Green,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }

        Divider(color = Color.DarkGray)

        // Input Area
        OutlinedTextField(
            value = inputCommand,
            onValueChange = { inputCommand = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Command", color = Color.LightGray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.Green,
                focusedBorderColor = Color.Green,
                unfocusedBorderColor = Color.DarkGray
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (inputCommand.isNotBlank()) {
                        viewModel.executeCommand(inputCommand)
                        inputCommand = ""
                    }
                }
            )
        )
    }
}
