package com.example.homepanel.data.network.dto

import com.example.homepanel.data.model.QualityProfile
import kotlinx.serialization.Serializable

// Общие ответы Radarr и Sonarr (API v3 у них одинаковое)

@Serializable
data class ArrItem(val id: Int = 0, val title: String = "")

@Serializable
data class ArrProfile(val id: Int, val name: String = "")

@Serializable
data class ArrRootFolder(val path: String = "")

// Раздача из живого поиска Radarr/Sonarr (по ней и делается grab)
@Serializable
data class ArrRelease(
    val guid: String = "",
    val title: String = "",
    val size: Long = 0,
    val indexerId: Int = 0,
)

@Serializable
data class ArrGrabBody(val guid: String, val indexerId: Int)

fun ArrProfile.toModel() = QualityProfile(id, name)
