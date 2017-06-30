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

fun computeFilesWithTimestamps(listFilesPaths: List<Path>, useEXIF: Boolean): List<FileWithTimestamp> {
    return listFilesPaths
            .map { computeFileWithTimestamp(it, useEXIF) }
}

private fun computeFileWithTimestamp(path: Path, useEXIF: Boolean): FileWithTimestamp {
    val datetime = if (useEXIF) {
        fromEXIF(path) ?: fromFileAttributes(path)
    } else {
        fromFileAttributes(path)
    }
    return FileWithTimestamp(path, datetime)
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

        val dateTime = toLocalDatetime(date.toInstant());
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

