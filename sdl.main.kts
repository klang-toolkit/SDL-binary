#!/usr/bin/env kotlinc -script -J-Xmx2g

import java.io.*
import java.net.URL


val fileUrl = "https://github.com/libsdl-org/SDL/releases/download/release-2.28.2/SDL2-2.28.2.dmg"
val fileName = "./tmp/temp.bin"
downloadFile(fileUrl, fileName)
"hdiutil attach $fileName".run { println(it) }
"cp -R /Volumes/SDL2/SDL2.framework ./tmp/SDL2".run { println(it) }
"zip -r ./tmp/headers.zip ./tmp/SDL2/Versions/A/Headers".run { println(it) }

fun downloadFile(fileUrl: String, fileName: String) {
    File(fileName).parentFile.mkdirs()
    BufferedInputStream(URL(fileUrl).openStream()).use { bufferedInputStream ->
        FileOutputStream(fileName).use { fileOutputStream ->
            val dataBuffer = ByteArray(1024)
            var bytesRead: Int
            while (bufferedInputStream.read(dataBuffer, 0, 1024).also { bytesRead = it } != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead)
            }
        }
    }

}

fun String.run(onError: (String) -> Unit) {
    val process = ProcessBuilder()
        .command("bash", "-c", this)
        .start()

    val reader = BufferedReader(InputStreamReader(process.errorStream))
    val errors = mutableListOf<String>()
    var line: String?
    while (reader.readLine().also { line = it } != null) {
        errors.add(line ?: "")
    }

    if (errors.isNotEmpty()) {
        onError(errors.joinToString("\n"))
    }
}
