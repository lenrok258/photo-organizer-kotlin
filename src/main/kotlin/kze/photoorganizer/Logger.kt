package kze.photoorganizer

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

private enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

fun debug(message: String, vararg args: Any) {
    logMessage(LogLevel.DEBUG, message, *args)
}

fun info(message: String, vararg args: Any) {
    logMessage(LogLevel.INFO, message, *args)
}

fun warn(message: String, vararg args: Any) {
    logMessage(LogLevel.WARN, message, *args)
}

fun error(message: String, vararg args: Any) {
    logMessage(LogLevel.ERROR, message, *args)
}

private fun logMessage(level: LogLevel, message: String, vararg args: Any) {
    val messageFormatted = formatMessage(level, message, *args)
    println(messageFormatted)
}

private fun formatMessage(level: LogLevel, message: String, vararg args: Any): String {
    val nowString = LocalDateTime.now().format(DATE_FORMATTER)
    val messageFormatted = message.format(*args)
    return "[$nowString] [$level] $messageFormatted"
}
