package com.catdiego.turbocore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import rikka.shizuku.Shizuku

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

            val outputDeferred = async { process.inputStream.bufferedReader().use { it.readText() } }
            val errorDeferred = async { process.errorStream.bufferedReader().use { it.readText() } }

            val output = outputDeferred.await()
            val error = errorDeferred.await()
            process.waitFor()

            if (error.isNotEmpty()) "$output\nErro: $error".trim()
            else if (output.isEmpty()) "Sucesso"
            else output.trim()
        } catch (e: Exception) { "Erro: ${e.message}" }
    }
}
