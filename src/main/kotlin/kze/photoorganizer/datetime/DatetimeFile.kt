package kze.photoorganizer.datetime

import java.nio.file.Path
import java.time.LocalDateTime

data class DatetimeFile(
        val filePath: Path,
        val dateTime: LocalDateTime
)
