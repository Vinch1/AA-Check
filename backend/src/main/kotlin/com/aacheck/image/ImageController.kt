package com.aacheck.image

import com.aacheck.common.badRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

@RestController
@RequestMapping("/api/images")
class ImageController(
    @Value("\${app.upload-dir}") private val uploadDir: String,
    @Value("\${app.image-url-prefix}") private val urlPrefix: String,
) {

    @PostMapping
    fun upload(@RequestParam("file") file: MultipartFile): Map<String, String> {
        if (file.isEmpty) throw badRequest("图片为空")
        if (file.size > 3 * 1024 * 1024) throw badRequest("图片不能超过 3MB（客户端应先压缩）")
        val ext = when (file.contentType) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> throw badRequest("仅支持 jpg/png/webp")
        }
        val dir = Paths.get(uploadDir).toAbsolutePath().normalize()
        Files.createDirectories(dir)
        val name = "${UUID.randomUUID()}.$ext"
        Files.copy(file.inputStream, dir.resolve(name))
        return mapOf("url" to "$urlPrefix/$name")
    }
}
