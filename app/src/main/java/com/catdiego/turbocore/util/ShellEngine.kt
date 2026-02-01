package com.catdiego.turbocore.util

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.File
import java.lang.reflect.Method

object ShellEngine {

    /**
     * Checks if Shizuku service is available.
     * Wrapped in try-catch for safety.
     */
    fun isAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Checks if the app has permission to use Shizuku.
     */
    fun checkPermission(): Boolean {
        return try {
            if (Shizuku.isPreV11()) {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Executes a command using Shizuku.
     * Uses Reflection as a fallback if direct access fails (or if library hides it).
     */
    fun newProcess(command: Array<String>, env: Array<String>? = null, dir: File? = null): Process? {
        return try {
            // Attempting direct access via Reflection to ensure compatibility
            val clazz = Class.forName("rikka.shizuku.Shizuku")
            val method: Method = clazz.getMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.invoke(null, command, env, dir?.absolutePath) as? Process
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Runs a command with "sh -c" prefix.
     */
    fun runCommand(command: String): Process? {
        // Enforce sh -c for complex commands (wm, settings, etc)
        return newProcess(arrayOf("sh", "-c", command))
    }

    /**
     * Runs a command with UI feedback (Toast).
     */
    suspend fun runCommandWithFeedback(context: Context, command: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Enviando: $command", Toast.LENGTH_SHORT).show()
        }

        val process = withContext(Dispatchers.IO) {
            runCommand(command)
        }

        withContext(Dispatchers.Main) {
            if (process != null) {
                // Wait for process in background to get exit code?
                // Getting exit code here might block main thread if we wait immediately.
                // Better to just say "Command Sent" or "Process Started".
                // But the user wants "Sucesso" or "Erro".
                // So we should wait in IO context.
            } else {
                Toast.makeText(context, "Erro: Falha ao iniciar processo Shizuku", Toast.LENGTH_LONG).show()
            }
        }

        if (process != null) {
            val exitCode = withContext(Dispatchers.IO) {
                try {
                    process.waitFor()
                } catch (e: InterruptedException) {
                    -1
                }
            }
            withContext(Dispatchers.Main) {
                if (exitCode == 0) {
                    Toast.makeText(context, "Sucesso ($command)", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Erro: Código de saída $exitCode", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Alias for runCommandWithFeedback to be used by UI components via runShizukuCommand logic if needed,
     * but ActionButtons.kt uses runCommandWithFeedback directly.
     * Keeping this for compatibility if referenced elsewhere.
     */
    fun runShizukuCommand(command: String): Process? {
        return runCommand(command)
    }
}
