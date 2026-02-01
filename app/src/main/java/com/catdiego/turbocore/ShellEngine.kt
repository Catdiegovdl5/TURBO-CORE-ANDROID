package com.catdiego.turbocore

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object ShellEngine {

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    suspend fun runCommand(command: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Mandatory 2000ms delay for MediaTek stability
                delay(2000)

                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    // Use Reflection to access private newProcess as per PR 1 architecture
                    val method = Shizuku::class.java.getDeclaredMethod(
                        "newProcess",
                        Array<String>::class.java,
                        Array<String>::class.java,
                        String::class.java
                    )
                    method.isAccessible = true
                    val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
                    val exitCode = process.waitFor()
                    return@withContext exitCode == 0
                } else {
                    return@withContext false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false
            }
        }
    }

    suspend fun runCommands(commands: List<String>): Boolean {
         return withContext(Dispatchers.IO) {
            try {
                delay(2000)

                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    val script = commands.joinToString("\n")
                    val method = Shizuku::class.java.getDeclaredMethod(
                        "newProcess",
                        Array<String>::class.java,
                        Array<String>::class.java,
                        String::class.java
                    )
                    method.isAccessible = true
                    val process = method.invoke(null, arrayOf("sh", "-c", script), null, null) as Process
                    val exitCode = process.waitFor()
                    return@withContext exitCode == 0
                } else {
                    return@withContext false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false
            }
        }
    }

    suspend fun runCommandWithOutput(command: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // Mandatory 2000ms delay for MediaTek stability
                delay(2000)

                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    val method = Shizuku::class.java.getDeclaredMethod(
                        "newProcess",
                        Array<String>::class.java,
                        Array<String>::class.java,
                        String::class.java
                    )
                    method.isAccessible = true
                    val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process

                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                    val output = StringBuilder()
                    var line: String?

                    // Read StdOut
                    while (reader.readLine().also { line = it } != null) {
                        output.append(line).append("\n")
                    }

                    // Read StdErr
                    while (errorReader.readLine().also { line = it } != null) {
                        output.append(line).append("\n")
                    }

                    process.waitFor()
                    return@withContext output.toString()
                } else {
                    return@withContext "Erro: Permissão Shizuku negada."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext "Erro: ${e.message}"
            }
        }
    }
}
