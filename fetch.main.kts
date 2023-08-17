#!/usr/bin/env kotlinc -script -J-Xmx2g

import java.io.*
import java.net.URL

/** Constants **/
val darwinUrl = "https://github.com/libsdl-org/SDL/releases/download/release-2.28.2/SDL2-2.28.2.dmg"
val windowsUrl = "https://github.com/libsdl-org/SDL/releases/download/release-2.28.2/SDL2-2.28.2-win32-x64.zip"
val tmpName = "./tmp/tmp.bin"

/** Actual script **/
// Download darwin(mac) SDK and extract headers
downloadFile(darwinUrl, tmpName)
"hdiutil attach $tmpName".run { println(it) }
"cp -R /Volumes/SDL2/SDL2.framework ./tmp/SDL2.framework".run { println(it) }
"cp ./tmp/SDL2.framework/Versions/A/SDL2 ./tmp/libSDL2.dylib".run { println(it) }
"cp -R ./tmp/SDL2.framework/Versions/A/Headers ./tmp/".run { println(it) }
"mv ./tmp/Headers ./tmp/SDL2".run { println(it) }
"cd ./tmp && zip -r ./headers.zip ./SDL2".run { println(it) }

// cleanup
File(tmpName).delete()
File("./tmp/SDL2").deleteRecursively()
File("./tmp/SDL2.framework").deleteRecursively()

// Download windows SDK and extract headers
downloadFile(windowsUrl, tmpName)
"unzip $tmpName -d ./tmp/SDL2".run { println(it) }
"cp ./tmp/SDL2/SDL2.dll ./tmp/libSDL2.dll".run { println(it) }

// cleanup
File(tmpName).delete()
File("./tmp/SDL2").deleteRecursively()

/** Utilities **/
fun downloadFile(fileUrl: String, fileName: String) {
    File(fileName).apply {
        if (parentFile.exists().not()) parentFile.mkdirs()
    }
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
