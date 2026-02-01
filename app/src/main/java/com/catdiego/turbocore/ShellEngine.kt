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
                    // Fallback to su if Shizuku is not available (though we prioritize Shizuku)
                    // Or simply fail if Shizuku is required by policy.
                    // Given the instruction "Standardize on Shizuku", we will rely on it.
                    // However, for robustness, if we can't use Shizuku, we return false.
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
                // Delay once per batch or per command? "Boot delay" usually implies initialization,
                // but "boot delay for MediaTek stability" in execution context might mean pacing.
                // We will add the delay at the start of the batch.
                delay(2000)

                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    // Running as a single script is often better
                    val script = commands.joinToString("\n")
                    // Use Reflection
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
}
