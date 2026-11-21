package com.example.homepanel.data.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

// Добавляет заголовок с ключом только к запросам на свой сервис (по хосту и порту).
// Так один OkHttpClient обслуживает все сервисы, и ключ не утечёт в чужой адрес.
class ApiKeyInterceptor(
    baseUrl: String,
    private val header: String,
    private val value: String,
) : Interceptor {
    private val target = baseUrl.toHttpUrlOrNull()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        if (target == null || url.host != target.host || url.port != target.port) {
            return chain.proceed(request)
        }
        return chain.proceed(request.newBuilder().header(header, value).build())
    }
}
