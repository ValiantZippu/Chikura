package com.chikura.auth

actual fun createGitHubAuthDataSource(): GitHubAuthDataSource = object : GitHubAuthDataSource {
    private var token: GitHubToken? = null
    override suspend fun getToken(): GitHubToken? = token
    override suspend fun saveToken(token: GitHubToken) { this.token = token }
    override suspend fun clearToken() { token = null }
    override suspend fun getUser(token: GitHubToken): GitHubUser? = null
}
