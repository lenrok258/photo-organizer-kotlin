package kze.photoorganizer

object Statistics {

    var startMillis = 0L
    var stopMillis = 0L
    var filesToOrganize = 0
    var filesWithValidEXIFData = 0
    var filesWithDuplicatedContent = 0

    fun report(): String {
        return """


            |--------------------------------------------
            |  Statistics
            |--------------------------------------------
            |  filesToOrganize            |  $filesToOrganize
            |  filesWithValidEXIFData     |  $filesWithValidEXIFData
            |  filesWithDuplicatedContent |  $filesWithDuplicatedContent
            |  totalTime                  |  ${(stopMillis - startMillis) * 1000L} seconds
            |--------------------------------------------

        """.trimMargin()
    }

}