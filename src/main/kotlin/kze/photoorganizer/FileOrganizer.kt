package kze.photoorganizer

import kze.photoorganizer.timestamp.FileWithTimestamp
import org.apache.commons.lang3.StringUtils
import java.nio.file.Files
import java.nio.file.Files.copy
import java.nio.file.Files.exists
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption.COPY_ATTRIBUTES
import java.time.format.DateTimeFormatter

fun organizeFiles(outputDir: Path, filesToOrganize: List<FileWithTimestamp>) {
    filesToOrganize.forEach {
        val targetDir = createTargetDirectory(outputDir, it)
        val targetFilePath = createTargetFilePath(targetDir, it)
        copyFile(it, targetFilePath)
    }

}

private fun createTargetDirectory(outputDir: Path, fileWithTimestamp: FileWithTimestamp): Path {
    val dateTime = fileWithTimestamp.dateTime
    val year = dateTime.format(DateTimeFormatter.ofPattern("yyyy"))
    val monthNumber = dateTime.format(DateTimeFormatter.ofPattern("MM"))
    val monthName = StringUtils.stripAccents(dateTime.format(DateTimeFormatter.ofPattern("LLLL", LOCALE)).capitalize())

    val monthDirName = "${year}_${monthNumber}${monthName}"

    val targetPath = Paths.get(outputDir.toString(), year, monthDirName)
    Files.createDirectories(targetPath)
    return targetPath
}

private fun createTargetFilePath(targetDir: Path, fileWithTimestamp: FileWithTimestamp): Path {
    val dateTime = fileWithTimestamp.dateTime
    val baseFileName = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
    val extension = fileWithTimestamp.filePath.toFile().extension
    val targetFileName = "$baseFileName.$extension"
    val result = Paths.get(targetDir.toString(), targetFileName)
    return result
}

private fun copyFile(fileWithTimestamp: FileWithTimestamp, targetPath: Path) {
    var targetPathNonExistence = targetPath // TwinPeaks reference
    var i = 1
    while (exists(targetPathNonExistence)) {
        targetPathNonExistence = computeTargetPathWithPostfix(targetPath, i++.toString())
        warn("Target file already exists. Trying version with postfix=[$targetPathNonExistence]")
    }

    info("[${fileWithTimestamp.filePath.fileName}] => [$targetPathNonExistence]")
    copy(fileWithTimestamp.filePath, targetPathNonExistence, COPY_ATTRIBUTES)

    var extension = targetPathNonExistence.toFile().extension
    Statistics.filesCopied++
    Statistics.reportExtension(extension)
}

private fun computeTargetPathWithPostfix(targetPath: Path, postfix: String): Path {
    val file = targetPath.toFile()
    return Paths.get(file.parent, "${file.nameWithoutExtension}_${postfix}.${file.extension}")
}