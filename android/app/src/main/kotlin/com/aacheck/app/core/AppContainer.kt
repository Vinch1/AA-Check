package com.aacheck.app.core

import android.content.Context
import com.aacheck.app.data.ApiService
import com.aacheck.app.data.Repository
import com.aacheck.app.data.WsClient
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 后端地址：
 * - Android Studio 模拟器：http://10.0.2.2:8080/
 * - 真机：改成 Mac 的局域网 IP，且手机与 Mac 同一 Wi-Fi
 */
object ServerConfig {
     var baseUrl: String = "http://10.0.2.2:8080/"
    val wsUrl: String get() = baseUrl.replace("http", "ws").trimEnd('/') + "/ws"

    fun absoluteUrl(pathOrUrl: String?): String? {
        if (pathOrUrl.isNullOrBlank()) return null
        return if (pathOrUrl.startsWith("http")) pathOrUrl
        else baseUrl.trimEnd('/') + "/" + pathOrUrl.trimStart('/')
    }
}

class AppContainer(context: Context) {
    val tokenStore = TokenStore(context)

    private val gson = Gson()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val token = tokenStore.token
            val req = if (token != null) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else chain.request()
            chain.proceed(req)
        }
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    val api: ApiService = Retrofit.Builder()
        .baseUrl(ServerConfig.baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(ApiService::class.java)

    val repository = Repository(api, tokenStore)
    val wsClient = WsClient(tokenStore)
}
