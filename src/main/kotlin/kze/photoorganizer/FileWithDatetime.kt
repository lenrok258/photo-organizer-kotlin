package kze.photoorganizer

import java.nio.file.Path
import java.time.LocalDateTime

data class FileWithDatetime(val filePath: Path, val dateTime: LocalDateTime)
