package com.example.homepanel.data.model

// Профиль качества Radarr/Sonarr («1080p · RU»): по нему сервис потом сам ищет обновления
data class QualityProfile(val id: Int, val name: String)

// Radarr/Sonarr не увидел выбранную раздачу у себя (например, другой сезон)
class ReleaseNotFoundException : Exception("release not found by Radarr/Sonarr")
