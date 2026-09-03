package com.chikura.auth

import kotlinx.serialization.Serializable

@Serializable
data class GitHubToken(val accessToken: String, val tokenType: String = "bearer", val scope: String = "")

@Serializable
data class GitHubUser(val login: String, val id: Long = 0, val avatarUrl: String? = null, val name: String? = null)

interface GitHubAuthDataSource {
    suspend fun getToken(): GitHubToken?
    suspend fun saveToken(token: GitHubToken)
    suspend fun clearToken()
    suspend fun getUser(token: GitHubToken): GitHubUser?
}

// Pure bulk OAuth helpers — no UI, just URL building
object GitHubOAuth {
    // Fill these from env / buildConfig or docs
    const val CLIENT_ID = "Ov23li_placeholder" // replace with real OAuth App client_id
    const val REDIRECT_URI = "http://localhost:8080/callback" // or chikura://oauth
    const val SCOPE = "repo user"

    fun buildAuthorizeUrl(state: String = "chikura_state"): String =
        "https://github.com/login/oauth/authorize?client_id=$CLIENT_ID&redirect_uri=$REDIRECT_URI&scope=$SCOPE&state=$state"

    fun buildTokenRequestUrl(code: String): String =
        "https://github.com/login/oauth/access_token"

    // Exchange code for token via backend or direct (requires client_secret — should be proxied)
    // For pure client-side demo we store token after manual paste
}

expect fun createGitHubAuthDataSource(): GitHubAuthDataSource
