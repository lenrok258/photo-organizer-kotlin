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

    // To remember: collision detection by computing hashes

    var datetime = obtainDatetimeFromEXIF(path)
    if (datetime == null) {
        datetime = obtainDatetimeFromFile(path)
    }

    return DatetimeFile(path, datetime)
}

private fun obtainDatetimeFromEXIF(path: Path): LocalDateTime? {
    try {
        val metadata = ImageMetadataReader.readMetadata(path.toFile())
        val exifSubIFDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
        val date = exifSubIFDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)
        debug("EXIF datetime [$date] for file [$path]")
        return toLocalDatetime(date.toInstant());
    } catch (e: ImageProcessingException) {
        warn("Cannot obtain EXIF for [$path]")
        return null
    }
}

private fun obtainDatetimeFromFile(path: Path): LocalDateTime {
    val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)
    val creationTime = attributes.creationTime()
    debug("Creation datetime [$creationTime] obtained from file attributes for file [$path]")
    return toLocalDatetime(creationTime.toInstant())
}

private fun toLocalDatetime(instant: Instant): LocalDateTime {
    return LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
}

