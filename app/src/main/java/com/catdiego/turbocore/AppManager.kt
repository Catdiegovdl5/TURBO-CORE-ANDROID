package com.catdiego.turbocore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream

object AppManager {

    suspend fun executeResetCommands(): Boolean {
        return withContext(Dispatchers.IO) {
            val commands = listOf(
                "wm size reset",
                "wm density reset",
                "settings put global window_animation_scale 1.0",
                "settings put global transition_animation_scale 1.0",
                "settings put global animator_duration_scale 1.0"
            )

            try {
                val process = Runtime.getRuntime().exec("su")
                val os = DataOutputStream(process.outputStream)
                for (cmd in commands) {
                    os.writeBytes(cmd + "\n")
                }
                os.writeBytes("exit\n")
                os.flush()
                os.close()
                val exitCode = process.waitFor()
                return@withContext exitCode == 0
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false
            }
        }
    }
}
