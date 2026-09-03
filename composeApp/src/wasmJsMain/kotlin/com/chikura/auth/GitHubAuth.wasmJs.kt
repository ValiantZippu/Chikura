package com.chikura.auth

import kotlinx.browser.localStorage
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true; isLenient = true }
private const val KEY = "chikura_github_token"

actual fun createGitHubAuthDataSource(): GitHubAuthDataSource = WasmGitHubAuthDataSource()

private class WasmGitHubAuthDataSource : GitHubAuthDataSource {
    override suspend fun getToken(): GitHubToken? {
        val raw = localStorage.getItem(KEY) ?: return null
        return try { json.decodeFromString<GitHubToken>(raw) } catch (_: Exception) { null }
    }
    override suspend fun saveToken(token: GitHubToken) {
        localStorage.setItem(KEY, json.encodeToString(GitHubToken.serializer(), token))
    }
    override suspend fun clearToken() {
        localStorage.removeItem(KEY)
    }
    override suspend fun getUser(token: GitHubToken): GitHubUser? = null // wasm: fetch via API with CORS — caller uses fetch; stub for bulk
}
