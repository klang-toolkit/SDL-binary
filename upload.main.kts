#!/usr/bin/env kotlinc -script -J-Xmx2g

@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.5.1")

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*


val version = System.getenv("GITHUB_REF_NAME")
val token = System.getenv("GITHUB_TOKEN")

File("./tmp/").walk()
    .filter { it.isFile }
    .forEach { fileToUpload ->
        uploadFile(fileToUpload)
    }

println("Upload complete")

fun uploadFile(fileToUpload: File) {
    val fileName = fileToUpload.name
    val releaseUrl = "https://api.github.com/repos/klang-toolkit/SDL-binary/releases/tags/$version"
    val releaseRequest = URL(releaseUrl).openConnection() as HttpURLConnection
    releaseRequest.requestMethod = "GET"
    releaseRequest.setRequestProperty("Authorization", "Bearer $token")
    val releaseResponse = releaseRequest.inputStream.bufferedReader().use { it.readText() }
    val json = Json.parseToJsonElement(releaseResponse).jsonObject
    var uploadUrl = json["upload_url"]!!.jsonPrimitive.content
    uploadUrl = uploadUrl.substring(0, uploadUrl.indexOf("{"))

    println("Uploading $fileName to $uploadUrl")
    val uploadRequest = URL("$uploadUrl?name=$fileName").openConnection() as HttpURLConnection
    uploadRequest.requestMethod = "POST"
    uploadRequest.setRequestProperty("Authorization", "Bearer $token")
    uploadRequest.setRequestProperty("Content-Type", "application/octet-stream")
    uploadRequest.setRequestProperty("Content-Length", fileToUpload.length().toString())
    uploadRequest.doOutput = true
    fileToUpload.inputStream().use { input ->
        uploadRequest.outputStream.use { output ->
            input.copyTo(output)
        }
    }
    uploadRequest.inputStream.bufferedReader().use { it.readText() }
        .let { println(it) }
}


fun tag() = System.getenv("GITHUB_REF_NAME")

fun githubHeaders(): Map<String, String> {
    val githubBasic = System.getenv("GITHUB_BASIC")
    val githubToken = System.getenv("GITHUB_TOKEN")

    val auth = if (githubBasic != null) {
        "Basic ${Base64.getEncoder().encodeToString(githubBasic.toByteArray(StandardCharsets.UTF_8))}"
    } else {
        githubToken
    }

    return mapOf(
        "Accept" to "application/vnd.github.v3+json",
        "Authorization" to auth
    )
}
