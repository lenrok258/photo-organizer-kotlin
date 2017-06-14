package kze.photoorganizer.timestamp

import java.nio.file.Path
import java.time.LocalDateTime

data class FileWithTimestamp(
        val filePath: Path,
        val dateTime: LocalDateTime
)
