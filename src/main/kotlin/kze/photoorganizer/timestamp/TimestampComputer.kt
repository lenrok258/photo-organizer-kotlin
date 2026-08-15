package kze.photoorganizer.timestamp

import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.drew.metadata.avi.AviDirectory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.mov.QuickTimeDirectory
import com.drew.metadata.mp4.Mp4Directory
import kze.photoorganizer.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

data class FileMetadata(
    val path: Path,
    val localDateTime: LocalDateTime?,
    val timezoneOffset: ZoneOffset?,
    val utcForSorting: LocalDateTime
)

fun computeFilesWithTimestamps(filePaths: List<Path>, parameters: ProgramParameters): List<FileWithTimestamp> {

    val useExif = parameters.useEXIF()

    val timeOffsetInMinutes = parameters.timeOffsetInMinutes()

    // Faza 1: Wczytaj surowe metadane
    val metadata = filePaths.map { loadFileMetadata(it, useExif) }

    // Faza 2: Przetworz z kontekstem timezone
    return resolveTimestamps(metadata, timeOffsetInMinutes)
}

private fun loadFileMetadata(path: Path, useEXIF: Boolean): FileMetadata {
    if (!useEXIF) {
        return fromFileAttributes(path)
    }

    if (isVideoFile(path)) {
        return loadVideoMetadata(path)
    }

    return loadExifMetadata(path)
}

private fun loadExifMetadata(path: Path): FileMetadata {
    try {
        debug("Reading EXIF metadata for a file [$path]")
        val exifSubIFDirectory = ImageMetadataReader
                .readMetadata(path.toFile())
                .getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)

        if (exifSubIFDirectory == null) {
            warn("Unable to obtain exifSubIFDirectory from EXIF for a file [$path]")
            return fromFileAttributes(path)
        }

        val dateString = exifSubIFDirectory.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)
        if (dateString == null) {
            warn("Unable to obtain date from EXIF for a file [$path]")
            return fromFileAttributes(path)
        }

        val localDateTime = parseExifDateTime(dateString)
        if (localDateTime == null) {
            warn("Unable to parse EXIF date [$dateString] for a file [$path]")
            return fromFileAttributes(path)
        }

        val offsetString = exifSubIFDirectory.getString(ExifSubIFDDirectory.TAG_TIME_ZONE_ORIGINAL)
        val offset = if (offsetString != null) parseExifTimezoneOffset(offsetString) else null

        val utcForSorting = if (offset != null) {
            localDateTime.toInstant(offset).atZone(ZoneOffset.UTC).toLocalDateTime()
        } else {
            debug("No timezone offset in EXIF for a file [$path], using local time for sorting")
            localDateTime
        }

        debug("EXIF metadata for [$path]: local=$localDateTime, offset=$offsetString, utcForSorting=$utcForSorting")
        Statistics.datetimesFromEXIF++
        return FileMetadata(path, localDateTime, offset, utcForSorting)
    } catch (e: ImageProcessingException) {
        warn("Cannot obtain EXIF for [$path]")
        return fromFileAttributes(path)
    }
}

private fun loadVideoMetadata(path: Path): FileMetadata {
    try {
        debug("Reading video metadata for a file [$path]")
        val metadata = ImageMetadataReader.readMetadata(path.toFile())
        val format = getVideoFormat(path)

        val date = when (format) {
            "mp4" -> {
                val dir = metadata.getFirstDirectoryOfType(Mp4Directory::class.java)
                dir?.getDate(Mp4Directory.TAG_CREATION_TIME)
            }
            "mov" -> {
                val dir = metadata.getFirstDirectoryOfType(QuickTimeDirectory::class.java)
                dir?.getDate(QuickTimeDirectory.TAG_CREATION_TIME)
            }
            "avi" -> {
                val dir = metadata.getFirstDirectoryOfType(AviDirectory::class.java)
                dir?.getDate(AviDirectory.TAG_DATETIME_ORIGINAL)
            }
            "unsupported" -> {
                debug("Video format not supported for metadata extraction [$path], will use file attributes")
                null
            }
            else -> null
        }

        if (date != null) {
            val utcDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"))
            debug("Video metadata for [$path]: utc=$utcDateTime")
            Statistics.datetimesFromVideoMetadata++
            return FileMetadata(path, null, null, utcDateTime)
        }

        warn("Unable to obtain video metadata timestamp for a file [$path]")
        return fromFileAttributes(path)
    } catch (e: ImageProcessingException) {
        warn("Cannot obtain video metadata for [$path]")
        return fromFileAttributes(path)
    }
}

private fun fromFileAttributes(path: Path): FileMetadata {
    val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)
    val lastModifiedTime = attributes.lastModifiedTime()
    debug("Last Modified Time [$lastModifiedTime] obtained from file attributes for a file [$path]")
    val utcDateTime = LocalDateTime.ofInstant(lastModifiedTime.toInstant(), ZoneId.of("UTC"))
    Statistics.datetimesFromFileAttributes++
    return FileMetadata(path, utcDateTime, null, utcDateTime)
}

private fun resolveTimestamps(metadata: List<FileMetadata>, timeOffsetInMinutes: Int): List<FileWithTimestamp> {
    if (metadata.isEmpty()) return emptyList()

    // Sortuj wg utcForSorting
    val sorted = metadata.sortedBy { it.utcForSorting }

    // Backward scan: ustal offset dla video przed pierwszym zdjęciem z offsetem
    val resolvedOffsets = resolveOffsetsForVideos(sorted)

    // Iteracja z lastKnownOffset
    var lastKnownOffset: ZoneOffset? = null
    val result = mutableListOf<FileWithTimestamp>()

    for ((index, fileMetadata) in sorted.withIndex()) {
        val resolvedOffset = resolvedOffsets[index] ?: lastKnownOffset
        val timestamp = resolveTimestamp(fileMetadata, resolvedOffset, timeOffsetInMinutes)
        result.add(FileWithTimestamp(fileMetadata.path, timestamp))

        if (fileMetadata.timezoneOffset != null) {
            lastKnownOffset = fileMetadata.timezoneOffset
        }
    }

    return result
}

private fun resolveOffsetsForVideos(sorted: List<FileMetadata>): Array<ZoneOffset?> {
    val offsets = arrayOfNulls<ZoneOffset>(sorted.size)

    // Znajdz indeksy video bez offsetu
    val videoIndices = sorted.indices.filter { sorted[it].timezoneOffset == null && isVideoFile(sorted[it].path) }

    for (videoIndex in videoIndices) {
        // Szukaj do przodu za najbliższym zdjęciem z offsetem
        for (j in (videoIndex + 1) until sorted.size) {
            if (sorted[j].timezoneOffset != null) {
                offsets[videoIndex] = sorted[j].timezoneOffset
                break
            }
        }
    }

    return offsets
}

private fun resolveTimestamp(
    fileMetadata: FileMetadata,
    offset: ZoneOffset?,
    timeOffsetInMinutes: Int
): LocalDateTime {
    val baseTimestamp = when {
        // Zdjęcie: użyj czasu lokalnego (z offsetem lub bez)
        fileMetadata.localDateTime != null -> fileMetadata.localDateTime
        // Video z offsetem: konwertuj UTC na local
        offset != null -> fileMetadata.utcForSorting.toInstant(ZoneOffset.UTC).atZone(offset).toLocalDateTime()
        // Video bez offsetu: użyj UTC
        else -> fileMetadata.utcForSorting
    }

    return applyTimeOffset(baseTimestamp, timeOffsetInMinutes)
}

private fun parseExifDateTime(dateString: String): LocalDateTime? {
    val patterns = listOf(
        "yyyy:MM:dd HH:mm:ss",
        "yyyy:MM:dd HH:mm",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm"
    )
    for (pattern in patterns) {
        try {
            val formatter = java.time.format.DateTimeFormatter.ofPattern(pattern)
            return LocalDateTime.parse(dateString, formatter)
        } catch (e: Exception) {
            // try next pattern
        }
    }
    return null
}

private fun parseExifTimezoneOffset(offsetString: String): ZoneOffset? {
    try {
        return ZoneOffset.of(offsetString)
    } catch (e: Exception) {
        return null
    }
}

private val VIDEO_EXTENSIONS_MP4 = setOf("mp4", "m4v", "3gp", "3g2")
private val VIDEO_EXTENSIONS_MOV = setOf("mov")
private val VIDEO_EXTENSIONS_AVI = setOf("avi")
private val VIDEO_EXTENSIONS_WITHOUT_METADATA = setOf(
    "mkv", "webm", "wmv", "asf", "flv", "mpg", "mpeg", "m2ts", "mts", "ts", "vob", "ogv", "rm", "rmvb"
)
private val ALL_VIDEO_EXTENSIONS = VIDEO_EXTENSIONS_MP4 + VIDEO_EXTENSIONS_MOV + VIDEO_EXTENSIONS_AVI + VIDEO_EXTENSIONS_WITHOUT_METADATA

private fun isVideoFile(path: Path): Boolean {
    val ext = path.toString().substringAfterLast('.').lowercase()
    return ext in ALL_VIDEO_EXTENSIONS
}

private fun getVideoFormat(path: Path): String {
    val ext = path.toString().substringAfterLast('.').lowercase()
    return when {
        ext in VIDEO_EXTENSIONS_MP4 -> "mp4"
        ext in VIDEO_EXTENSIONS_MOV -> "mov"
        ext in VIDEO_EXTENSIONS_AVI -> "avi"
        else -> "unsupported"
    }
}

private fun applyTimeOffset(datetime: LocalDateTime, timeOffsetInMinutes: Int): LocalDateTime {
    var timeWithOffset = datetime
    if (timeOffsetInMinutes != 0) {
        debug("Applying requested time offset in minutes: $timeOffsetInMinutes")
        timeWithOffset = datetime.plusMinutes(timeOffsetInMinutes.toLong())
        debug("Time before without offset: $datetime, time with offset: $timeWithOffset")
    }
    return timeWithOffset
}
