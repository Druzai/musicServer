package com.react.musicServer.controllers

import com.react.musicServer.data.MessageData
import com.react.musicServer.services.MainService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("api")
class ApiController @Autowired constructor(private val service: MainService) {
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
    suspend fun upload(@RequestPart("file") file: FilePart): MessageData {
        val pair = service.upload(file)
        return MessageData(
            fileName = pair.first,
            fileUrl = ""
                .plus("/api/download/")
                .plus(pair.second),
            fileUUID = pair.second.toString(),
            message = "File uploaded with success!"
        )
    }
}