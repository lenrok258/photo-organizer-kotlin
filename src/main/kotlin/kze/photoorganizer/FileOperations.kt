package kze.photoorganizer

import org.apache.commons.io.FileUtils.deleteDirectory
import java.nio.file.Files.createDirectory
import java.nio.file.Files.walk
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors.toList

fun createOutputDirectory(): Path {
    val outputPath = Paths.get(OUTPUT_DIRECTORY_NAME)
    return outputPath.apply {
        deleteDirectory(this.toFile())
        createDirectory(this)
        info("Output directory=[${this.toAbsolutePath()}] created")
    }
}

fun listFilesPaths(inputDirPath: Path): List<Path> {
    return walk(inputDirPath)
            .filter { path -> path.toFile().isFile }
            .collect(toList())
            .apply { Statistics.filesToOrganize = this.size }
}