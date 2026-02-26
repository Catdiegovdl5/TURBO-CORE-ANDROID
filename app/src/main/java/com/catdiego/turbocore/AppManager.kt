package com.catdiego.turbocore

import rikka.shizuku.Shizuku
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

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

            coroutineScope {
                val outputDeferred = async {
                    process.inputStream.bufferedReader().use { it.readText() }
                }
                val errorDeferred = async {
                    process.errorStream.bufferedReader().use { it.readText() }
                }

                val exitCode = process.waitFor()
                val output = outputDeferred.await()
                val error = errorDeferred.await()

                if (exitCode == 0 && output.isEmpty() && error.isEmpty()) "Sucesso"
                else if (output.isNotEmpty()) output
                else if (error.isNotEmpty()) "Erro: $error"
                else "Código de saída: $exitCode"
            }
        } catch (e: Exception) {
            "Erro: ${e.message}"
        }
    }
}
