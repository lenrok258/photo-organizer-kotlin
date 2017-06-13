package kze.photoorganizer

import kze.photoorganizer.config.LOCALE
import kze.photoorganizer.config.OUTPUT_DIRECTORY_NAME
import kze.photoorganizer.datetime.DatetimeFile
import kze.photoorganizer.datetime.computeDatetimeFile
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.StringUtils.stripAccents
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    info("Start")

    validateInputParams(args)
    val inputDirPath: Path = getInputDirectory(args)
    val outputDirPath: Path = createOutputDirectory()
    val filesPaths: List<Path> = listFilesPaths(inputDirPath)
    val filesWithDatetimes = computeDatetimeFiles(filesPaths)
    val filesToOrganize = deduplicate(filesWithDatetimes)
    organizeFiles(outputDirPath, filesToOrganize)

    info("Stop")
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
    Files.deleteIfExists(outputPath)
    Files.createDirectory(outputPath)
    info("Output directory=[${outputPath.toAbsolutePath()}] created")
    return outputPath
}

private fun listFilesPaths(inputDirPath: Path): List<Path> {
    val paths = Files.walk(inputDirPath)
            .filter { path -> path.toFile().isFile }
            .collect(Collectors.toList())
    info("Number of files to organize=[%s]", paths.size)
    return paths
}

private fun computeDatetimeFiles(listFilesPaths: List<Path>): List<DatetimeFile> {
    return listFilesPaths
            .map(::computeDatetimeFile)
}

private fun deduplicate(filesWithDatetimes: List<DatetimeFile>): List<DatetimeFile> {
    info("About to search for duplicates")
    val result = ArrayList<DatetimeFile>()
    val hashesMap = HashMap<String, DatetimeFile>()
    for (file in filesWithDatetimes) {
        val md5Hex = DigestUtils.md5Hex(file.filePath.toFile().inputStream())
        debug("MD5 [$md5Hex] for a file [${file.filePath}]")
        if (hashesMap.containsKey(md5Hex)) {
            val existingFile = hashesMap.get(md5Hex)
            warn("Duplicate found. Files: [${file.filePath}] and [$existingFile] have the same hash [$md5Hex]. Duplicate will be skipped")
            continue
        }
        hashesMap.put(md5Hex, file)
        result.add(file)
    }
    return result
}

private fun organizeFiles(outputDir: Path, filesToOrganize: List<DatetimeFile>) {
    filesToOrganize
            .forEach {
                val targetDir = createTargetDirectory(outputDir, it)
                val targetFilePath = createTargetFilePath(targetDir, it)
                copyFile(it, targetFilePath)
            }

}

private fun createTargetDirectory(outputDir: Path, datetimeFile: DatetimeFile): Path {
    val dateTime = datetimeFile.dateTime
    val year = dateTime.format(DateTimeFormatter.ofPattern("yyyy"))
    val monthNumber = dateTime.format(DateTimeFormatter.ofPattern("MM"))
    val monthName = stripAccents(dateTime.format(DateTimeFormatter.ofPattern("LLLL", LOCALE)).capitalize())

    val monthDirName = "${year}_${monthNumber}${monthName}"

    val targetPath = Paths.get(outputDir.toString(), year, monthDirName)
    Files.createDirectories(targetPath)
    return targetPath
}

private fun createTargetFilePath(targetDir: Path, datetimeFile: DatetimeFile): Path {
    val dateTime = datetimeFile.dateTime
    val baseFileName = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
    val extension = datetimeFile.filePath.toFile().extension
    val targetFileName = "$baseFileName.$extension"
    val result = Paths.get(targetDir.toString(), targetFileName)
    return result
}

private fun copyFile(datetimeFile: DatetimeFile, targetPath: Path) {
    info("[${datetimeFile.filePath.fileName}] => [$targetPath]")
    //TODO: Check if file already exists and add postfix _X
    Files.copy(datetimeFile.filePath, targetPath)
}



