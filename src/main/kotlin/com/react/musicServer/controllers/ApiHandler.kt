package com.react.musicServer.controllers

import com.react.musicServer.data.Data
import com.react.musicServer.data.message.MessageData
import com.react.musicServer.services.MainUploadService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import java.io.FileNotFoundException
import java.net.URLConnection
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.stream.Collectors

@Component
class ApiHandler constructor(@Autowired private val service: MainUploadService) {
    suspend fun download(request: ServerRequest): ServerResponse {
        val uuid = UUID.fromString(request.pathVariable("uuid"))
        val pair = service.download(uuid) ?: throw FileNotFoundException("Could not find file!")
        return ServerResponse.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + String(pair.second.toByteArray(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1) + "\"")
            .header(HttpHeaders.ACCEPT_CHARSET, "utf-8")
            .header(HttpHeaders.CONTENT_TYPE, withContext(Dispatchers.IO) {
                URLConnection.guessContentTypeFromName(pair.second)
            })
            .bodyValueAndAwait(pair.first)
    }

    suspend fun downloadHead(request: ServerRequest): ServerResponse {
        val uuid = UUID.fromString(request.pathVariable("uuid"))
        val pair = service.download(uuid) ?: throw FileNotFoundException("Could not find file!")
        return ServerResponse.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + String(pair.second.toByteArray(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1) + "\"")
            .header(HttpHeaders.ACCEPT_CHARSET, "utf-8")
//            .header(HttpHeaders.CONTENT_LENGTH, pair.first.size.toString())
            .header(HttpHeaders.CONTENT_TYPE, withContext(Dispatchers.IO) {
                URLConnection.guessContentTypeFromName(pair.second)
            })
            .bodyValueAndAwait("")
    }

    suspend fun delete(request: ServerRequest): ServerResponse {
        val uuid = UUID.fromString(request.pathVariable("uuid"))
        val fileName = service.delete(uuid) ?: throw FileNotFoundException("Could not find file!")
        return ServerResponse.ok()
            .bodyValueAndAwait(
                MessageData(
                    fileName = fileName,
                    fileUrl = request.uri().toString(),
                    fileUUID = uuid.toString(),
                    message = "File deleted with success!"
                )
            )
    }

//    suspend fun upload(request: ServerRequest): Mono<ServerResponse> {
//        val data = request.awaitMultipartData()["file"] as List<FilePart>
//        val pair = service.upload(data.stream().findFirst())
//        val file = request.multipartData().flatMapIterable { map ->
//            map.keys.stream()
//                .filter { it == "files" }
//                .flatMap { key -> map[key]?.stream()?.filter { part -> part is FilePart } }
//                .collect(Collectors.toList())
//        }.doOnNext {
//            println(it.name())
//            it.transferTo()
//        }.collect(Collectors.toList())
//        val pair = service.upload(file)
        /*return ResponseEntity.status(HttpStatus.OK)
            .body(
                MessageData(
                    fileName = pair.first,
                    fileUrl = serverHttpRequest.uri.host
                        .plus("/api/download/")
                        .plus(pair.first),
                    message = "File uploaded with success!"
                )
            )*/
//        return ServerResponse.ok().build()
//    }

    suspend fun getMusic(request: ServerRequest): ServerResponse =
        ServerResponse.ok()
            .bodyValueAndAwait(
                Data.config.filesList.stream().map {
                    MessageData(
                        fileName = it.fileName,
                        fileUrl = request.uri().toString().removeSuffix(request.path())
                            .plus("/api/download/")
                            .plus(it.uuid),
                        fileUUID = it.uuid.toString(),
                        fileType = URLConnection.guessContentTypeFromName(it.fileName),
                        message = "File uploaded with success!"
                    )
                }.collect(Collectors.toList())
            )
}