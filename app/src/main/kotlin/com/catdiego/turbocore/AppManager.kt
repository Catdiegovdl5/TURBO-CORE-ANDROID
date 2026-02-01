package com.catdiego.turbocore

import android.content.Context
import android.app.ActivityManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: ImageBitmap?,
    val isSystemApp: Boolean
)

fun Drawable.toOptimalImageBitmap(): ImageBitmap {
    val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    this.setBounds(0, 0, 128, 128)
    this.draw(canvas)
    return bitmap.asImageBitmap()
}

object AppManager {

    suspend fun runCommand(command: String): Boolean {
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
                    val output = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
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

    fun getRamUsage(context: Context): Float {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)

        val totalMem = memoryInfo.totalMem.toFloat()
        val availMem = memoryInfo.availMem.toFloat()
        val usedMem = totalMem - availMem

        return usedMem / totalMem
    }

    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val apps = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
        } else {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        }

        // Filtering system apps to save RAM as requested
        return apps.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map { app ->
                AppInfo(
                    name = app.loadLabel(pm).toString(),
                    packageName = app.packageName,
                    icon = app.loadIcon(pm).toOptimalImageBitmap(),
                    isSystemApp = false
                )
            }
    }

    suspend fun resetEverything(): Boolean {
        val commands = listOf(
            "wm size reset",
            "wm density reset",
            "settings put global window_animation_scale 1.0",
            "settings put global transition_animation_scale 1.0",
            "settings put global animator_duration_scale 1.0"
        )
        return runCommands(commands)
    }
}
