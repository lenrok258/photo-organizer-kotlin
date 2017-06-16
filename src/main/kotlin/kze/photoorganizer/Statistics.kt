package kze.photoorganizer

import java.time.Duration

object Statistics {

    var startMillis = 0L
    var stopMillis = 0L
    var filesToOrganize = 0
    var filesWithValidEXIFData = 0
    var filesWithDuplicatedContent = 0
    var datetimesFromFileAttributes = 0
    var filesCopied = 0

    fun report(): String {

        val duration = Duration.ofMillis(stopMillis - startMillis)

        return """


            |-----------------------------------------------------------------------------
            |  Statistics
            |-----------------------------------------------------------------------------
            |  Number of input files            |  $filesToOrganize
            |  Files with proper EXIF           |  $filesWithValidEXIFData
            |  Datetimes from file attributes   |  $datetimesFromFileAttributes
            |  Files with duplicated content    |  $filesWithDuplicatedContent
            |  Files copied                     |  $filesCopied
            |  Execution time (mm:ss:ms)        |  ${duration.toMinutes()}:${duration.seconds}:${duration.toMillis()}
            |-----------------------------------------------------------------------------

        """.trimMargin()


    }

}