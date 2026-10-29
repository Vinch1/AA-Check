package com.aacheck.common

import org.springframework.http.HttpStatus

class ApiException(
    val status: HttpStatus,
    override val message: String,
) : RuntimeException(message)

fun notFound(message: String) = ApiException(HttpStatus.NOT_FOUND, message)
fun badRequest(message: String) = ApiException(HttpStatus.BAD_REQUEST, message)
fun forbidden(message: String) = ApiException(HttpStatus.FORBIDDEN, message)
fun unauthorized(message: String) = ApiException(HttpStatus.UNAUTHORIZED, message)
