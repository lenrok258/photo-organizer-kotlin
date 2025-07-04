package kze.photoorganizer

import java.time.Duration

object Statistics {

    var startMillis = 0L
    var stopMillis = 0L
    var filesToOrganize = 0
    var datetimesFromEXIF = 0
    var datetimesFromFilename = 0
    var filesWithDuplicatedContent = 0
    var datetimesFromFileAttributes = 0
    var filesCopied = 0

    private var extensionMap = HashMap<String, Int>()

    fun reportStart() {
        startMillis = System.currentTimeMillis()
    }

    fun reportStop() {
        stopMillis = System.currentTimeMillis()
    }

    fun reportExtension(extension: String) {
        val value = extensionMap.getOrDefault(extension, 0)
        extensionMap.put(extension, value.inc())
    }

    fun generateReport(): String {

        val duration = Duration.ofMillis(stopMillis - startMillis)

        return """


            |-----------------------------------------------------------------------------
            |  Statistics
            |-----------------------------------------------------------------------------
            |  Number of input files            |  $filesToOrganize
            |  Datetimes from EXIF metadata     |  $datetimesFromEXIF
            |  Datetimes from filenames         |  $datetimesFromFilename
            |  Datetimes from file attributes   |  $datetimesFromFileAttributes
            |  Files with duplicated content    |  $filesWithDuplicatedContent
            |  Files copied                     |  $filesCopied
            |  Files extensions                 |  $extensionMap
            |  Execution time (mm:ss:ms)        |  ${duration.toMinutes()}:${duration.seconds % 60}:${duration.toMillis() % 1000}
            |-----------------------------------------------------------------------------

        """.trimMargin()
    }

}