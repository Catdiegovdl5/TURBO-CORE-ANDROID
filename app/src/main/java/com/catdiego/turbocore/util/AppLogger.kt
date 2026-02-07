package com.catdiego.turbocore.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR, SUCCESS
}

data class LogEntry(
    val timestamp: String,
    val tag: String,
    val message: String,
    val level: LogLevel
)

object AppLogger {
    private val _logFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logFlow = _logFlow.asStateFlow()

    private const val MAX_LOGS = 300
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun log(tag: String, message: String, level: LogLevel = LogLevel.INFO) {
        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            tag = tag,
            message = message,
            level = level
        )
        _logFlow.update { currentList ->
            // Adiciona no final, mas limita o tamanho removendo do início
            val newList = currentList + entry
            if (newList.size > MAX_LOGS) {
                newList.drop(newList.size - MAX_LOGS)
            } else {
                newList
            }
        }
    }

    fun d(tag: String, message: String) = log(tag, message, LogLevel.DEBUG)
    fun i(tag: String, message: String) = log(tag, message, LogLevel.INFO)
    fun w(tag: String, message: String) = log(tag, message, LogLevel.WARN)
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val msg = if (throwable != null) "$message\nStack: ${throwable.stackTraceToString()}" else message
        log(tag, msg, LogLevel.ERROR)
    }
    fun s(tag: String, message: String) = log(tag, message, LogLevel.SUCCESS)
}
