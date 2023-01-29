package com.react.musicServer.controllers

import com.react.musicServer.data.Data
import com.react.musicServer.data.message.MessageData
import com.react.musicServer.data.filepart.RemoteFilePart
import com.react.musicServer.services.MainUploadService
import com.react.musicServer.services.YoutubeUploadService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.reactive.server.awaitFormData
import org.springframework.web.server.ServerWebExchange
import java.util.*

@RestController
@RequestMapping("api")
class ApiController @Autowired constructor(
    private val service: MainUploadService,
    private val youtubeUploadService: YoutubeUploadService,
) {
//    @GetMapping("download/{uuid:.+}")
//    suspend fun download(@PathVariable uuid: UUID): ByteArray {
//        val pair = service.download(uuid) ?: throw FileNotFoundException("Could not find file!")
//        return pair.first
//        return ServerResponse.ok()
//            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pair.second + "\"")
//            .header(HttpHeaders.CONTENT_TYPE, withContext(Dispatchers.IO) {
//                URLConnection.guessContentTypeFromStream(pair.first.inputStream())
//            })
//            .bodyValueAndAwait(pair.first)
//    }

    @PostMapping("upload")
    suspend fun upload(
        @RequestPart("file") file: FilePart,
        @RequestHeader("Content-Length") contentLength: Long
    ): MessageData {
        if (contentLength > Data.MAX_FILE_SIZE)
            throw MaxUploadSizeExceededException(contentLength)
        val result = service.upload(RemoteFilePart(file))
        return MessageData(
            fileName = result.filename,
            fileUrl = "/api/download/${result.uuid}",
            fileUUID = result.uuid,
            fileTranscoded = result.wasTranscoded,
            message = "File uploaded with success!"
        )
    }

    @RequestMapping(
        value = ["upload/youtube"],
        method = [RequestMethod.POST],
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE]
    )
    suspend fun uploadFromYoutube(exchange: ServerWebExchange): MessageData {
        val formData = exchange.awaitFormData()
        val result = youtubeUploadService.upload(formData["url"]?.firstOrNull()!!)
        return MessageData(
            fileName = result.filename,
            fileUrl = "/api/download/${result.uuid}",
            fileUUID = result.uuid,
            fileTranscoded = result.wasTranscoded,
            message = "Audio uploaded with success!"
        )
    }
}
