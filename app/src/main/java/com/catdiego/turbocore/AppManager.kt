package com.catdiego.turbocore

import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

object AppManager {
    fun runCommand(command: String): String {
        if (!Shizuku.pingBinder()) return "Erro: Serviço Shizuku parado no sistema!"
        return try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java, Array<String>::class.java, String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            process.waitFor()
            if (output.isEmpty()) "Sucesso" else output
        } catch (e: Exception) { "Erro: ${e.message}" }
    }
}
