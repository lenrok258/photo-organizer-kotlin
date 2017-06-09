package kze.photoorganizer

import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import kze.photoorganizer.config.OUTPUT_DIRECTORY_NAME
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.stream.Collectors
import kotlin.system.exitProcess
import com.drew.metadata.exif.ExifSubIFDDirectory




fun main(args: Array<String>) {
    info("Start")

    validateInputParams(args)
    val inputDirPath: Path = getInputDirectory(args)
    val outputDirPath: Path = createOutputDirectory()
    val filesPaths: List<Path> = listFilesPaths(inputDirPath)
    val filesWithDatetimes = computeFilesWithDatetimes(filesPaths)
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

private fun computeFilesWithDatetimes(listFilesPaths: List<Path>): List<FileWithDatetime> {
    return listFilesPaths
            .map(::computeFileWithDatetime)
}

private fun computeFileWithDatetime(path: Path): FileWithDatetime {
    // To remember: collision detection by computing hashes
    try {
        val metadata = ImageMetadataReader.readMetadata(path.toFile())
        val exifSubIFDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
        val date = exifSubIFDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)
        info("EXIF datetime for file $path = [%s]", date)
    } catch (e: ImageProcessingException) {
        warn("Cannot obtain EXIF for $path")
    }

    return FileWithDatetime(path, LocalDateTime.now())
}
