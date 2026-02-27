package com.catdiego.turbocore

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object AppManager {
    suspend fun runCommand(command: String): String = withContext(Dispatchers.IO) {
        if (!Shizuku.pingBinder()) return@withContext "Erro: Serviço Shizuku parado no sistema!"
        try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java, Array<String>::class.java, String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process

            val output = coroutineScope {
                val stdout = async {
                    process.inputStream.bufferedReader().use { it.readText() }
                }
                val stderr = async {
                    process.errorStream.bufferedReader().use { it.readText() }
                }
                stdout.await() + stderr.await()
            }

            process.waitFor()
            if (output.isEmpty()) "Sucesso" else output
        } catch (e: Exception) { "Erro: ${e.message}" }
    }

}
