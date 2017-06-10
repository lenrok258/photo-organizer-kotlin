package kze.photoorganizer

import kze.photoorganizer.config.OUTPUT_DIRECTORY_NAME
import kze.photoorganizer.datetime.DatetimeFile
import kze.photoorganizer.datetime.computeDatetimeFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    info("Start")

    validateInputParams(args)
    val inputDirPath: Path = getInputDirectory(args)
    val outputDirPath: Path = createOutputDirectory()
    val filesPaths: List<Path> = listFilesPaths(inputDirPath)
    val filesWithDatetimes = computeDatetimeFiles(filesPaths)
    // Segregate files

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


