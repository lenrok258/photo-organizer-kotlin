package kze.photoorganizer.datetime

import java.nio.file.Path
import java.time.LocalDateTime

data class DatetimeFile(val filePath: java.nio.file.Path, val dateTime: java.time.LocalDateTime)
