package kze.photoorganizer.datetime

import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.drew.metadata.exif.ExifSubIFDDirectory
import kze.photoorganizer.debug
import kze.photoorganizer.warn
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId


fun computeDatetimeFile(path: Path): DatetimeFile {
    val datetime = obtainDatetimeFromEXIF(path) ?: obtainDatetimeFromFile(path)
    return DatetimeFile(path, datetime)
}

private fun obtainDatetimeFromEXIF(path: Path): LocalDateTime? {
    try {
        debug("Reading EXIF datetime for a file [$path]")
        val metadata = ImageMetadataReader.readMetadata(path.toFile())
        val exifSubIFDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
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
        debug("EXIF datetime [$dateTime] for a file [$path]")
        return dateTime
    } catch (e: ImageProcessingException) {
        warn("Cannot obtain EXIF for [$path]")
        return null
    }
}

private fun obtainDatetimeFromFile(path: Path): LocalDateTime {
    val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)
    val creationTime = attributes.creationTime()
    debug("Creation datetime [$creationTime] obtained from file attributes for a file [$path]")
    return toLocalDatetime(creationTime.toInstant())
}

private fun toLocalDatetime(instant: Instant): LocalDateTime {
    return LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
}

