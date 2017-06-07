package kze.photoorganizer

private enum class LogLevel {
    INFO, ERROR
}

fun info(message: String) {
    val messageFormatted = formatMessage(LogLevel.INFO, message)
    println(messageFormatted)
}

fun error(message: String) {
    val messageFormatted = formatMessage(LogLevel.ERROR, message)
    println(messageFormatted)
}

private fun formatMessage(level: LogLevel, message: String): String {
    return "[date] [$level] $message";
}
