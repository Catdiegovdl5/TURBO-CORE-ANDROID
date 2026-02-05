package com.catdiego.turbocore

import android.os.Build
import android.util.Log
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object ShellEngine {

    suspend fun getPid(packageName: String): String? = withContext(Dispatchers.IO) {
        val output = runCommand("pidof $packageName")
        if (output.contains("Erro") || output == "Sucesso" || output.isEmpty()) null
        else output.trim().split(" ").firstOrNull()
    }

    suspend fun runCommand(command: String): String = withContext(Dispatchers.IO) {
        if (!Shizuku.pingBinder()) return@withContext "Erro: Serviço Shizuku parado no sistema!"
        try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java, Array<String>::class.java, String::class.java
            )
            method.isAccessible = true
            // Redirect stderr to stdout to capture error messages
            val process = method.invoke(null, arrayOf("sh", "-c", "$command 2>&1"), null, null) as Process
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            process.waitFor()
            if (output.isEmpty()) "Sucesso" else output
        } catch (e: Exception) {
            "Erro: ${e.message}"
        }
    }

    suspend fun findTouchIrq(): String? = withContext(Dispatchers.IO) {
        try {
            val output = runCommand("cat /proc/interrupts")
            val lines = output.split("\n")
            for (line in lines) {
                if (line.contains("mtk-tpd", ignoreCase = true) || line.contains("touchscreen", ignoreCase = true)) {
                    // Format: " 123: ..." -> extract 123
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.isNotEmpty()) {
                        return@withContext parts[0].replace(":", "")
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun applyRankXi(): String {
        val sb = StringBuilder()

        // 1. Atomic Memory Compaction
        sb.append(runCommand("echo 1 > /proc/sys/vm/compact_memory")).append("\n")
        delay(2000)

        // 2. Thermal Bypass
        val thermalCommands = listOf(
            "stop thermal-engine",
            "stop thermal_manager",
            "stop thermald",
            "pm disable-user com.samsung.android.thermal"
        )
        for (cmd in thermalCommands) {
            sb.append(runCommand(cmd)).append("\n")
            delay(2000)
        }

        // 3. IRQ Affinity
        val irq = findTouchIrq()
        if (irq != null) {
            val result = runCommand("echo 1 > /proc/irq/$irq/smp_affinity")
            delay(2000)
            if (result.contains("Permission denied") || result.contains("Erro")) {
                sb.append("Ξ: IRQ Access Denied\n")
            } else {
                sb.append("IRQ $irq moved to Core 0\n")
            }
        } else {
            sb.append("Ξ: Touch IRQ not found\n")
        }

        return sb.toString()
    }

    suspend fun applyProfile(profile: Profile): String {
        // Basic implementation for other profiles based on memory/context
        return when (profile) {
            is Profile.Eco -> {
                val res = runCommand("settings put global low_power 1 && pm suspend com.google.android.gms")
                delay(2000)
                res
            }
            is Profile.Balanced -> {
                val res = runCommand("wm size reset && wm density reset && settings put global low_power 0")
                delay(2000)
                res
            }
            is Profile.Turbo -> {
                val res = runCommand("cmd activity kill-all && echo 10 > /proc/sys/vm/swappiness")
                delay(2000)
                res
            }
            is Profile.Sacrifice -> applyRankXi()
        }
    }
}
