package com.example.homepanel

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import com.example.homepanel.data.AppContainer
import com.example.homepanel.data.DefaultAppContainer

// Живёт всё время работы приложения и держит контейнер зависимостей.
// Также настраивает Coil: постеры грузятся тем же клиентом (с ключом) и кешируются на диск.
class HomepanelApplication : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer()
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient { container.httpClient }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("posters")).build() }
        .crossfade(true)
        .build()
}
