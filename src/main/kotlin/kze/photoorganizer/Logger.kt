package kze.photoorganizer

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

private enum class LogLevel {
    INFO, ERROR
}

fun info(message: String) {
    val messageFormatted = formatMessage(LogLevel.INFO, message)
    println(messageFormatted)
}

fun info(message: String, vararg args: Any) {
    val messageFormatted = formatMessage(LogLevel.INFO, message, args)
    println(messageFormatted)
}

fun error(message: String) {
    val messageFormatted = formatMessage(LogLevel.ERROR, message)
    println(messageFormatted)
}

private fun formatMessage(level: LogLevel, message: String, args: Array<out Any> = emptyArray()): String {
    val nowString = LocalDateTime.now().format(DATE_FORMATTER)
    val messageFormatted = message.format(args)
    return "[$nowString] [$level] $messageFormatted";
}
