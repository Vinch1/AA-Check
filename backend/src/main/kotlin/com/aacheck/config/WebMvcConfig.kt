package com.aacheck.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Paths

@Configuration
class WebMvcConfig(
    @Value("\${app.upload-dir}") private val uploadDir: String,
    @Value("\${app.image-url-prefix}") private val urlPrefix: String,
) : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val absolute = Paths.get(uploadDir).toAbsolutePath().normalize().toString()
        registry.addResourceHandler("$urlPrefix/**")
            .addResourceLocations("file:$absolute/")
    }
}
