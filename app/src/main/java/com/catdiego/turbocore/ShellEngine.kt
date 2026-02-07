package com.catdiego.turbocore

import android.os.Build
import android.util.Log
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.*
import com.catdiego.turbocore.util.AppLogger

object ShellEngine {
    private var job: Job? = null

    // 1. PID Retriever
    suspend fun getPid(packageName: String): String? = withContext(Dispatchers.IO) {
        val output = runCommand("pidof -s $packageName")
        if (output.any { it.isDigit() }) output.trim() else null
    }

    // 2. CPU Delta Parser
    suspend fun getCpuRawStats(): Pair<Long, Long> = withContext(Dispatchers.IO) {
        try {
            val output = runCommand("cat /proc/stat | head -n 1")
            val p = output.trim().split("\\s+".toRegex())
            if (p.size >= 8) {
                // user + nice + system + irq + softirq
                val active = p[1].toLong() + p[2].toLong() + p[3].toLong() + p[6].toLong() + p[7].toLong()
                // active + idle + iowait
                val total = active + p[4].toLong() + p[5].toLong()
                return@withContext Pair(active, total)
            }
        } catch (e: Exception) {
            AppLogger.e("ShellEngine", "Failed to parse CPU stats", e)
        }
        Pair(0L, 0L)
    }

    // 3. Executor com Timeout e Logging
    suspend fun runCommand(command: String): String = withContext(Dispatchers.IO) {
        if (!Shizuku.pingBinder()) {
            AppLogger.e("ShellEngine", "Shizuku offline during command: $command")
            return@withContext "Erro: Shizuku Offline"
        }
        try {
            AppLogger.d("ShellEngine", "Exec: $command")
            withTimeout(3000L) {
                val method = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java, Array<String>::class.java, String::class.java
                )
                method.isAccessible = true
                val process = method.invoke(null, arrayOf("sh", "-c", "$command 2>&1"), null, null) as Process
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = reader.readText()
                process.waitFor()

                if (output.isNotEmpty()) {
                    AppLogger.d("ShellEngine", "Output: $output")
                } else {
                    AppLogger.s("ShellEngine", "Command success (no output)")
                }

                if (output.isEmpty()) "Sucesso" else output
            }
        } catch (e: TimeoutCancellationException) {
            AppLogger.e("ShellEngine", "Timeout on command: $command")
            "Erro: Timeout (Comando demorou demais)"
        } catch (e: Exception) {
            AppLogger.e("ShellEngine", "Exception on command: $command", e)
            "Erro: ${e.message}"
        }
    }

    suspend fun findTouchIrq(): String? = withContext(Dispatchers.IO) {
        try {
            val output = runCommand("cat /proc/interrupts")
            val lines = output.split("\n")
            for (line in lines) {
                if (line.contains("mtk-tpd", ignoreCase = true) || line.contains("touchscreen", ignoreCase = true)) {
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.isNotEmpty()) {
                        return@withContext parts[0].replace(":", "")
                    }
                }
            }
            null
        } catch (e: Exception) {
            AppLogger.e("ShellEngine", "Failed to find IRQ", e)
            null
        }
    }

    suspend fun getThermalTemp(): Float = withContext(Dispatchers.IO) {
        try {
            val raw = runCommand("cat /sys/class/thermal/thermal_zone0/temp")
            val temp = raw.trim().toIntOrNull() ?: 0
            return@withContext temp / 1000f
        } catch (e: Exception) {
            AppLogger.e("ShellEngine", "Failed to read temp", e)
            0f
        }
    }

    suspend fun getManufacturer(): String = withContext(Dispatchers.IO) {
        runCommand("getprop ro.product.manufacturer").trim()
    }

    suspend fun getTotalRam(): Long = withContext(Dispatchers.IO) {
        try {
            val output = runCommand("grep MemTotal /proc/meminfo")
            val parts = output.split("\\s+".toRegex())
            if (parts.size >= 2) parts[1].toLongOrNull() ?: 0L else 0L
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun bypassKnox(): String {
        return runCommand("pm disable-user com.samsung.android.knox.analytics.uploader")
    }

    suspend fun disableJoyose(): String {
        return runCommand("pm disable-user com.xiaomi.joyose")
    }

    suspend fun applyThreadPinning(pid: String): String {
        return runCommand("taskset -p f0 $pid")
    }

    suspend fun applyRankXi(pid: String?): String {
        val sb = StringBuilder()
        // Herda otimizações do Rank Omega (Pinning, JIT, Fsync)
        sb.append(applyRankOmega(pid))

        sb.append(runCommand("echo 1 > /proc/sys/vm/compact_memory")).append("\n")
        val thermalCommands = listOf(
            "stop thermal-engine",
            "stop thermal_manager",
            "stop thermald",
            "pm disable-user com.samsung.android.thermal"
        )
        for (cmd in thermalCommands) {
            sb.append(runCommand(cmd)).append("\n")
        }
        val irq = findTouchIrq()
        if (irq != null) {
            val result = runCommand("echo 1 > /proc/irq/$irq/smp_affinity")
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

    suspend fun applyRankOmega(pid: String?): String {
        val sb = StringBuilder()
        if (pid != null) {
            sb.append(applyThreadPinning(pid)).append("\n")
            sb.append("Omega: App Pinned to Big Cores\n")
        } else {
            sb.append("Omega: App not found for pinning\n")
        }
        sb.append(runCommand("echo 0 > /sys/module/sync/parameters/fsync_enabled"))
        return sb.toString()
    }

    suspend fun applyRankSSS(): String {
        val sb = StringBuilder()
        sb.append(runCommand("echo 2048 > /sys/block/mmcblk0/queue/read_ahead_kb")).append("\n")
        val gpuPaths = listOf(
            "/sys/class/devfreq/18500000.mali/governor",
            "/sys/class/kgsl/kgsl-3d0/devfreq/governor"
        )
        for (path in gpuPaths) {
            sb.append(runCommand("echo performance > $path")).append("\n")
        }
        sb.append(runCommand("stop logd"))
        return sb.toString()
    }

    suspend fun applyRankS(): String {
        return runCommand("echo 10 > /proc/sys/vm/swappiness")
    }

    suspend fun applySensiFF(): String {
        val sb = StringBuilder()
        sb.append(runCommand("wm density 180")).append("\n")
        val irq = findTouchIrq()
        if (irq != null) {
            val result = runCommand("echo 1 > /proc/irq/$irq/smp_affinity")
            sb.append(if(result.contains("Erro")) "Sensi: IRQ Fail" else "Sensi: IRQ -> Core 0")
        }
        return sb.toString()
    }

    suspend fun applyProfile(profile: Profile, targetPid: String? = null): String {
        AppLogger.i("ShellEngine", "Applying profile: ${profile.name}")
        return when (profile) {
            is Profile.Eco -> runCommand("settings put global low_power 1 && pm suspend com.google.android.gms")
            is Profile.RankS -> applyRankS()
            is Profile.RankSSS -> applyRankSSS()
            is Profile.RankOmega -> applyRankOmega(targetPid)
            is Profile.RankXi -> applyRankXi(targetPid)
            is Profile.SensiFF -> applySensiFF()
            is Profile.Balanced -> runCommand("wm size reset && wm density reset && settings put global low_power 0")
            else -> "Perfil Desconhecido"
        }
    }
}
