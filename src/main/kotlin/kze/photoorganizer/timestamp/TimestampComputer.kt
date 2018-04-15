package kze.photoorganizer.timestamp

import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.drew.metadata.exif.ExifSubIFDDirectory
import kze.photoorganizer.Statistics
import kze.photoorganizer.debug
import kze.photoorganizer.warn
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

fun computeFilesWithTimestamps(listFilesPaths: List<Path>, useEXIF: Boolean, timeOffsetInMinutes: Int): List<FileWithTimestamp> {
    return listFilesPaths
            .map { computeFileWithTimestamp(it, useEXIF, timeOffsetInMinutes) }
}

private fun computeFileWithTimestamp(path: Path, useEXIF: Boolean, timeOffsetInMinutes: Int): FileWithTimestamp {
    val datetime = if (useEXIF) {
        fromEXIF(path) ?: fromFileAttributes(path)
    } else {
        fromFileAttributes(path)
    }
    var datetimeWithOffset = applyTimeOffset(datetime, timeOffsetInMinutes)
    return FileWithTimestamp(path, datetimeWithOffset)
}

private fun fromEXIF(path: Path): LocalDateTime? {
    try {
        debug("Reading EXIF timestamp for a file [$path]")
        val exifSubIFDirectory = ImageMetadataReader
                .readMetadata(path.toFile())
                .getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)

        if (exifSubIFDirectory == null) {
            warn("Unable to obtain exifSubIFDirectory from EXIF for a file [$path]")
            return null
        }

        val date = exifSubIFDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)
        if (date == null) {
            warn("Unable to obtain date from EXIF for a file [$path]")
            return null
        }

        val dateTime = toLocalDatetime(date);
        debug("EXIF timestamp [$dateTime] for a file [$path]")
        Statistics.datetimesFromEXIF++
        return dateTime
    } catch (e: ImageProcessingException) {
        warn("Cannot obtain EXIF for [$path]")
        return null
    }
}

private fun fromFileAttributes(path: Path): LocalDateTime {
    val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)
    val creationTime = attributes.creationTime()
    debug("Creation timestamp [$creationTime] obtained from file attributes for a file [$path]")
    Statistics.datetimesFromFileAttributes++
    return toLocalDatetime(creationTime.toInstant())
}

private fun toLocalDatetime(instant: Instant): LocalDateTime {
    return LocalDateTime.ofInstant(instant, ZoneId.of("UTC"))
}

@Suppress("DEPRECATION")
private fun toLocalDatetime(date: Date): LocalDateTime {
    val zoneOffset = ZoneOffset.ofTotalSeconds(date.timezoneOffset * 60)
    val zoneId = ZoneId.ofOffset("", zoneOffset)
    debug("Time zone: $zoneId, zone offset: ${zoneOffset.totalSeconds}")
    return LocalDateTime.ofInstant(date.toInstant(), zoneId)
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

