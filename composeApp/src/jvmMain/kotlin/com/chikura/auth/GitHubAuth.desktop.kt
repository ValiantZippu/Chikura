package com.chikura.auth

import com.chikura.platform.FileSystem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.File

private val tokenFile: File
    get() = File(System.getProperty("user.home"), "Chikura/.chikura-cache/github_token.json")

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

actual fun createGitHubAuthDataSource(): GitHubAuthDataSource = DesktopGitHubAuthDataSource()

private class DesktopGitHubAuthDataSource : GitHubAuthDataSource {
    override suspend fun getToken(): GitHubToken? {
        val f = tokenFile
        if (!f.exists()) return null
        return try { json.decodeFromString<GitHubToken>(f.readText()) } catch (_: Exception) { null }
    }
    override suspend fun saveToken(token: GitHubToken) {
        tokenFile.parentFile?.mkdirs()
        tokenFile.writeText(json.encodeToString(GitHubToken.serializer(), token))
    }
    override suspend fun clearToken() {
        tokenFile.delete()
    }
    override suspend fun getUser(token: GitHubToken): GitHubUser? {
        return try {
            val client = HttpClient(CIO) { install(ContentNegotiation) { json(json) } }
            val user: GitHubUser = client.get("https://api.github.com/user") {
                header("Authorization", "Bearer ${token.accessToken}")
                header("Accept", "application/vnd.github+json")
            }.body()
            client.close()
            user
        } catch (_: Exception) { null }
    }
}
