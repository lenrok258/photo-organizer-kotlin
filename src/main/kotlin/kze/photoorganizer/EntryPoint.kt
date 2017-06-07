package kze.photoorganizer

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    info("Start")

    validateInputParams(args)
    val inputDirPath: Path = getInputDirectory(args)
    val outputDirPath: Path = createOutputDirectory(inputDirPath)
//    val listFiles: File? = listFiles(inputDirPath)

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

fun createOutputDirectory(inputDirPath: Path): Path {
    val outputPath = Paths.get(OUTPUT_DIRECTORY_NAME)
    Files.createDirectory(outputPath)
    info("Output directory=[${outputPath.toAbsolutePath()}] created")
    return outputPath
}

/*fun listFiles(inputDirPath: Path): File? {
    Files.walk(inputDirPath)
            .filter()

}*/
