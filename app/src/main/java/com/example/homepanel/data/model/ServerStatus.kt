package com.example.homepanel.data.model

// Сервис на сервере и отвечает ли он
data class ServiceStatus(
    val name: String,
    val online: Boolean,
    val version: String?,
)

// Сводка по серверу для вкладки «Сервер»
data class ServerStatus(
    val serverName: String,
    val diskUsedBytes: Long,
    val diskTotalBytes: Long,
    val services: List<ServiceStatus>,
)
