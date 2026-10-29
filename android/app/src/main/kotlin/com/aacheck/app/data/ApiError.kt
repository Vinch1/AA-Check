package com.aacheck.app.data

import com.google.gson.Gson
import retrofit2.HttpException

/** 把异常转换成给用户看的提示，优先使用后端返回的 message */
fun Throwable.toUserMessage(): String {
    val http = this as? HttpException
    if (http != null) {
        val body = runCatching { http.response()?.errorBody()?.string() }.getOrNull()
        if (!body.isNullOrBlank()) {
            val msg = runCatching { Gson().fromJson(body, ErrorBody::class.java)?.message }.getOrNull()
            if (!msg.isNullOrBlank()) return msg
        }
        return when (http.code()) {
            400 -> "请求参数不正确"
            401 -> "登录已过期，请重新登录"
            403 -> "没有权限执行此操作"
            404 -> "数据不存在"
            else -> "请求失败（HTTP ${http.code()}）"
        }
    }
    return message ?: "网络请求失败，请检查手机与电脑是否在同一 Wi-Fi"
}
