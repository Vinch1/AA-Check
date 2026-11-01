package com.aacheck

import java.net.URI
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AaCheckApplication

fun main(args: Array<String>) {
    // 云平台（Render / Heroku 等）注入的 DATABASE_URL 多为 postgres:// 格式，
    // 而 Spring JDBC 需要 jdbc:postgresql://，这里做兼容转换。
    // 若已用 DATASOURCE_URL 显式传入 jdbc 格式，则跳过。
    convertDatabaseUrl()
    runApplication<AaCheckApplication>(*args)
}

private fun convertDatabaseUrl() {
    val raw = System.getenv("DATABASE_URL") ?: return
    if (!raw.startsWith("postgres://") && !raw.startsWith("postgresql://")) return

    val uri = URI(raw.replace("postgres://", "http://").replace("postgresql://", "http://"))
    val host = uri.host
    val port = if (uri.port == -1) 5432 else uri.port
    val db = uri.path.removePrefix("/")
    System.setProperty("spring.datasource.url", "jdbc:postgresql://$host:$port/$db")

    uri.userInfo?.let { info ->
        val (user, password) = info.split(":", limit = 2)
        System.setProperty("spring.datasource.username", user)
        if (password.isNotEmpty()) System.setProperty("spring.datasource.password", password)
    }
}
