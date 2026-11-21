package com.example.homepanel.data.model

// Что сейчас происходит с раздачей (упрощённо из состояний qBittorrent)
enum class TorrentState { DOWNLOADING, PAUSED, DONE, ERROR }

data class Torrent(
    val hash: String,
    val name: String,
    val sizeBytes: Long,
    val progress: Float,           // 0..1
    val downloadSpeed: Long,       // байт/с
    val etaSeconds: Long,
    val state: TorrentState,
)
