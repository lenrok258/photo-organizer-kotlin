package kze.photoorganizer

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.system.exitProcess

class ProgramParameters(args: Array<String>) {

    init {
        validate(args)
    }

    fun getInputDirectory(args: Array<String>): Path {
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

    private fun validate(args: Array<String>) {
        if (args.size < 1) {
            error("Missing required argument")
            printProgramUsage()
            exitProcess(-1)
        }
    }

    private fun printProgramUsage() {
        info("Usage: ./run.sh {directory-with-photos-to-process}")
    }
}