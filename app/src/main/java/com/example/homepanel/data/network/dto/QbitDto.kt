package com.example.homepanel.data.network.dto

import com.example.homepanel.data.model.Torrent
import com.example.homepanel.data.model.TorrentState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Раздача из /api/v2/torrents/info (берём только нужные поля)
@Serializable
data class QbitTorrent(
    @SerialName("hash") val hash: String,
    @SerialName("name") val name: String = "",
    @SerialName("size") val size: Long = 0,
    @SerialName("progress") val progress: Float = 0f,
    @SerialName("dlspeed") val dlspeed: Long = 0,
    @SerialName("eta") val eta: Long = 0,
    @SerialName("state") val state: String = "",
)

// У qBittorrent ~20 состояний, нам хватает четырёх
fun QbitTorrent.toTorrent(): Torrent {
    val s = when {
        state == "error" || state == "missingFiles" -> TorrentState.ERROR
        state.startsWith("paused") || state.startsWith("stopped") -> if (progress >= 1f) TorrentState.DONE else TorrentState.PAUSED
        progress >= 1f -> TorrentState.DONE
        else -> TorrentState.DOWNLOADING
    }
    return Torrent(
        hash = hash,
        name = name,
        sizeBytes = size,
        progress = progress,
        downloadSpeed = dlspeed,
        etaSeconds = if (eta >= 8_640_000) 0 else eta,  // 8640000 = «бесконечность» у qBittorrent
        state = s,
    )
}
