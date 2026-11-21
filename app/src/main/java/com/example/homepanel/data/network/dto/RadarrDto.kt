package com.example.homepanel.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class RadarrDisk(val path: String = "", val freeSpace: Long = 0, val totalSpace: Long = 0)

@Serializable
data class RadarrStatus(val version: String = "")
