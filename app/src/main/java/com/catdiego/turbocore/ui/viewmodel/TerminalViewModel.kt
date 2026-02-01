package com.catdiego.turbocore.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catdiego.turbocore.util.ShellEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.InputStreamReader

class TerminalViewModel : ViewModel() {

    private val _terminalOutput = MutableStateFlow("")
    val terminalOutput: StateFlow<String> = _terminalOutput.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val mutex = Mutex()

    fun executeCommand(command: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isRunning.value = true
            appendOutput("$ $command\n")

            // Safety Checks
            if (!ShellEngine.isAvailable()) {
                appendOutput("Error: Shizuku service is not available (binder not received).\n")
                _isRunning.value = false
                return@launch
            }

            if (!ShellEngine.checkPermission()) {
                appendOutput("Error: Shizuku permission denied.\n")
                _isRunning.value = false
                return@launch
            }

            val process = ShellEngine.runCommand(command)
            if (process == null) {
                appendOutput("Error: Failed to start process.\n")
                _isRunning.value = false
                return@launch
            }

            try {
                // Read stdout
                val stdoutJob = launch {
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        appendOutput(line + "\n")
                    }
                }

                // Read stderr
                val stderrJob = launch {
                    val reader = BufferedReader(InputStreamReader(process.errorStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        appendOutput("ERR: " + line + "\n")
                    }
                }

                process.waitFor()
                stdoutJob.join()
                stderrJob.join()
            } catch (e: Exception) {
                appendOutput("Exception: ${e.message}\n")
            } finally {
                _isRunning.value = false
                appendOutput("\n")
            }
        }
    }

    private suspend fun appendOutput(text: String?) {
        if (text == null) return
        mutex.withLock {
            _terminalOutput.value += text
        }
    }

    fun clearTerminal() {
        _terminalOutput.value = ""
    }
}
