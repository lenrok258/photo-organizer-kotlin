package kze.photoorganizer

import kze.photoorganizer.timestamp.FileWithTimestamp
import org.apache.commons.codec.digest.DigestUtils


fun deduplicate(filesWithTimestamps: List<FileWithTimestamp>): List<FileWithTimestamp> {

    info("About to search for files with duplicated content")
    val result = ArrayList<FileWithTimestamp>()
    val hashesMap = HashMap<String, FileWithTimestamp>()

    for (file in filesWithTimestamps) {
        val md5Hex = DigestUtils.md5Hex(file.filePath.toFile().inputStream())
        debug("MD5 [$md5Hex] for a file [${file.filePath}]")
        if (hashesMap.containsKey(md5Hex)) {
            val existingFile = hashesMap.get(md5Hex)
            warn("Duplicate found. Files: [${file.filePath}] and [$existingFile] have the same hash [$md5Hex]. Duplicate will be skipped")
            Statistics.filesWithDuplicatedContent++
            continue
        }
        hashesMap.put(md5Hex, file)
        result.add(file)
    }

    return result
}