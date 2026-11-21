package com.example.homepanel.data.network.dto

import com.example.homepanel.data.model.Release
import kotlinx.serialization.Serializable

@Serializable
data class ProwlarrRelease(
    val guid: String = "",
    val indexerId: Int = 0,
    val indexer: String = "",
    val title: String = "",
    val size: Long = 0,
    val seeders: Int? = null,
    val leechers: Int? = null,
)

fun ProwlarrRelease.toRelease() = Release(
    guid = guid,
    indexerId = indexerId,
    indexer = indexer,
    title = title,
    sizeBytes = size,
    seeders = seeders ?: 0,
    leechers = leechers ?: 0,
)
