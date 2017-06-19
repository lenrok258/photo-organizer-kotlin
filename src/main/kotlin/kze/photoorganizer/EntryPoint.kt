package kze.photoorganizer

import kze.photoorganizer.config.LOCALE
import kze.photoorganizer.config.OUTPUT_DIRECTORY_NAME
import kze.photoorganizer.timestamp.FileWithTimestamp
import kze.photoorganizer.timestamp.computeFileWithTimestamp
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils.stripAccents
import java.lang.System.currentTimeMillis
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    Statistics.startMillis = currentTimeMillis()
    info("Start")

    validateInputParams(args)
    val inputDirPath: Path = getInputDirectory(args)
    val outputDirPath: Path = createOutputDirectory()
    val filesPaths: List<Path> = listFilesPaths(inputDirPath)
    val filesWithDatetimes = computeDatetimeFiles(filesPaths)
    val filesToOrganize = deduplicate(filesWithDatetimes)
    organizeFiles(outputDirPath, filesToOrganize)

    info("Stop")

    Statistics.stopMillis = currentTimeMillis()
    info(Statistics.getReport())
}

private fun printProgramUsage() {
    info("Usage: ./run.sh {directory-with-photos-to-process}")
}

private fun validateInputParams(args: Array<String>) {
    if (args.size < 1) {
        error("Missing required argument")
        printProgramUsage()
        exitProcess(-1)
    }
}

private fun getInputDirectory(args: Array<String>): Path {
    val inputDirString = args[0]
    val path = Paths.get(inputDirString)
    if (Files.exists(path).not()) {
        error("Given directory=[$inputDirString] does not exist")
        printProgramUsage()
        exitProcess(-1)
    }
    info("Input directory=[${path.toAbsolutePath()}]")
    return path
}

private fun createOutputDirectory(): Path {
    val outputPath = Paths.get(OUTPUT_DIRECTORY_NAME)
    FileUtils.deleteDirectory(outputPath.toFile())
    Files.createDirectory(outputPath)
    info("Output directory=[${outputPath.toAbsolutePath()}] created")
    return outputPath
}

private fun listFilesPaths(inputDirPath: Path): List<Path> {
    val paths = Files.walk(inputDirPath)
            .filter { path -> path.toFile().isFile }
            .collect(Collectors.toList())
    info("Number of files to organize=[%s]", paths.size)
    Statistics.filesToOrganize = paths.size
    return paths
}

private fun computeDatetimeFiles(listFilesPaths: List<Path>): List<FileWithTimestamp> {
    return listFilesPaths
            .map(::computeFileWithTimestamp)
}

private fun deduplicate(filesWithWithTimestamps: List<FileWithTimestamp>): List<FileWithTimestamp> {
    info("About to search for filesWithDuplicatedContent")
    val result = ArrayList<FileWithTimestamp>()
    val hashesMap = HashMap<String, FileWithTimestamp>()
    for (file in filesWithWithTimestamps) {
        val md5Hex = DigestUtils.md5Hex(file.filePath.toFile().inputStream())
        debug("MD5 [$md5Hex] for a file [${file.filePath}]")
        if (hashesMap.containsKey(md5Hex)) {
            val existingFile = hashesMap.get(md5Hex)
            warn("Duplicate found. Files: [${file.filePath}] and [$existingFile] have the same hash [$md5Hex]. Duplicate will be skipped")
            Statistics.filesWithDuplicatedContent++
            continue
        }
        hashesMap.put(md5Hex, file)
        result.add(file)
    }
    return result
}

private fun organizeFiles(outputDir: Path, filesToOrganize: List<FileWithTimestamp>) {
    filesToOrganize
            .forEach {
                val targetDir = createTargetDirectory(outputDir, it)
                val targetFilePath = createTargetFilePath(targetDir, it)
                copyFile(it, targetFilePath)
            }

}

private fun createTargetDirectory(outputDir: Path, fileWithTimestamp: FileWithTimestamp): Path {
    val dateTime = fileWithTimestamp.dateTime
    val year = dateTime.format(DateTimeFormatter.ofPattern("yyyy"))
    val monthNumber = dateTime.format(DateTimeFormatter.ofPattern("MM"))
    val monthName = stripAccents(dateTime.format(DateTimeFormatter.ofPattern("LLLL", LOCALE)).capitalize())

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
    while (Files.exists(targetPathNonExistence)) {
        targetPathNonExistence = computeTargetPathWithPostfix(targetPath, i++.toString())
        warn("Target file already exists. Trying version with postfix=[$targetPathNonExistence]")
    }

    info("[${fileWithTimestamp.filePath.fileName}] => [$targetPathNonExistence]")
    Files.copy(fileWithTimestamp.filePath, targetPathNonExistence)

    var extension = targetPathNonExistence.toFile().extension
    Statistics.filesCopied++
    Statistics.reportExtension(extension)
}

private fun computeTargetPathWithPostfix(targetPath: Path, postfix: String): Path {
    val file = targetPath.toFile()
    return Paths.get(file.parent, "${file.nameWithoutExtension}_${postfix}.${file.extension}")
}



