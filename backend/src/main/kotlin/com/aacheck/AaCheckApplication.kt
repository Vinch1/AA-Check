package com.aacheck

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AaCheckApplication

fun main(args: Array<String>) {
    runApplication<AaCheckApplication>(*args)
}
