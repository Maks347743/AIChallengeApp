package com.example.githubmcpserver.tools

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubSearchResponse(
    val items: List<GitHubRepository> = emptyList()
)

@Serializable
data class GitHubRepository(
    @SerialName("full_name") val fullName: String = "unknown",
    val description: String? = null,
    @SerialName("stargazers_count") val stars: Int = 0,
    val language: String? = null,
    @SerialName("html_url") val htmlUrl: String = ""
)

@Serializable
data class GitHubUser(
    val login: String = "unknown",
    val name: String? = null,
    val bio: String? = null,
    @SerialName("public_repos") val publicRepos: Int = 0,
    val followers: Int = 0
)
