package kze.photoorganizer.timestamp

import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.drew.metadata.exif.ExifSubIFDDirectory
import kze.photoorganizer.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.Optional.empty
import kotlin.io.nameWithoutExtension

fun computeFilesWithTimestamps(filePaths: List<Path>, parameters: ProgramParameters): List<FileWithTimestamp> {

    // TODO: Detect profile name by scanning files and obtaining device name from EXIF
    // TODO: Use videoTimeoffset from profile

    val profile = profileByName(parameters.deviceProfileName().orEmpty())
    if (profile != null) {
        info("Profile found for device ${parameters.deviceProfileName()}. 'useExif' and 'timeOffsetInMins' will be overridden");
        info("Profile: $profile")
        return computeFilesWithTimestamps(filePaths, profile.photosUseExif, profile.photosTimeOffsetInMins)
    } else {
        return computeFilesWithTimestamps(filePaths, parameters.useEXIF(), parameters.timeOffsetInMinutes())
    }
}

private fun computeFilesWithTimestamps(listFilesPaths: List<Path>, useEXIF: Boolean, timeOffsetInMinutes: Int): List<FileWithTimestamp> {
    return listFilesPaths
            .map { computeFileWithTimestamp(it, useEXIF, timeOffsetInMinutes) }
}

private fun computeFileWithTimestamp(path: Path, useEXIF: Boolean, timeOffsetInMinutes: Int): FileWithTimestamp {
    // first, try to obtain timestamp from file's name
    val timestampFromName: Optional<LocalDateTime> = obtainTimestampFromFilename(path)

    val datetime = if (timestampFromName.isPresent) {
        timestampFromName.get()
    } else if (useEXIF) {
        fromEXIF(path) ?: fromFileAttributes(path)
    } else {
        fromFileAttributes(path)
    }
    val datetimeWithOffset = applyTimeOffset(datetime, timeOffsetInMinutes)
    return FileWithTimestamp(path, datetimeWithOffset)
}

fun obtainTimestampFromFilename(path: Path): Optional<LocalDateTime>  {
    return try {
        val nameWithoutExtension = path.toFile().nameWithoutExtension
        val localDateTime = LocalDateTime.parse(nameWithoutExtension, DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss"))
        debug("Timestamp [$localDateTime] obtained from filename for a file [$path]")
        Statistics.datetimesFromFilename++
        Optional.of(localDateTime)
    } catch (e: Exception) {
        empty()
    }
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

        // TODO: Not user if timezone makes sense and works as expected - investigate
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
    val lastModifiedTime = attributes.lastModifiedTime() // creationTime not working since JDK 21
    debug("Last Modified Time [$lastModifiedTime] obtained from file attributes for a file [$path]")
    Statistics.datetimesFromFileAttributes++
    return toLocalDatetime(lastModifiedTime.toInstant())
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


