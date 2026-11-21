package com.example.homepanel.data

// Ручной DI: одно место, где создаются зависимости приложения.
// ViewModel получают репозиторий отсюда, а не создают его сами.
interface AppContainer {
    val repository: MediaRepository
}

class DefaultAppContainer : AppContainer {
    override val repository: MediaRepository by lazy { FakeMediaRepository() }
}
