package com.example.homepanel.data.model

// Что происходит с задачей MediaDL
enum class JobStatus { QUEUED, RUNNING, DONE, FAILED }

// Задача MediaDL: один тайтл (фильм или несколько серий сезона), качается с онлайн-сайта
data class OnlineJob(
    val id: String,
    val title: String,
    val season: Int,
    val dub: String,
    val isFilm: Boolean,
    val status: JobStatus,
    val progress: Float,       // 0..1
    val doneEpisodes: Int,
    val totalEpisodes: Int,
    val error: String,         // текст ошибки от сервера, пусто если всё хорошо
    val createdAt: Double,     // unix-время, для сортировки
)
