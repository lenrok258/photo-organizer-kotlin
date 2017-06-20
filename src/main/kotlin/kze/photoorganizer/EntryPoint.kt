package kze.photoorganizer

import kze.photoorganizer.timestamp.computeFilesWithTimestamps
import java.nio.file.Path

fun main(args: Array<String>) {

    Statistics.reportStart()

    val parameters = ProgramParameters(args)
    val inputDirPath: Path = parameters.getInputDirectory(args)
    val outputDirPath: Path = createOutputDirectory()
    val filesPaths: List<Path> = listFilesPaths(inputDirPath)
    val filesWithTimestamps = computeFilesWithTimestamps(filesPaths)
    val filesToOrganize = deduplicate(filesWithTimestamps)
    organizeFiles(outputDirPath, filesToOrganize)

    info(Statistics.generateReport())
}


