package com.aacheck.common

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ApiException::class)
    fun handleApi(e: ApiException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(e.status).body(mapOf("message" to e.message))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<Map<String, String>> =
        ResponseEntity.badRequest().body(
            mapOf("message" to (e.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "参数不合法"))
        )

    @ExceptionHandler(Exception::class)
    fun handleOther(e: Exception): ResponseEntity<Map<String, String>> {
        e.printStackTrace()
        return ResponseEntity.internalServerError().body(mapOf("message" to (e.message ?: "服务器内部错误")))
    }
}
