package kze.photoorganizer

import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ProgramParameters(args: Array<String>) {

    private data class ParametersData(val inputDir: String, val useEXIF: Boolean)

    private val cmdLineOptions: Options
    private val parametersData: ParametersData

    init {
        cmdLineOptions = createCmdLineOptions()
        parametersData = parseCmdLineArgs(args)
        validate(parametersData)
    }

    fun inputDirectory(): Path {
        val pathString = parametersData.inputDir
        info("Input directory=[$pathString]")
        return Paths.get(pathString)
    }

    fun useEXIF(): Boolean {
        val useEXIF = parametersData.useEXIF
        info("Use EXIF=[$useEXIF]")
        return useEXIF
    }

    private fun createCmdLineOptions(): Options {
        return Options().apply {
            addRequiredOption("i", "input", true, "Input directory path")
            addOption("e", "exif", false, "Enable reading timestamps from EXIF metadata")
        }
    }

    private fun parseCmdLineArgs(cmdLineArgs: Array<String>): ParametersData {
        val parser = DefaultParser()
        try {
            val values = parser.parse(cmdLineOptions, cmdLineArgs)
            return ParametersData(values.getOptionValue("i"), values.hasOption("e"))
        } catch(e: ParseException) {
            error(e.message ?: "")
            callForHelp()
            System.exit(-1)
        }
        throw IllegalStateException("Not gonna happen")
    }

    private fun validate(parametersData: ParametersData) {
        val inputDir = parametersData.inputDir
        val path = Paths.get(inputDir)
        if (!Files.isDirectory(path)) {
            exitWithErrorMessage("Given input path [$inputDir] in not a directory")
        }
        if (Files.notExists(path)) {
            exitWithErrorMessage("Given input path [$inputDir] does not exist")
        }
    }

    private fun exitWithErrorMessage(errorMessage: String) {
        error(errorMessage)
        callForHelp()
        System.exit(-1)
    }

    private fun callForHelp() { // intentional Twin Peaks reference
        HelpFormatter().apply {
            println()
            printHelp("run.sh", cmdLineOptions)
            println()
        }
    }

}