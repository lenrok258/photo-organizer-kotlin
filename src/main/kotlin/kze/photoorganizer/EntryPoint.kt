package kze.photoorganizer

import kze.photoorganizer.timestamp.computeFilesWithTimestamps
import java.nio.file.Path

fun main(args: Array<String>) {

    Statistics.reportStart()

    val parameters = ProgramParameters(args)

    val inputDirPath: Path = parameters.inputDirectory()
    val outputDirPath: Path = createOutputDirectory(inputDirPath)
    val filesPaths: List<Path> = listFilesPaths(inputDirPath)
    val filesWithTimestamps = computeFilesWithTimestamps(filesPaths, parameters.useEXIF(), parameters.timeOffsetInMinutes())
    val filesToOrganize = if (parameters.skipDuplicatesCheck()) filesWithTimestamps else deduplicate(filesWithTimestamps)
    organizeFiles(outputDirPath, filesToOrganize)

    Statistics.reportStop()
    info(Statistics.generateReport())
}


