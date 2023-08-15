#!/usr/bin/env kotlinc -script -J-Xmx2g

@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.5.1")

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.serialization.json.*


val version = "test"//tag()
val headers = githubHeaders()

File("./tmp/").walk().forEach { fileToUpload ->
    uploadFile(fileToUpload)
}

println("Upload complete")

fun uploadFile(fileToUpload: File) {
    val fileName = fileToUpload.name
    val releaseUrl = "https://api.github.com/repos/ygdrasil-io/SDL-binary/releases/tags/$version"
    val releaseRequest = URL(releaseUrl).openConnection() as HttpURLConnection
    releaseRequest.requestMethod = "GET"
    releaseRequest.setRequestProperty("Authorization", "Bearer ${headers["Authorization"]}")
    val releaseResponse = releaseRequest.inputStream.bufferedReader().use { it.readText() }
    val json = Json.parseToJsonElement(releaseResponse).jsonObject
    val uploadUrl = URL(URL(json["upload_url"]!!.toString()).protocol + "://" + URL(releaseResponse).host).toString() + "/assets"

    println("Uploading $fileName to $uploadUrl")
    val uploadRequest = URL("$uploadUrl?name=$fileName").openConnection() as HttpURLConnection
    uploadRequest.requestMethod = "POST"
    uploadRequest.setRequestProperty("Authorization", "Bearer ${headers["Authorization"]}")
    uploadRequest.setRequestProperty("Content-Type", "application/octet-stream")
    uploadRequest.setRequestProperty("Content-Length", fileToUpload.length().toString())
    uploadRequest.doOutput = true
    fileToUpload.inputStream().use { input ->
        uploadRequest.outputStream.use { output ->
            input.copyTo(output)
        }
    }
}


fun tag() = System.getenv("GITHUB_REF_NAME")

fun githubHeaders(): Map<String, String> {
    val githubBasic = System.getenv("GITHUB_BASIC")
    val githubToken = System.getenv("GITHUB_TOKEN")

    val auth = if (githubBasic != null) {
        "Basic ${Base64.getEncoder().encodeToString(githubBasic.toByteArray(StandardCharsets.UTF_8))}"
    } else {
        "token $githubToken"
    }

    return mapOf(
        "Accept" to "application/vnd.github.v3+json",
        "Authorization" to auth
    )
}