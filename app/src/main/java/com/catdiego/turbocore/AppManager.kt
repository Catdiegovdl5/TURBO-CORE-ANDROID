package com.catdiego.turbocore

import android.content.Context
import android.app.ActivityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppManager {

    fun getRamUsage(context: Context): Float {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.getMemoryInfo(memoryInfo)

        val totalMem = memoryInfo.totalMem.toFloat()
        val availMem = memoryInfo.availMem.toFloat()
        val usedMem = totalMem - availMem

        return usedMem / totalMem
    }

    suspend fun applyGoldenRatioResolution(context: Context, width: Int = 720): Boolean {
        return withContext(Dispatchers.IO) {
            val metrics = context.resources.displayMetrics
            val originalWidth = metrics.widthPixels
            val originalHeight = metrics.heightPixels

            val ratio = originalHeight.toFloat() / originalWidth.toFloat()
            val newHeight = (width * ratio).toInt()

            // Apply resolution using Shizuku
            ShellEngine.runCommand("wm size ${width}x${newHeight}")
        }
    }

    suspend fun resetEverything(): Boolean {
        val commands = listOf(
            "wm size reset",
            "wm density reset",
            "settings put global window_animation_scale 1.0",
            "settings put global transition_animation_scale 1.0",
            "settings put global animator_duration_scale 1.0",
            "cmd package compile -m speed-profile -a" // 128px icon/optimization implied logic
        )
        return ShellEngine.runCommands(commands)
    }

    // New Features Placeholders
    suspend fun enableSuperEconomy(): Boolean {
        return ShellEngine.runCommand("settings put global low_power 1")
    }

    suspend fun enableUltraEconomy(): Boolean {
        val commands = listOf(
            "settings put global low_power 1",
            "cmd power set-mode 1"
        )
        return ShellEngine.runCommands(commands)
    }

    suspend fun enableGamerUltimate(): Boolean {
        val commands = listOf(
            "cmd package compile -m speed -a",
            "settings put global window_animation_scale 0.0",
            "settings put global transition_animation_scale 0.0",
            "settings put global animator_duration_scale 0.0"
        )
        return ShellEngine.runCommands(commands)
    }

    suspend fun enableSensiFreeFire(): Boolean {
        return ShellEngine.runCommand("settings put system pointer_speed 7")
    }
}
