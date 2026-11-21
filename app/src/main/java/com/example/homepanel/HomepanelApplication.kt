package com.example.homepanel

import android.app.Application
import com.example.homepanel.data.AppContainer
import com.example.homepanel.data.DefaultAppContainer

// Живёт всё время работы приложения и держит контейнер зависимостей
class HomepanelApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer()
    }
}
